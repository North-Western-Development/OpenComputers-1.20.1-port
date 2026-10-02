package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.InventorySlots.InventorySlot;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.SideTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.IntSupplier;

public class DynamicComponentSlot extends ComponentSlot {
    public final li.cil.oc.common.container.Player agentContainer;
    public final Function<DynamicComponentSlot, InventorySlot> info;
    public final IntSupplier containerTierGetter;

    public DynamicComponentSlot(li.cil.oc.common.container.Player agentContainer, Container inventory, int index, int x, int y, @Nullable Class<? extends EnvironmentHost> host,
                                Function<DynamicComponentSlot, InventorySlot> info, IntSupplier containerTierGetter) {
        super(inventory, index, x, y, host);
        this.agentContainer = agentContainer;
        this.info = info;
        this.containerTierGetter = containerTierGetter;
    }

    @Override
    public li.cil.oc.common.container.Player agentContainer() {
        return agentContainer;
    }

    @Override
    public int tier() {
        int mainTier = containerTierGetter.getAsInt();
        if (mainTier >= 0) return info.apply(this).tier;
        return mainTier;
    }

    @Override
    public ResourceLocation tierIcon() {
        return SlotIcons.get(tier());
    }

    @Override
    public String slot() {
        int mainTier = containerTierGetter.getAsInt();
        if (mainTier >= 0) return info.apply(this).slot;
        return li.cil.oc.common.Slot.None;
    }

    @Override
    public boolean hasBackground() {
        return SlotIcons.get(slot()) != null;
    }

    @Override
    public ResourceLocation getBackgroundLocation() {
        ResourceLocation location = SlotIcons.get(slot());
        return location != null ? location : super.getBackgroundLocation();
    }

    @Override
    public int getMaxStackSize() {
        String slot = slot();
        if (li.cil.oc.common.Slot.Tool.equals(slot) || li.cil.oc.common.Slot.Any.equals(slot) || li.cil.oc.common.Slot.Filtered.equals(slot)) {
            return super.getMaxStackSize();
        }
        if (li.cil.oc.common.Slot.None.equals(slot)) return 0;
        return 1;
    }

    @Override
    protected void clearIfInvalid(Player player) {
        if (SideTracker.isServer() && hasItem() && !mayPlace(getItem())) {
            ItemStack stack = getItem();
            set(ItemStack.EMPTY);
            InventoryUtils.addToPlayerInventory(stack, player);
        }
    }
}
