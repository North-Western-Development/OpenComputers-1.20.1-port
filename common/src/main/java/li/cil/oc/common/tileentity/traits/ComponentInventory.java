package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface ComponentInventory extends Environment, Inventory, li.cil.oc.common.inventory.ComponentInventory {
    final class State {
        // Cache changes to inventory slots on the client side to avoid recreating
        // components when we don't have to and the slots are just cleared by MC
        // temporarily.
        public final List<ItemStack> pendingRemovals = new ArrayList<>();
        public final List<ItemStack> pendingAdds = new ArrayList<>();
        public boolean updateScheduled = false;
        /** Backing state of {@link li.cil.oc.common.inventory.ComponentInventory}. */
        public final li.cil.oc.common.inventory.ComponentInventory.ComponentState componentState = new li.cil.oc.common.inventory.ComponentInventory.ComponentState();
    }

    /** Provided by {@link TileEntity}. */
    State componentInventoryState();

    @Override
    default li.cil.oc.common.inventory.ComponentInventory.ComponentState componentState() {
        return componentInventoryState().componentState;
    }

    @Override
    default EnvironmentHost host() {
        return this;
    }

    // ----------------------------------------------------------------------- //

    default List<ItemStack> pendingRemovals() {
        final List<ItemStack> list = componentInventoryState().pendingRemovals;
        adjustSize(list, getContainerSize());
        return list;
    }

    default List<ItemStack> pendingAdds() {
        final List<ItemStack> list = componentInventoryState().pendingAdds;
        adjustSize(list, getContainerSize());
        return list;
    }

    private static void adjustSize(List<ItemStack> buffer, int size) {
        while (buffer.size() > size) {
            buffer.remove(buffer.size() - 1);
        }
        while (buffer.size() < size) {
            buffer.add(ItemStack.EMPTY);
        }
    }

    private void applyInventoryChanges() {
        componentInventoryState().updateScheduled = false;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final ItemStack removed = pendingRemovals().get(slot);
            final ItemStack added = pendingAdds().get(slot);
            if (!removed.isEmpty() && !added.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(removed, added)) {
                    li.cil.oc.common.inventory.ComponentInventory.super.onItemRemoved(slot, removed);
                    li.cil.oc.common.inventory.ComponentInventory.super.onItemAdded(slot, added);
                    ocSetChanged();
                } // else: No change, ignore.
            } else if (!removed.isEmpty()) {
                li.cil.oc.common.inventory.ComponentInventory.super.onItemRemoved(slot, removed);
                ocSetChanged();
            } else if (!added.isEmpty()) {
                li.cil.oc.common.inventory.ComponentInventory.super.onItemAdded(slot, added);
                ocSetChanged();
            } // else: No change.

            pendingRemovals().set(slot, ItemStack.EMPTY);
            pendingAdds().set(slot, ItemStack.EMPTY);
        }
    }

    private void scheduleInventoryChange() {
        final State state = componentInventoryState();
        if (!state.updateScheduled) {
            state.updateScheduled = true;
            EventHandler.scheduleClient(this::applyInventoryChanges);
        }
    }

    @Override
    default void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) li.cil.oc.common.inventory.ComponentInventory.super.onItemAdded(slot, stack);
        else {
            final ItemStack removed = pendingRemovals().get(slot);
            if (!removed.isEmpty() && ItemStack.isSameItemSameTags(removed, stack)) {
                // Reverted to original state.
                pendingAdds().set(slot, ItemStack.EMPTY);
                pendingRemovals().set(slot, ItemStack.EMPTY);
            } else {
                // Got a removal and an add of *something else* in the same tick.
                pendingAdds().set(slot, stack);
                scheduleInventoryChange();
            }
        }
    }

    @Override
    default void onItemRemoved(int slot, ItemStack stack) {
        if (isServer()) li.cil.oc.common.inventory.ComponentInventory.super.onItemRemoved(slot, stack);
        else {
            if (!pendingAdds().get(slot).isEmpty()) {
                // If we have a pending add and get a remove on a slot it is
                // now either empty, or the previous remove is valid again.
                pendingAdds().set(slot, ItemStack.EMPTY);
            } else {
                // If we have no pending add, only the first removal can be
                // relevant (further ones should in fact be impossible).
                if (pendingRemovals().get(slot).isEmpty()) {
                    pendingRemovals().set(slot, stack);
                    scheduleInventoryChange();
                }
            }
        }
    }

    @Override
    default void save(ManagedEnvironment component, DriverItem driver, ItemStack stack) {
        if (isServer()) {
            li.cil.oc.common.inventory.ComponentInventory.super.save(component, driver, stack);
        }
    }

    // ----------------------------------------------------------------------- //

    static void onInitialize(ComponentInventory self) {
        if (self.isClient()) {
            self.connectComponents();
        }
    }

    static void onDispose(ComponentInventory self) {
        if (self.isClient()) {
            self.disconnectComponents();
        }
    }

    @Override
    default void onConnect(Node node) {
        Environment.super.onConnect(node);
        if (node == this.node()) {
            connectComponents();
        }
    }

    @Override
    default void onDisconnect(Node node) {
        Environment.super.onDisconnect(node);
        if (node == this.node()) {
            disconnectComponents();
        }
    }

    // TODO(port): Forge capabilities provided by installed components (Scala getCapability
    //  forwarded to components implementing ICapabilityProvider) are not exposed anymore.

    static void onSaveForClient(ComponentInventory self, CompoundTag nbt) {
        self.connectComponents();
        self.saveData(nbt);
    }

    static void onLoadForClient(ComponentInventory self, CompoundTag nbt) {
        self.loadData(nbt);
        self.connectComponents();
    }
}
