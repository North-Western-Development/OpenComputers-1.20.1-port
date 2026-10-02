package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverEEPROM implements Item {
    public static final DriverEEPROM INSTANCE = new DriverEEPROM();

    private DriverEEPROM() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.EEPROM));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        return new li.cil.oc.server.component.EEPROM();
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.EEPROM;
    }

    @Override
    public int tier(ItemStack stack) {
        return Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverEEPROM.INSTANCE.worksWith(stack))
                return li.cil.oc.server.component.EEPROM.class;
            else return null;
        }
    }
}
