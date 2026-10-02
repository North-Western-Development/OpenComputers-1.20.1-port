package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedEnumFacing;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class Keyboard extends SimpleBlock {
    // For Immibis Microblock support.
    public final Object ImmibisMicroblocks_TransformableBlockMarker = null;

    public Keyboard(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        Direction pitch = state.getValue(PropertyRotatable.Pitch);
        Direction yaw = state.getValue(PropertyRotatable.Yaw);
        Direction forward, up;
        if (pitch == Direction.DOWN || pitch == Direction.UP) {
            forward = pitch;
            up = yaw;
        }
        else {
            forward = yaw;
            up = Direction.UP;
        }
        Direction side = ExtendedEnumFacing.getRotation(forward, up);
        float[] sizes = new float[]{7f / 16f, 4f / 16f, 7f / 16f};
        float x0 = -up.getStepX() * sizes[1] - side.getStepX() * sizes[2] - forward.getStepX() * sizes[0];
        float x1 = up.getStepX() * sizes[1] + side.getStepX() * sizes[2] - forward.getStepX() * 0.5f;
        float y0 = -up.getStepY() * sizes[1] - side.getStepY() * sizes[2] - forward.getStepY() * sizes[0];
        float y1 = up.getStepY() * sizes[1] + side.getStepY() * sizes[2] - forward.getStepY() * 0.5f;
        float z0 = -up.getStepZ() * sizes[1] - side.getStepZ() * sizes[2] - forward.getStepZ() * sizes[0];
        float z1 = up.getStepZ() * sizes[1] + side.getStepZ() * sizes[2] - forward.getStepZ() * 0.5f;
        // Shapes.box requires min <= max, so normalize (AxisAlignedBB did that implicitly).
        return Shapes.box(0.5 + Math.min(x0, x1), 0.5 + Math.min(y0, y1), 0.5 + Math.min(z0, z1),
            0.5 + Math.max(x0, x1), 0.5 + Math.max(y0, y1), 0.5 + Math.max(z0, z1));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.KEYBOARD.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState prevState, boolean moved) {
        if (!world.isClientSide) {
            world.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof li.cil.oc.common.tileentity.Keyboard) {
            Network.joinOrCreateNetwork(tileEntity);
        }
        world.scheduleTick(pos, this, 10);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clicked = ctx.getClickedFace();
        Direction pitch, yaw;
        if (clicked == Direction.DOWN || clicked == Direction.UP) {
            pitch = clicked;
            yaw = ctx.getHorizontalDirection();
        }
        else {
            pitch = Direction.NORTH;
            yaw = clicked;
        }
        return defaultBlockState().setValue(PropertyRotatable.Pitch, pitch).setValue(PropertyRotatable.Yaw, yaw);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        // Check without the TE because this is called to check if the block may be placed.
        Direction pitch = state.getValue(PropertyRotatable.Pitch);
        Direction side = (pitch == Direction.UP || pitch == Direction.DOWN) ? pitch : state.getValue(PropertyRotatable.Yaw);
        BlockPos sidePos = pos.relative(side.getOpposite());
        if (!world.getBlockState(sidePos).isFaceSturdy(world, sidePos, side)) return false;
        if (world.getBlockEntity(sidePos) instanceof li.cil.oc.common.tileentity.Screen screen) {
            return screen.facing() != side;
        }
        return true;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        if (!canSurvive(world.getBlockState(pos), world, pos)) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            InventoryUtils.spawnStackInWorld(BlockPosition.apply(pos, world), Items.get(Constants.BlockName.Keyboard).createItemStack(1));
        }
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        Optional<AdjacencyInfo> info = adjacencyInfo(world, pos);
        return info.map(i -> i.screen.rightClick(world, i.pos, player, hand, heldItem, i.facing, 0, 0, 0, true)).orElse(false);
    }

    public record AdjacencyInfo(li.cil.oc.common.tileentity.Keyboard keyboard, Screen screen, BlockPos pos, Direction facing) {
    }

    public Optional<AdjacencyInfo> adjacencyInfo(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Keyboard keyboard) {
            BlockPos blockPos = pos.relative(keyboard.facing().getOpposite());
            if (world.getBlockState(blockPos).getBlock() instanceof Screen screen) {
                return Optional.of(new AdjacencyInfo(keyboard, screen, blockPos, keyboard.facing().getOpposite()));
            }
            // Special case #1: check for screen in front of the keyboard.
            Direction forward = (keyboard.facing() == Direction.UP || keyboard.facing() == Direction.DOWN) ? keyboard.yaw() : Direction.UP;
            BlockPos frontPos = pos.relative(forward);
            if (world.getBlockState(frontPos).getBlock() instanceof Screen screen) {
                return Optional.of(new AdjacencyInfo(keyboard, screen, frontPos, forward));
            }
            if (keyboard.facing() != Direction.UP && keyboard.facing() != Direction.DOWN) {
                // Special case #2: check for screen below keyboards on walls.
                BlockPos belowPos = pos.relative(forward.getOpposite());
                if (world.getBlockState(belowPos).getBlock() instanceof Screen screen) {
                    return Optional.of(new AdjacencyInfo(keyboard, screen, belowPos, forward.getOpposite()));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Direction[] getValidRotations(Level world, BlockPos pos) {
        return null;
    }
}
