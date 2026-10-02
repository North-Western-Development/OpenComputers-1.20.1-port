package li.cil.oc.common.container;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.common.tileentity.traits.PlayerInputAware;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class ComponentSlot extends Slot {
    protected final Class<? extends EnvironmentHost> host;

    public Optional<Consumer<Slot>> changeListener = Optional.empty();

    protected ComponentSlot(Container inventory, int index, int x, int y, @Nullable Class<? extends EnvironmentHost> host) {
        super(inventory, index, x, y);
        this.host = host;
    }

    public abstract li.cil.oc.common.container.Player agentContainer();

    public abstract String slot();

    public abstract int tier();

    /**
     * Client only (references client textures).
     */
    @Nullable
    public abstract ResourceLocation tierIcon();

    // ----------------------------------------------------------------------- //

    /**
     * Client only.
     */
    public boolean hasBackground() {
        return getBackgroundLocation() != null;
    }

    /**
     * Client only (references client textures).
     */
    @Nullable
    public ResourceLocation getBackgroundLocation() {
        return null;
    }

    @Override
    public boolean isActive() {
        return !li.cil.oc.common.Slot.None.equals(slot()) && tier() != Tier.None && super.isActive();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!container.canPlaceItem(getContainerSlot(), stack)) return false;
        String slot = slot();
        int tier = tier();
        if (li.cil.oc.common.Slot.None.equals(slot) || tier == Tier.None) return false;
        if (li.cil.oc.common.Slot.Any.equals(slot) && tier == Tier.Any) return true;
        // Special case: tool slots fit everything.
        if (li.cil.oc.common.Slot.Tool.equals(slot)) return true;
        DriverItem driver = Driver.driverFor(stack, host);
        if (driver != null) {
            boolean slotOk = li.cil.oc.common.Slot.Any.equals(slot) || slot.equals(driver.slot(stack));
            boolean tierOk = tier == Tier.Any || driver.tier(stack) <= tier;
            return slotOk && tierOk;
        }
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        for (Slot slot : agentContainer().slots) {
            if (slot instanceof ComponentSlot dynamic) dynamic.clearIfInvalid(player);
        }
        super.onTake(player, stack);
    }

    @Override
    public void set(ItemStack stack) {
        super.set(stack);
        if (container instanceof PlayerInputAware playerAware) {
            playerAware.onSetInventorySlotContents(agentContainer().playerInventory.player, getContainerSlot(), stack);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        for (Slot slot : agentContainer().slots) {
            if (slot instanceof ComponentSlot dynamic) dynamic.clearIfInvalid(agentContainer().playerInventory.player);
        }
        changeListener.ifPresent(listener -> listener.accept(this));
    }

    protected void clearIfInvalid(Player player) {
    }
}
