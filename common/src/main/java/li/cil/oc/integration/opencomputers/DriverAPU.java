package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverAPU extends DriverCPU implements HostAware {
    public static final DriverAPU INSTANCE = new DriverAPU();

    private DriverAPU() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.APUTier1),
                Items.get(Constants.ItemName.APUTier2),
                Items.get(Constants.ItemName.APUCreative));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        switch (gpuTier(stack)) {
            case Tier.One:
                return new li.cil.oc.server.component.APU(Tier.One);
            case Tier.Two:
                return new li.cil.oc.server.component.APU(Tier.Two);
            case Tier.Three:
                return new li.cil.oc.server.component.APU(Tier.Three);
            default:
                return null;
        }
    }

    @Override
    public int cpuTier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.APU apu) return apu.cpuTier();
        return Tier.One;
    }

    public int gpuTier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.APU apu) return apu.gpuTier();
        return Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverAPU.INSTANCE.worksWith(stack))
                return li.cil.oc.server.component.GraphicsCard.class;
            else return null;
        }
    }
}
