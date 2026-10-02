package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverUpgradeHover implements Item, HostAware {
    public static final DriverUpgradeHover INSTANCE = new DriverUpgradeHover();

    private DriverUpgradeHover() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.HoverUpgradeTier1),
                Items.get(Constants.ItemName.HoverUpgradeTier2));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.UpgradeHover item) return item.tier;
        return Tier.One;
    }
}
