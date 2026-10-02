package li.cil.oc.integration.minecraft;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import li.cil.oc.util.ExtendedArguments.TankProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Driver for block entities that directly are a fluid tank. On 1.16.5 this
 * targeted Forge's {@code IFluidTank}; the loader-independent equivalent is a
 * block entity implementing {@link FluidHandler} (the first tank is reported).
 */
public final class DriverFluidTank extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return FluidHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((FluidHandler) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<FluidHandler> {
        public Environment(final FluidHandler tileEntity) {
            super(tileEntity, "fluid_tank");
        }

        @Callback(doc = "function():table -- Get some information about this tank.")
        public Object[] getInfo(final Context context, final Arguments args) {
            if (tileEntity.getTanks() < 1) {
                return new Object[]{new TankProperties(0, dev.architectury.fluid.FluidStack.empty())};
            }
            return new Object[]{new TankProperties(tileEntity.getTankCapacity(0), tileEntity.getFluidInTank(0))};
        }
    }
}
