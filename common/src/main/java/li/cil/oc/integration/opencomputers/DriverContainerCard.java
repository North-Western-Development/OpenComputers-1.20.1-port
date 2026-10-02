package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverContainerCard implements Item, Container {
    public static final DriverContainerCard INSTANCE = new DriverContainerCard();

    private DriverContainerCard() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.CardContainerTier1),
                Items.get(Constants.ItemName.CardContainerTier2),
                Items.get(Constants.ItemName.CardContainerTier3));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Container;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.UpgradeContainerCard item) return item.tier;
        return Tier.One;
    }

    @Override
    public String providedSlot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int providedTier(ItemStack stack) {
        return tier(stack);
    }
}
