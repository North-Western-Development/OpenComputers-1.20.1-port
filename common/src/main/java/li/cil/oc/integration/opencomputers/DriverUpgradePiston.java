package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverUpgradePiston implements Item, HostAware {
    public static final DriverUpgradePiston INSTANCE = new DriverUpgradePiston();

    private DriverUpgradePiston() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.PistonUpgrade));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        if (host instanceof Drone drone) return new li.cil.oc.server.component.UpgradePiston.Drone(drone);
        if (host instanceof Tablet tablet) return new li.cil.oc.server.component.UpgradePiston.Tablet(tablet);
        if (host instanceof Rotatable) return new li.cil.oc.server.component.UpgradePiston.Rotatable((EnvironmentHost & Rotatable) host);
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverUpgradePiston.INSTANCE.worksWith(stack))
                return li.cil.oc.server.component.UpgradePiston.class;
            else return null;
        }
    }
}
