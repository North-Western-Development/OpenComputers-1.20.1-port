package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.InventorySlots.InventorySlot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.SideTracker;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Base class of OC menus. The name is kept from the Scala code (it's the "player inventory + other
 * inventory" container), it is unrelated to {@link net.minecraft.world.entity.player.Player}.
 */
public abstract class Player extends AbstractContainerMenu {
    public final Inventory playerInventory;
    public final Container otherInventory;

    /**
     * Number of player inventory slots to display horizontally.
     */
    protected final int playerInventorySizeX = Math.min(9, Inventory.getSelectionSize());

    protected final int playerInventorySizeY;

    /**
     * Render size of slots (width and height).
     */
    protected final int slotSize = 18;

    private long lastSync = System.currentTimeMillis();

    protected final SynchronizedData synchronizedData = new SynchronizedData();

    protected Player(@Nullable MenuType<?> selfType, int id, Inventory playerInventory, Container otherInventory) {
        super(selfType, id);
        this.playerInventory = playerInventory;
        this.otherInventory = otherInventory;
        this.playerInventorySizeY = Math.min(4, playerInventory.items.size() / playerInventorySizeX);
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return otherInventory.stillValid(player);
    }

    @Override
    public void clicked(int slot, int dragType, ClickType clickType, net.minecraft.world.entity.player.Player player) {
        super.clicked(slot, dragType, clickType, player);
        if (SideTracker.isServer()) {
            broadcastChanges(); // We have to enforce this more than MC does itself
            // because stacks can change their... "character" just by being inserted in
            // certain containers - by being assigned an address.
        }
    }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        Slot slot = index >= 0 && index < slots.size() ? slots.get(index) : null;
        if (slot != null && slot.hasItem()) {
            tryTransferStackInSlot(slot, slot.container == otherInventory);
            if (SideTracker.isServer()) {
                broadcastChanges();
            }
        }
        return ItemStack.EMPTY;
    }

    // return true if all items have been moved or no more work to do
    protected boolean tryMoveAllSlotToSlot(@Nullable Slot from, @Nullable Slot to) {
        if (to == null)
            return false; // nowhere to move it

        if (from == null ||
            !from.hasItem() ||
            from.getItem().isEmpty())
            return true; // all moved because nothing to move

        if (to.container == from.container)
            return false; // not intended for moving in the same inventory

        // for ghost slots we don't care about stack size
        ItemStack fromStack = from.getItem();
        ItemStack toStack = to.hasItem() ? to.getItem() : ItemStack.EMPTY;
        int toStackSize = !toStack.isEmpty() ? toStack.getCount() : 0;

        int maxStackSize = Math.min(fromStack.getMaxStackSize(), to.getMaxStackSize());
        int itemsMoved = Math.min(maxStackSize - toStackSize, fromStack.getCount());

        if (!toStack.isEmpty()) {
            if (toStackSize < maxStackSize &&
                ItemStack.isSameItemSameTags(fromStack, toStack) &&
                itemsMoved > 0) {
                toStack.grow(from.remove(itemsMoved).getCount());
            }
            else return false;
        }
        else if (to.mayPlace(fromStack)) {
            to.set(from.remove(itemsMoved));
            if (maxStackSize == 0) {
                // Special case: we have an inventory with "phantom/ghost stacks", i.e.
                // zero size stacks, usually used for configuring machinery. In that
                // case we stop early if whatever we're shift clicking is already in a
                // slot of the target inventory. This workaround can be problematic if
                // an inventory has both real and phantom slots, but we don't have
                // something like that, yet, so hey.
                return true;
            }
        }
        else return false;

        to.setChanged();
        from.setChanged();
        return false;
    }

    protected List<Integer> fillOrder(boolean backFill) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) indices.add(i);
        if (backFill) java.util.Collections.reverse(indices);
        // Stable sort, same as Scala's sortBy.
        indices.sort(Comparator.comparingInt(i -> {
            Slot s = slots.get(i);
            if (s.hasItem()) return -1;
            if (s instanceof ComponentSlot componentSlot) return componentSlot.tier();
            return 99;
        }));
        return indices;
    }

    protected void tryTransferStackInSlot(Slot from, boolean intoPlayerInventory) {
        for (int i : fillOrder(intoPlayerInventory)) {
            if (tryMoveAllSlotToSlot(from, slots.get(i)))
                return;
        }
    }

    // Used by the ComponentSlots to make host-aware item placement decisions.
    @Nullable
    protected abstract Class<? extends EnvironmentHost> getHostClass();

    public void addSlotToContainer(int x, int y, String slot, int tier) {
        int index = slots.size();
        addSlot(new StaticComponentSlot(this, otherInventory, index, x, y, getHostClass(), slot, tier));
    }

    public void addSlotToContainer(int x, int y, String slot) {
        addSlotToContainer(x, y, slot, Tier.Any);
    }

    public void addSlotToContainer(int x, int y) {
        addSlotToContainer(x, y, li.cil.oc.common.Slot.Any, Tier.Any);
    }

    public void addSlotToContainer(int x, int y, InventorySlot[][] info, IntSupplier containerTierGetter) {
        int index = slots.size();
        addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, getHostClass(),
            slot -> info[slot.containerTierGetter.getAsInt()][slot.getContainerSlot()], containerTierGetter));
    }

    public void addSlotToContainer(int x, int y, Function<DynamicComponentSlot, InventorySlot> info) {
        int index = slots.size();
        addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, getHostClass(), info, () -> Tier.One));
    }

    /**
     * Render player inventory at the specified coordinates.
     */
    protected void addPlayerInventorySlots(int left, int top) {
        // Show the inventory proper. Start at plus one to skip hot bar.
        for (int slotY = 1; slotY < playerInventorySizeY; slotY++) {
            for (int slotX = 0; slotX < playerInventorySizeX; slotX++) {
                int index = slotX + slotY * playerInventorySizeX;
                int x = left + slotX * slotSize;
                // Compensate for hot bar offset.
                int y = top + (slotY - 1) * slotSize;
                addSlot(new Slot(playerInventory, index, x, y));
            }
        }

        // Show the quick slot bar below the internal inventory.
        int quickBarSpacing = 4;
        for (int index = 0; index < playerInventorySizeX; index++) {
            int x = left + index * slotSize;
            int y = top + slotSize * (playerInventorySizeY - 1) + quickBarSpacing;
            addSlot(new Slot(playerInventory, index, x, y));
        }
    }

    // Note: in 1.16 this tracked ServerPlayer slot listeners. Since 1.17 players are no longer
    // container listeners; the only player that can have this menu open is its owner.
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (SideTracker.isServer() && playerInventory.player instanceof ServerPlayer player && !PlatformHooks.isFakePlayer(player)) {
            CompoundTag nbt = new CompoundTag();
            detectCustomDataChanges(nbt);
            PacketSender.sendContainerUpdate(this, nbt, player);
        }
    }

    // Used for custom value synchronization, because shorts simply don't cut it most of the time.
    protected void detectCustomDataChanges(CompoundTag nbt) {
        CompoundTag delta = synchronizedData.getDelta();
        if (delta != null && !delta.isEmpty()) {
            nbt.put("delta", delta);
        }
        else if (System.currentTimeMillis() - lastSync > 250) {
            nbt.put("delta", synchronizedData);
            lastSync = Long.MAX_VALUE;
        }
    }

    public void updateCustomData(CompoundTag nbt) {
        if (nbt.contains("delta")) {
            CompoundTag delta = nbt.getCompound("delta");
            for (String key : delta.getAllKeys()) {
                synchronizedData.put(key, delta.get(key));
            }
        }
    }

    protected static class SynchronizedData extends CompoundTag {
        private CompoundTag delta = new CompoundTag();

        @Nullable
        public synchronized CompoundTag getDelta() {
            if (delta.isEmpty()) return null;
            CompoundTag result = delta;
            delta = new CompoundTag();
            return result;
        }

        @Override
        public synchronized Tag put(String key, Tag value) {
            if (!value.equals(get(key))) delta.put(key, value);
            return super.put(key, value);
        }

        @Override
        public synchronized void putByte(String key, byte value) {
            if (value != getByte(key)) delta.putByte(key, value);
            super.putByte(key, value);
        }

        @Override
        public synchronized void putShort(String key, short value) {
            if (value != getShort(key)) delta.putShort(key, value);
            super.putShort(key, value);
        }

        @Override
        public synchronized void putInt(String key, int value) {
            if (value != getInt(key)) delta.putInt(key, value);
            super.putInt(key, value);
        }

        @Override
        public synchronized void putLong(String key, long value) {
            if (value != getLong(key)) delta.putLong(key, value);
            super.putLong(key, value);
        }

        @Override
        public synchronized void putFloat(String key, float value) {
            if (value != getFloat(key)) delta.putFloat(key, value);
            super.putFloat(key, value);
        }

        @Override
        public synchronized void putDouble(String key, double value) {
            if (value != getDouble(key)) delta.putDouble(key, value);
            super.putDouble(key, value);
        }

        @Override
        public synchronized void putString(String key, String value) {
            if (!value.equals(getString(key))) delta.putString(key, value);
            super.putString(key, value);
        }

        @Override
        public synchronized void putByteArray(String key, byte[] value) {
            if (get(key) instanceof ByteArrayTag arr && !Arrays.equals(value, arr.getAsByteArray())) delta.putByteArray(key, value);
            super.putByteArray(key, value);
        }

        @Override
        public synchronized void putIntArray(String key, int[] value) {
            if (get(key) instanceof IntArrayTag arr && !Arrays.equals(value, arr.getAsIntArray())) delta.putIntArray(key, value);
            super.putIntArray(key, value);
        }

        @Override
        public synchronized void putBoolean(String key, boolean value) {
            if (value != getBoolean(key)) delta.putBoolean(key, value);
            super.putBoolean(key, value);
        }
    }
}
