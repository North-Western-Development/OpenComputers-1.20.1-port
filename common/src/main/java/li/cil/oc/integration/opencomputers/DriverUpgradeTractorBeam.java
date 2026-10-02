package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.item.TabletWrapper;
import net.minecraft.world.item.ItemStack;

public final class DriverUpgradeTractorBeam implements Item, HostAware {
    public static final DriverUpgradeTractorBeam INSTANCE = new DriverUpgradeTractorBeam();

    private DriverUpgradeTractorBeam() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.TractorBeamUpgrade));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        if (host instanceof Drone drone) return new li.cil.oc.server.component.UpgradeTractorBeam.Drone(drone);
        if (host instanceof Robot robot) return new li.cil.oc.server.component.UpgradeTractorBeam.Player(host, robot::player);
        if (host instanceof TabletWrapper tablet) return new li.cil.oc.server.component.UpgradeTractorBeam.Player(host, () -> tablet.player);
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        return Tier.Three;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverUpgradeTractorBeam.INSTANCE.worksWith(stack))
                return li.cil.oc.server.component.UpgradeTractorBeam.Common.class;
            else return null;
        }
    }
}
