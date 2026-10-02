package li.cil.oc.util;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.common.transfer.ItemFluidHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Fluid transfer helpers. All amounts are in millibuckets (see {@link FluidHandler}).
 */
public final class FluidUtils {
    private FluidUtils() {
    }

    /** One bucket in millibuckets (formerly {@code FluidAttributes.BUCKET_VOLUME}). */
    public static final int BUCKET_VOLUME = 1000;

    /**
     * Retrieves an actual fluid handler implementation for a specified world coordinate.
     * <p/>
     * This performs special handling for in-world liquids.
     */
    public static Optional<FluidHandler> fluidHandlerAt(BlockPosition position, @Nullable Direction side) {
        if (position.world.isPresent() && ExtendedWorld.blockExists(position.world.get(), position)) {
            final Level world = position.world.get();
            final BlockEntity blockEntity = ExtendedWorld.getBlockEntity(world, position);
            if (blockEntity instanceof FluidHandler handler) {
                return Optional.of(handler);
            }
            final FluidHandler handler = PlatformHooks.getFluidHandler(world, position.toBlockPos(), side);
            if (handler != null) {
                return Optional.of(handler);
            }
            return Optional.of(new GenericBlockWrapper(position));
        }
        return Optional.empty();
    }

    /**
     * The fluid handler of an item stack, or null. Note that the handler operates on a
     * copy of the stack, use {@link ItemFluidHandler#getContainer()} to get the result.
     */
    @Nullable
    public static ItemFluidHandler fluidHandlerOf(@Nullable ItemStack stack) {
        if (stack != null) {
            return PlatformHooks.getFluidHandler(stack);
        }
        return null;
    }

    /**
     * Transfers some fluid between two fluid handlers.
     * <p/>
     * This will try to extract up the specified amount of fluid from any handler,
     * then insert it into the specified sink handler. If the insertion fails, the
     * fluid will remain in the source handler.
     * <p/>
     * This returns the amount of fluid (mB) that was transferred.
     */
    public static long transferBetweenFluidHandlers(FluidHandler source, FluidHandler sink, long limit) {
        final FluidStack drained = source.drain(limit, true);
        if (drained == null || drained.isEmpty()) {
            return 0;
        }
        final long filled = sink.fill(drained, true);
        return sink.fill(source.drain(filled, false), false);
    }

    public static long transferBetweenFluidHandlers(FluidHandler source, FluidHandler sink) {
        return transferBetweenFluidHandlers(source, sink, BUCKET_VOLUME);
    }

