package li.cil.oc.integration.platform;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.EnergyHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import static li.cil.oc.util.ResultWrapper.result;

/**
 * Driver for blocks exposing an energy storage (Forge Energy on Forge, Team
 * Reborn Energy on Fabric), via {@link PlatformHooks#getEnergyHandler(Level, BlockPos, Direction)}.
 * Formerly {@code integration.minecraftforge.DriverEnergyStorage}.
 *
 * @author Vexatos
 */
public final class DriverEnergyStorage implements DriverBlock {
    public static final DriverEnergyStorage INSTANCE = new DriverEnergyStorage();

    private DriverEnergyStorage() {
    }

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        return world.getBlockEntity(pos) != null && PlatformHooks.getEnergyHandler(world, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        if (world.getBlockEntity(pos) == null) return null;
        final EnergyHandler storage = PlatformHooks.getEnergyHandler(world, pos, side);
        return storage != null ? new Environment(storage) : null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock {
        public final EnergyHandler storage;

        public Environment(EnergyHandler storage) {
            this.storage = storage;
            setNode(Network.newNode(this, Visibility.Network).withComponent("energy_device").create());
        }

        @Callback(doc = "function():number -- Returns the amount of stored energy on the connected side.")
        public Object[] getEnergyStored(Context context, Arguments args) {
            return result(storage.getEnergyStored());
        }

        @Callback(doc = "function():number -- Returns the maximum amount of stored energy on the connected side.")
        public Object[] getMaxEnergyStored(Context context, Arguments args) {
            return result(storage.getMaxEnergyStored());
        }

        @Callback(doc = "function():number -- Returns whether this component can have energy extracted from the connected side.")
        public Object[] canExtract(Context context, Arguments args) {
            return result(storage.canExtract());
        }

        @Callback(doc = "function():number -- Returns whether this component can receive energy on the connected side.")
        public Object[] canReceive(Context context, Arguments args) {
            return result(storage.canReceive());
        }

        @Override
        public String preferredName() {
            return "energy_device";
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
