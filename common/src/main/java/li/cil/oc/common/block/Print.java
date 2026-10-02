package li.cil.oc.common.block;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class Print extends RedstoneAware {
    /**
     * Light emission of the print. Vanilla light emission is per block state, so the print's
     * light level (from its PrintData) is mirrored into this property (set in setPlacedBy).
     */
    public static final IntegerProperty Light = IntegerProperty.create("light", 0, 15);

    public Print(Properties props) {
        super(props.lightLevel(state -> state.getValue(Light)));
        registerDefaultState(stateDefinition.any().setValue(Light, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Light);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipBody(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        super.tooltipBody(stack, world, tooltip, advanced);
        PrintData data = new PrintData(stack);
        data.tooltip.ifPresent(s -> s.lines().forEach(line -> tooltip.add(Component.literal(line).setStyle(Tooltip.DefaultStyle))));
    }

    @Override
    protected void tooltipTail(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        super.tooltipTail(stack, world, tooltip, advanced);
        PrintData data = new PrintData(stack);
        if (data.isBeaconBase) {
            tooltip.add(Component.literal(Localization.Tooltip.PrintBeaconBase()).setStyle(Tooltip.DefaultStyle));
        }
        if (data.emitRedstone()) {
            tooltip.add(Component.literal(Localization.Tooltip.PrintRedstoneLevel(data.redstoneLevel)).setStyle(Tooltip.DefaultStyle));
        }
        if (data.emitLight()) {
            tooltip.add(Component.literal(Localization.Tooltip.PrintLightValue(data.lightLevel)).setStyle(Tooltip.DefaultStyle));
        }
    }

    /**
     * Updates the {@link #Light} block state property to match the print's data.
     * Should be called whenever the print's light level may have changed.
     */
    public static void updateLightLevel(Level world, BlockPos pos, int lightLevel) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof Print) {
            int clamped = Math.max(0, Math.min(15, lightLevel));
            if (state.getValue(Light) != clamped) {
                world.setBlock(pos, state.setValue(Light, clamped), 3);
            }
        }
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        // Prints have a dynamic shape, so this is not cached per state and may depend on the tile entity.
        if (world instanceof Level level && level.isLoaded(pos) && Settings.get().printsHaveOpacity
            && level.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print print) {
            return (int) (print.data.opacity() * 4);
        }
        return super.getLightBlock(state, world, pos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print print) {
            return print.data.createItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print print) {
            return print.shape();
        }
        return super.getShape(state, world, pos, ctx);
    }

    public int tickRate(Level world) {
        return 20;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print print) {
            if (print.state) print.toggleState();
        }
    }

    // TODO(port): per-position beacon base (Forge isBeaconBase) is gone; vanilla uses the
    // minecraft:beacon_base_blocks tag, which cannot depend on the print's data.
    @Deprecated
    public boolean isBeaconBase(BlockGetter world, BlockPos pos, BlockPos beacon) {
        return world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print print && print.data.isBeaconBase;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.PRINT.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print print) {
            return print.activate() ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
        }
        return super.use(state, world, pos, player, hand, trace);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print print && print.data.emitRedstone(print.state)) {
            world.updateNeighborsAt(pos, this);
            for (Direction side : Direction.values()) {
                world.updateNeighborsAt(pos.relative(side), this);
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Print tileEntity) {
            tileEntity.data.loadData(stack);
            tileEntity.updateShape();
            tileEntity.updateRedstone();
            updateLightLevel(world, pos, tileEntity.data.lightLevel);
            world.getLightEngine().checkBlock(pos);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof li.cil.oc.common.tileentity.Print tileEntity) {
            builder = builder.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f -> f.accept(tileEntity.data.createItemStack()));
        }
        return super.getDrops(state, builder);
    }
}
