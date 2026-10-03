package li.cil.oc.common.item.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.transfer.EnergyHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.DoubleConsumer;

/**
 * OC items that store energy. Loader energy APIs (Forge {@code ENERGY} capability /
 * Team Reborn Energy) should expose such items through {@link Provider}, e.g. from
 * {@code PlatformHooks.getEnergyHandler(ItemStack)}'s implementations.
 */
public interface Chargeable extends li.cil.oc.api.driver.item.Chargeable {
    double maxCharge(ItemStack stack);

    double getCharge(ItemStack stack);

    void setCharge(ItemStack stack, double amount);

    default boolean canExtract(ItemStack stack) {
        return false;
    }

    // ----------------------------------------------------------------------- //

    ResourceLocation KEY = new ResourceLocation(OpenComputers.ID, "chargeable");

    // ratioForgeEnergy is OC energy per FE (0.1 by default: 10 FE = 1 OC), like everywhere else
    // (integration.util.Power, power converters). 1.12 / 1.16.5 had these two inverted, so items
    // charged through their energy capability got 100x the energy; since the OC charger charges OC
    // items through the platform energy handler now, that made hover boots / tablets charge
    // (nearly) instantly.
    static double convertForgeEnergyToOpenComputers(long fe) {
        return fe * Settings.get().ratioForgeEnergy;
    }

    static long convertOpenComputersToForgeEnergy(double oc) {
        return (long) (oc / Settings.get().ratioForgeEnergy);
    }

    static double applyCharge(double amount, double current, double maximum, DoubleConsumer save) {
        final double target = current + amount;
        final double result = Math.min(Math.max(target, 0), maximum);
        final double used = result - current;
        final double unused = amount - used;
        if (used > Double.MIN_VALUE || used < -Double.MIN_VALUE) {
            save.accept(used);
        }
        return unused;
    }

    /**
     * Energy view of a chargeable item stack, in Forge Energy units (formerly the
     * Forge {@code ICapabilityProvider} + {@code IEnergyStorage}). Operates on the stack in place.
     */
    final class Provider implements EnergyHandler {
        private final ItemStack stack;
        private final Chargeable item;

        public Provider(ItemStack stack, Chargeable item) {
            this.stack = stack;
            this.item = item;
        }

        @Override
        public long receiveEnergy(long maxReceive, boolean simulate) {
            // Chargeable.charge() returns the amount UNUSED
            // EnergyHandler wants the amount USED
            return maxReceive - convertOpenComputersToForgeEnergy(item.charge(stack, convertForgeEnergyToOpenComputers(maxReceive), simulate));
        }

        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            if (canExtract()) {
                return -receiveEnergy(-maxExtract, simulate);
            } else {
                return 0;
            }
        }

        @Override
        public long getEnergyStored() {
            return convertOpenComputersToForgeEnergy(item.getCharge(stack));
        }

        @Override
        public long getMaxEnergyStored() {
            return convertOpenComputersToForgeEnergy(item.maxCharge(stack));
        }

        @Override
        public boolean canExtract() {
            return item.canExtract(stack);
        }

        @Override
        public boolean canReceive() {
            return item.canCharge(stack);
        }
    }
}
