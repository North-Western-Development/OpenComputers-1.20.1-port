package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import li.cil.oc.util.ExtendedArguments.TankProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Driver for any block exposing fluid storage through the platform transfer API
 * (Forge fluid capability / Fabric {@code Storage<FluidVariant>}).
 */
public final class DriverFluidHandler implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity == null) {
            return false;
        }
        return PlatformHooks.getFluidHandler(world, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        final FluidHandler handler = PlatformHooks.getFluidHandler(world, pos, side);
        return handler != null ? new Environment(handler) : null;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<FluidHandler> {
        public Environment(final FluidHandler tileEntity) {
            super(tileEntity, "fluid_handler");
        }

        @Callback(doc = "function():table -- Get some information about the tank accessible from the specified side.")
        public Object[] getTankInfo(final Context context, final Arguments args) {
            TankProperties[] props = new TankProperties[tileEntity.getTanks()];
            for (int i = 0; i < props.length; i++) {
                props[i] = new TankProperties(tileEntity.getTankCapacity(i), tileEntity.getFluidInTank(i));
            }
            return props;
        }
    }
}
