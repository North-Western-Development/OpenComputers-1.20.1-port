package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverUpgradeBattery implements Item, HostAware {
    public static final DriverUpgradeBattery INSTANCE = new DriverUpgradeBattery();

    private DriverUpgradeBattery() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.BatteryUpgradeTier1),
                Items.get(Constants.ItemName.BatteryUpgradeTier2),
                Items.get(Constants.ItemName.BatteryUpgradeTier3));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        return new li.cil.oc.server.component.UpgradeBattery(tier(stack));
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.UpgradeBattery item) return item.tier;
        return Tier.One;
    }
}
