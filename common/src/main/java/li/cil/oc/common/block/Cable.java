package li.cil.oc.common.block;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.block.property.PropertyCableConnection;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;

public class Cable extends SimpleBlock {
    // For Immibis Microblock support.
    public final Object ImmibisMicroblocks_TransformableBlockMarker = null;

    // For FMP part coloring.
    public Optional<Integer> colorMultiplierOverride = Optional.empty();

    public Cable(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().
            setValue(PropertyCableConnection.DOWN, PropertyCableConnection.Shape.NONE).
            setValue(PropertyCableConnection.UP, PropertyCableConnection.Shape.NONE).
            setValue(PropertyCableConnection.NORTH, PropertyCableConnection.Shape.NONE).
            setValue(PropertyCableConnection.SOUTH, PropertyCableConnection.Shape.NONE).
            setValue(PropertyCableConnection.WEST, PropertyCableConnection.Shape.NONE).
            setValue(PropertyCableConnection.EAST, PropertyCableConnection.Shape.NONE));
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyCableConnection.DOWN, PropertyCableConnection.UP,
            PropertyCableConnection.NORTH, PropertyCableConnection.SOUTH,
            PropertyCableConnection.WEST, PropertyCableConnection.EAST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int color = getConnectionColor(ctx.getItemInHand());
        BlockPos.MutableBlockPos fromPos = new BlockPos.MutableBlockPos();
        BlockState state = defaultBlockState();
        for (Direction fromSide : Direction.values()) {
            fromPos.setWithOffset(ctx.getClickedPos(), fromSide);
            BlockState fromState = ctx.getLevel().getBlockState(fromPos);
            state = updateState(state, null, color, fromSide, fromState, ctx.getLevel(), fromPos);
        }
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Cable t) {
            return t.createItemStack();
        }
        return createItemStack();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return shape(state);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block other, BlockPos otherPos, boolean moved) {
        if (world.isClientSide) return;
        BlockState newState = state;
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof li.cil.oc.common.tileentity.Cable) {
            BlockPos.MutableBlockPos fromPos = new BlockPos.MutableBlockPos();
            for (Direction fromSide : Direction.values()) {
                fromPos.setWithOffset(pos, fromSide);
                BlockState fromState = world.getBlockState(fromPos);
                newState = updateState(newState, tileEntity, -1, fromSide, fromState, world, fromPos);
            }
        }
        if (newState != state) world.setBlock(pos, newState, 0x13);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction fromSide, BlockState fromState, LevelAccessor world, BlockPos pos, BlockPos fromPos) {
        return updateState(state, world.getBlockEntity(pos), -1, fromSide, fromState, world, fromPos);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.CABLE.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Cable tileEntity) {
            tileEntity.fromItemStack(stack);
            state.updateNeighbourShapes(world, pos, 2);
        }
    }

    // ----------------------------------------------------------------------- //

    public static final double MIN = 0.375;
    public static final double MAX = 1 - MIN;

    public static final VoxelShape DefaultShape = Shapes.box(MIN, MIN, MIN, MAX, MAX, MAX);

    public static final VoxelShape[] CachedParts = new VoxelShape[]{
        Shapes.box(MIN, 0, MIN, MAX, MIN, MAX), // Down
        Shapes.box(MIN, MAX, MIN, MAX, 1, MAX), // Up
        Shapes.box(MIN, MIN, 0, MAX, MAX, MIN), // North
        Shapes.box(MIN, MIN, MAX, MAX, MAX, 1), // South
        Shapes.box(0, MIN, MIN, MIN, MAX, MAX), // West
        Shapes.box(MAX, MIN, MIN, 1, MAX, MAX)}; // East

    public static final VoxelShape[] CachedBounds;

    static {
        // 6 directions = 6 bits = 11111111b >> 2 = 0xFF >> 2
        CachedBounds = new VoxelShape[(0xFF >> 2) + 1];
        for (int mask = 0; mask <= 0xFF >> 2; mask++) {
            VoxelShape shape = DefaultShape;
            for (Direction side : Direction.values()) {
                if (((1 << side.get3DDataValue()) & mask) != 0) shape = Shapes.or(shape, CachedParts[side.ordinal()]);
            }
            CachedBounds[mask] = shape;
        }
    }

    public static int mask(Direction side, int value) {
        return value | (1 << side.get3DDataValue());
    }

    public static int mask(Direction side) {
        return mask(side, 0);
    }

    public static VoxelShape shape(BlockState state) {
        int result = 0;
        for (Direction side : Direction.values()) {
            PropertyCableConnection.Shape sideShape = state.getValue(PropertyCableConnection.BY_DIRECTION.get(side));
            if (sideShape != PropertyCableConnection.Shape.NONE) {
                result = mask(side, result);
            }
        }
        return CachedBounds[result];
    }

    public static BlockState updateState(BlockState state, @Nullable BlockEntity tileEntity, int defaultColor, Direction fromSide, BlockState fromState, BlockGetter world, BlockPos fromPos) {
        EnumProperty<PropertyCableConnection.Shape> prop = PropertyCableConnection.BY_DIRECTION.get(fromSide);
        BlockEntity neighborTileEntity = world.getBlockEntity(fromPos);
        if (neighborTileEntity != null && neighborTileEntity.getLevel() != null) {
            boolean neighborHasNode = hasNetworkNode(neighborTileEntity, fromSide.getOpposite());
            boolean canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity, defaultColor);
            // TODO(port): integration - Immibis microblock side checks (canConnectFromSideIM) dropped.
            if (neighborHasNode && canConnectColor) {
                if (fromState.is(state.getBlock())) {
                    return state.setValue(prop, PropertyCableConnection.Shape.CABLE);
                }
                else {
                    return state.setValue(prop, PropertyCableConnection.Shape.DEVICE);
                }
            }
        }
        return state.setValue(prop, PropertyCableConnection.Shape.NONE);
    }

    private static boolean hasNetworkNode(@Nullable BlockEntity tileEntity, Direction side) {
        if (tileEntity != null) {
            if (tileEntity instanceof li.cil.oc.common.tileentity.RobotProxy) return false;

            if (tileEntity instanceof SidedEnvironment host) {
                return tileEntity.getLevel().isClientSide ? host.canConnect(side) : host.sidedNode(side) != null;
            }

            if (tileEntity instanceof Environment) return true;
        }

        return false;
    }

    private static int lightGray() {
        return Color.rgbValues.get(DyeColor.LIGHT_GRAY);
    }

    private static int getConnectionColor(ItemStack stack) {
        int color = ItemColorizer.getColor(stack);
        return color == -1 ? lightGray() : color;
    }

    private static int getConnectionColor(@Nullable BlockEntity tileEntity) {
        if (tileEntity instanceof li.cil.oc.api.internal.Colored colored && colored.controlsConnectivity()) {
            return colored.getColor();
        }
        return lightGray();
    }

    private static boolean canConnectBasedOnColor(@Nullable BlockEntity te1, BlockEntity te2, int c1Default) {
        int c1 = te1 == null ? c1Default : getConnectionColor(te1);
        int c2 = getConnectionColor(te2);
        return c1 == c2 || c1 == lightGray() || c2 == lightGray();
    }
}
