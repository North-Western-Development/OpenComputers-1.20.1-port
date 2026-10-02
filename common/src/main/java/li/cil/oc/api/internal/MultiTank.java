package li.cil.oc.api.internal;

import li.cil.oc.common.transfer.FluidHandler;

/**
 * Implemented by objects with multiple internal tanks.
 * <p/>
 * This is specifically for containers where the side does not matter when
 * accessing the internal tanks, only the index of the tank; unlike with the
 * Forge's {@code IFluidHandler} interface.
 * <p/>
 * Note: in the 1.20.1 port the individual tanks are exposed as
 * {@link FluidHandler} instances (OC's loader-agnostic fluid storage, amounts
 * in millibuckets) that expose exactly one tank (index 0), replacing Forge's
 * {@code IFluidTank}.
 */
public interface MultiTank {
    /**
     * The number of tanks currently installed.
     */
    int tankCount();

    /**
     * Get the installed fluid tank with the specified index.
     *
     * @param index the index of the tank to get.
     * @return a single-tank handler for the tank with the specified index.
     */
    FluidHandler getFluidTank(int index);
}
