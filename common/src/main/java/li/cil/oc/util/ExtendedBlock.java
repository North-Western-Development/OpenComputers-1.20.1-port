package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Former implicit extension class for {@link Block}; call as
 * {@code ExtendedBlock.method(block, position, ...)}. The {@link BlockPosition}
 * must carry a world.
 */
public final class ExtendedBlock {
    private ExtendedBlock() {
    }

    @Deprecated
    public static boolean isAir(Block block, BlockPosition position) {
        final Level world = position.world.get();
        return world.getBlockState(position.toBlockPos()).isAir();
    }

    @Deprecated
    public static boolean isReplaceable(Block block, BlockPosition position) {
        // Formerly `defaultBlockState.getMaterial.isReplaceable`; materials are gone in 1.20.
        return block.defaultBlockState().canBeReplaced();
    }

    @Deprecated
    public static float getBlockHardness(Block block, BlockPosition position) {
        final Level world = position.world.get();
        return world.getBlockState(position.toBlockPos()).getDestroySpeed(world, position.toBlockPos());
    }

    @Deprecated
    public static int getComparatorInputOverride(Block block, BlockPosition position, Direction side) {
        final Level world = position.world.get();
        return world.getBlockState(position.toBlockPos()).getAnalogOutputSignal(world, position.toBlockPos());
    }

    // TODO(port): ExtendedFluidBlock (drain / canDrain / getFilledPercentage on Forge's IFluidBlock)
    //  was dropped: IFluidBlock is Forge-only and nothing outside util used these helpers. In-world
    //  fluids are handled through FluidUtils (vanilla LiquidBlock / BucketPickup).
}
