package li.cil.oc.util;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Former implicit extension classes for {@link BlockGetter} ({@code ExtendedBlockAccess})
 * and {@link Level} ({@code ExtendedWorld}); call as
 * {@code ExtendedWorld.method(world, position, ...)}.
 */
public final class ExtendedWorld {
    private ExtendedWorld() {
    }

    // ----------------------------------------------------------------------- //
    // ExtendedBlockAccess

    public static Block getBlock(BlockGetter world, BlockPosition position) {
        return world.getBlockState(position.toBlockPos()).getBlock();
    }

    public static MapColor getBlockMapColor(BlockGetter world, BlockPosition position) {
        return getBlockMetadata(world, position).getMapColor(world, position.toBlockPos());
    }

    public static BlockState getBlockMetadata(BlockGetter world, BlockPosition position) {
        return world.getBlockState(position.toBlockPos());
    }

    @Nullable
    public static BlockEntity getBlockEntity(BlockGetter world, BlockPosition position) {
        return world.getBlockEntity(position.toBlockPos());
    }

    @Nullable
    public static BlockEntity getBlockEntity(BlockGetter world, EnvironmentHost host) {
        return getBlockEntity(world, BlockPosition.apply(host));
    }

    public static boolean isAirBlock(BlockGetter world, BlockPosition position) {
        final BlockState state = world.getBlockState(position.toBlockPos());
        return state.isAir();
    }

    // ----------------------------------------------------------------------- //
    // ExtendedWorld

    public static boolean blockExists(Level world, BlockPosition position) {
        return world.isLoaded(position.toBlockPos());
    }

    public static boolean breakBlock(Level world, BlockPosition position, boolean drops) {
        return world.destroyBlock(position.toBlockPos(), drops);
    }

    public static boolean breakBlock(Level world, BlockPosition position) {
        return breakBlock(world, position, true);
    }

    public static void destroyBlockInWorldPartially(Level world, int entityId, BlockPosition position, int progress) {
        world.destroyBlockProgress(entityId, position.toBlockPos(), progress);
    }

    public static boolean extinguishFire(Level world, Player player, BlockPosition position, Direction side) {
        final BlockPos pos = position.toBlockPos();
        final BlockState state = world.getBlockState(pos);
        // Formerly `state.getMaterial == Material.FIRE`; materials are gone in 1.20.
        if (state.is(BlockTags.FIRE)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        } else return false;
    }

    public static float getBlockHardness(Level world, BlockPosition position) {
        return world.getBlockState(position.toBlockPos()).getDestroySpeed(world, position.toBlockPos());
    }

    /**
     * Approximation of Forge's removed harvest levels using the vanilla
     * {@code needs_*_tool} tags: 3 = diamond, 2 = iron, 1 = stone, 0 = none needed.
     */
    public static int getBlockHarvestLevel(Level world, BlockPosition position) {
        // TODO(port): Forge harvest levels are gone (tiers + tags in 1.20); this is an approximation.
        final BlockState state = getBlockMetadata(world, position);
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    /**
     * Name of the tool type ("pickaxe", "axe", "shovel", "hoe") able to mine the
     * block, based on the vanilla {@code mineable/*} tags, or null if none.
     */
    @Nullable
    public static String getBlockHarvestTool(Level world, BlockPosition position) {
        // TODO(port): Forge ToolType is gone; derived from the mineable tags instead.
        final BlockState state = getBlockMetadata(world, position);
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "pickaxe";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "axe";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "shovel";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "hoe";
        return null;
    }

    public static int computeRedstoneSignal(Level world, BlockPosition position, Direction side) {
        return Math.max(isBlockProvidingPowerTo(world, position.offset(side), side), getIndirectPowerLevelTo(world, position.offset(side), side));
    }

    public static int isBlockProvidingPowerTo(Level world, BlockPosition position, Direction side) {
        return world.getDirectSignal(position.toBlockPos(), side);
    }

    public static int getIndirectPowerLevelTo(Level world, BlockPosition position, Direction side) {
        return world.getSignal(position.toBlockPos(), side);
    }

    public static void notifyBlockUpdate(Level world, BlockPos pos) {
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
    }

    public static void notifyBlockUpdate(Level world, BlockPosition position) {
        world.sendBlockUpdated(position.toBlockPos(), world.getBlockState(position.toBlockPos()), world.getBlockState(position.toBlockPos()), 3);
    }

    public static void notifyBlockUpdate(Level world, BlockPosition position, BlockState oldState, BlockState newState, int flags) {
        world.sendBlockUpdated(position.toBlockPos(), oldState, newState, flags);
    }

    public static void notifyBlockUpdate(Level world, BlockPosition position, BlockState oldState, BlockState newState) {
        notifyBlockUpdate(world, position, oldState, newState, 3);
    }

    public static void notifyBlockOfNeighborChange(Level world, BlockPosition position, Block block) {
        world.neighborChanged(position.toBlockPos(), block, position.toBlockPos());
    }

    @Deprecated
    public static void notifyBlocksOfNeighborChange(Level world, BlockPosition position, Block block, boolean updateObservers) {
        world.updateNeighborsAt(position.toBlockPos(), block);
    }

    public static void notifyBlocksOfNeighborChange(Level world, BlockPosition position, Block block, Direction side) {
        world.updateNeighborsAtExceptFromFacing(position.toBlockPos(), block, side);
    }

    public static void playAuxSFX(Level world, int id, BlockPosition position, int data) {
        world.levelEvent(id, position.toBlockPos(), data);
    }

    public static boolean setBlock(Level world, BlockPosition position, Block block) {
        return world.setBlockAndUpdate(position.toBlockPos(), block.defaultBlockState());
    }

    @Deprecated
    public static boolean setBlock(Level world, BlockPosition position, Block block, int metadata, int flag) {
        final List<BlockState> states = block.getStateDefinition().getPossibleStates();
        final BlockState state = (metadata >= 0 && metadata < states.size()) ? states.get(metadata) : block.defaultBlockState();
        return world.setBlock(position.toBlockPos(), state, flag);
    }

    public static boolean setBlockToAir(Level world, BlockPosition position) {
        return world.setBlockAndUpdate(position.toBlockPos(), Blocks.AIR.defaultBlockState());
    }

    public static boolean isLoaded(Level world, BlockPosition position) {
        return world.isLoaded(position.toBlockPos());
    }
}
