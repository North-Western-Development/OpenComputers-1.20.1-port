package li.cil.oc.server.component.traits;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.util.ExtendedArguments;

import java.util.Optional;

/**
 * Tanks returned by {@link MultiTank#getFluidTank(int)} are single-tank
 * {@link FluidHandler} views (tank index 0), amounts in millibuckets.
 */
public interface TankAware {
    MultiTank tank();

    int selectedTank();

    void setSelectedTank(int value);

    // ----------------------------------------------------------------------- //

    default int optTank(Arguments args, int n) {
        // Note: like the original, this always checks argument 0.
        if (args.count() > 0 && args.checkAny(0) != null) return ExtendedArguments.checkTank(args, tank(), 0);
        else return selectedTank();
    }

    default Optional<FluidHandler> getTank(int index) {
        return Optional.ofNullable(tank().getFluidTank(index));
    }

    default Optional<FluidStack> fluidInTank(int index) {
        return getTank(index).map(tank -> tank.getFluidInTank(0));
    }

    default boolean haveSameFluidType(FluidStack stackA, FluidStack stackB) {
        return stackA.isFluidEqual(stackB);
    }

    /** Capacity of a single-tank view. */
    static long capacityOf(FluidHandler tank) {
        return tank.getTankCapacity(0);
    }

    /** Fluid amount of a single-tank view. */
    static long amountOf(FluidHandler tank) {
        final FluidStack fluid = tank.getFluidInTank(0);
        return fluid == null ? 0 : fluid.getAmount();
    }
}