    /**
     * Utility method for calling <tt>transferBetweenFluidHandlers</tt> on handlers
     * in the world.
     * <p/>
     * This uses the <tt>fluidHandlerAt</tt> method, and therefore handles special
     * cases such as fluid blocks.
     */
    public static long transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide, BlockPosition sinkPos, Direction sinkSide, long limit) {
        return fluidHandlerAt(sourcePos, sourceSide).map(source ->
                fluidHandlerAt(sinkPos, sinkSide).map(sink ->
                        transferBetweenFluidHandlers(source, sink, limit)).orElse(0L)).orElse(0L);
    }

    public static long transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide, BlockPosition sinkPos, Direction sinkSide) {
        return transferBetweenFluidHandlersAt(sourcePos, sourceSide, sinkPos, sinkSide, BUCKET_VOLUME);
    }

    /**
     * Lookup fluid taking into account flowing liquid blocks...
     * For legacy reasons, returns null when the block is not a fluid, not Fluids.EMPTY.
     */
    @Deprecated
    @Nullable
    public static Fluid lookupFluidForBlock(Block block) {
        if (block instanceof LiquidBlock) {
            // The default state of a liquid block is its source state.
            return block.defaultBlockState().getFluidState().getType();
        }
        return null;
    }

    // ----------------------------------------------------------------------- //

    private static boolean isFullLiquidBlock(BlockPosition position) {
        final BlockState state = position.world.get().getBlockState(position.toBlockPos());
        return state.getValue(LiquidBlock.LEVEL) == 0;
    }

    private static final class GenericBlockWrapper implements FluidHandler {
        private final BlockPosition position;

        GenericBlockWrapper(BlockPosition position) {
            this.position = position;
        }

        @Override
        public int getTanks() {
            return currentWrapper().map(FluidHandler::getTanks).orElse(0);
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return currentWrapper().map(w -> w.getFluidInTank(tank)).orElse(FluidStack.empty());
        }

        @Override
        public long getTankCapacity(int tank) {
            return currentWrapper().map(w -> w.getTankCapacity(tank)).orElse(0L);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluid) {
            return currentWrapper().map(w -> w.isFluidValid(tank, fluid)).orElse(false);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            return currentWrapper().map(w -> w.drain(resource, simulate)).orElse(FluidStack.empty());
        }

        @Override
        public FluidStack drain(long maxDrain, boolean simulate) {
            return currentWrapper().map(w -> w.drain(maxDrain, simulate)).orElse(FluidStack.empty());
        }

        @Override
        public long fill(FluidStack resource, boolean simulate) {
            return currentWrapper().map(w -> w.fill(resource, simulate)).orElse(0L);
        }

        Optional<FluidHandler> currentWrapper() {
            final Level world = position.world.get();
            if (ExtendedWorld.blockExists(world, position)) {
                final Block block = ExtendedWorld.getBlock(world, position);
                // TODO(port): Forge's IFluidBlock (modded fluid blocks) case was dropped; modded
                //  fluids on 1.20 are LiquidBlocks too, so they're covered by the case below.
                if (block instanceof LiquidBlock liquid && lookupFluidForBlock(block) != null && isFullLiquidBlock(position)) {
                    return Optional.of(new LiquidBlockWrapper(position, liquid));
                } else if (ExtendedBlock.isAir(block, position) || ExtendedBlock.isReplaceable(block, position)) {
                    return Optional.of(new AirBlockWrapper(position, block));
                }
                return Optional.empty();
            } else return Optional.empty();
        }
    }

    private abstract static class BlockWrapperBase implements FluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long getTankCapacity(int tank) {
            return BUCKET_VOLUME;
        }

        protected abstract FluidStack uncheckedDrain(boolean simulate);

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            final FluidStack drained = uncheckedDrain(true);
            if (!drained.isEmpty() && (resource == null || (drained.getFluid() == resource.getFluid() && drained.getAmount() <= resource.getAmount()))) {
                return uncheckedDrain(simulate);
            } else return FluidStack.empty();
        }

        @Override
        public FluidStack drain(long maxDrain, boolean simulate) {
            final FluidStack drained = uncheckedDrain(true);
            if (!drained.isEmpty() && drained.getAmount() <= maxDrain) {
                return uncheckedDrain(simulate);
            } else return FluidStack.empty();
        }

        @Override
        public long fill(FluidStack resource, boolean simulate) {
            return 0;
        }
    }

    private static final class LiquidBlockWrapper extends BlockWrapperBase {
        final BlockPosition position;
        final LiquidBlock block;
        final Fluid fluid;

        LiquidBlockWrapper(BlockPosition position, LiquidBlock block) {
            this.position = position;
            this.block = block;
            this.fluid = lookupFluidForBlock(block);
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return isFullLiquidBlock(position) ? FluidStack.create(fluid, BUCKET_VOLUME) : FluidStack.empty();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return fluid.isSame(stack.getFluid());
        }

        @Override
        protected FluidStack uncheckedDrain(boolean simulate) {
            // Note: evaluate before removing the block, the result of the drain is the removed liquid.
            final FluidStack result = isFullLiquidBlock(position) ? FluidStack.create(fluid, BUCKET_VOLUME) : FluidStack.empty();
            if (!simulate) {
                ExtendedWorld.setBlockToAir(position.world.get(), position);
            }
            return result;
        }
    }

    private static final class AirBlockWrapper implements FluidHandler {
        final BlockPosition position;
        final Block block;

        AirBlockWrapper(BlockPosition position, Block block) {
            this.position = position;
            this.block = block;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long getTankCapacity(int tank) {
            return BUCKET_VOLUME;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.empty();
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            return FluidStack.empty();
        }

        @Override
        public FluidStack drain(long maxDrain, boolean simulate) {
            return FluidStack.empty();
        }

        private static boolean hasBlock(Fluid fluid) {
            return !fluid.defaultFluidState().createLegacyBlock().isAir();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluid) {
            return hasBlock(fluid.getFluid());
        }

        @Override
        public long fill(FluidStack resource, boolean simulate) {
            if (resource != null && !resource.isEmpty() && hasBlock(resource.getFluid()) && resource.getAmount() >= BUCKET_VOLUME) {
                if (!simulate) {
                    final Level world = position.world.get();
                    if (!ExtendedWorld.isAirBlock(world, position) && !world.containsAnyLiquid(position.bounds()))
                        ExtendedWorld.breakBlock(world, position);
                    world.setBlockAndUpdate(position.toBlockPos(), resource.getFluid().defaultFluidState().createLegacyBlock());
                    // This fake neighbor update is required to get stills to start flowing.
                    ExtendedWorld.notifyBlockOfNeighborChange(world, position, ExtendedWorld.getBlock(world, position));
                }
                return BUCKET_VOLUME;
            } else return 0;
        }
    }
}
