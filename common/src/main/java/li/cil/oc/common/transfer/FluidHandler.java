package li.cil.oc.common.transfer;

import dev.architectury.fluid.FluidStack;

/**
 * Loader-agnostic multi-tank fluid storage, modelled after Forge's {@code IFluidHandler}.
 * <p>
 * <b>All amounts are in millibuckets</b>, independent of loader (Fabric
 * implementations convert from/to droplets). The {@link FluidStack} instances
 * handed in and out of this interface therefore also carry millibucket amounts;
 * use {@link #toPlatform(FluidStack)} / {@link #fromPlatform(FluidStack)} when
 * passing them to Architectury APIs that expect platform units.
 */
public interface FluidHandler {
    int getTanks();

    FluidStack getFluidInTank(int tank);

    long getTankCapacity(int tank);

    default boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    /** @return the amount (mB) that was / would have been filled. */
    long fill(FluidStack resource, boolean simulate);

    /** Drain a specific fluid. @return what was / would have been drained. */
    FluidStack drain(FluidStack resource, boolean simulate);

    /** Drain any fluid. @return what was / would have been drained. */
    FluidStack drain(long maxDrain, boolean simulate);

    static FluidStack toPlatform(FluidStack millibuckets) {
        return FluidStack.create(millibuckets, millibuckets.getAmount() * dev.architectury.hooks.fluid.FluidStackHooks.bucketAmount() / 1000);
    }

    static FluidStack fromPlatform(FluidStack platform) {
        return FluidStack.create(platform, platform.getAmount() * 1000 / dev.architectury.hooks.fluid.FluidStackHooks.bucketAmount());
    }
}
