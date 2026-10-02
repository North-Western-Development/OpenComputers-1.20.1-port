package li.cil.oc.common.block;

import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.Colored;
import li.cil.oc.common.tileentity.traits.Inventory;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.Color;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

import java.util.List;

/**
 * Base class of all OC blocks.
 * <p>
 * The Scala block traits (GUI, PowerAcceptor, StateAware) were stackable overrides on top of this
 * class. In Java they are marker interfaces in {@code common.block.traits}, and the behaviour they
 * added is implemented here guarded by {@code instanceof} checks.
 */
public abstract class SimpleBlock extends BaseEntityBlock {
    private static final BlockEntityTicker<BlockEntity> TICKER = (level, pos, state, blockEntity) -> {
        if (blockEntity instanceof Tickable && blockEntity instanceof TileEntity tileEntity) {
            tileEntity.updateEntity();
        }
    };

    protected final Direction[] validRotations_ = new Direction[]{Direction.UP, Direction.DOWN};

    protected SimpleBlock(Properties props) {
        super(props);
    }

    public ItemStack createItemStack(int amount) {
        return new ItemStack(this, amount);
    }

    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Whether the block entity is Tickable is only known per instance; non-tickable ones simply no-op.
        return (BlockEntityTicker<T>) TICKER;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ----------------------------------------------------------------------- //
    // BlockItem
    // ----------------------------------------------------------------------- //

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
        tooltipHead(stack, world, tooltip, flag);
        tooltipBody(stack, world, tooltip, flag);
        tooltipTail(stack, world, tooltip, flag);
    }

    protected void tooltipHead(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
    }

    protected void tooltipBody(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
        addLines(tooltip, Tooltip.get(getClass().getSimpleName().toLowerCase()));
    }

    protected void tooltipTail(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
        if (this instanceof PowerAcceptor acceptor) {
            addLines(tooltip, Tooltip.extended("poweracceptor", (int) acceptor.energyThroughput()));
        }
    }

    protected static void addLines(List<Component> tooltip, Iterable<String> lines) {
        for (String curr : lines) {
            tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
        }
    }

    // ----------------------------------------------------------------------- //
    // Rotation
    // ----------------------------------------------------------------------- //

    public Direction getFacing(BlockGetter world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof Rotatable tileEntity) return tileEntity.facing();
        return Direction.SOUTH;
    }

    public boolean setFacing(Level world, BlockPos pos, Direction value) {
        if (world.getBlockEntity(pos) instanceof Rotatable rotatable) {
            rotatable.setFromFacing(value);
            return true;
        }
        return false;
    }

    public boolean setRotationFromEntityPitchAndYaw(Level world, BlockPos pos, Entity value) {
        if (world.getBlockEntity(pos) instanceof Rotatable rotatable) {
            rotatable.setFromEntityPitchAndYaw(value);
            return true;
        }
        return false;
    }

    public Direction toLocal(BlockGetter world, BlockPos pos, Direction value) {
        if (world.getBlockEntity(pos) instanceof Rotatable rotatable) return rotatable.toLocal(value);
        return value;
    }

    // ----------------------------------------------------------------------- //
    // Block
    // ----------------------------------------------------------------------- //

    // Note: canHarvestBlock/getHarvestTool are gone; all OC blocks are in minecraft:mineable/pickaxe
    // and do not require the correct tool for drops.

    @Nullable
    public Direction[] getValidRotations(Level world, BlockPos pos) {
        return validRotations_;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof Inventory inventory) {
            builder = builder.withDynamicDrop(LootFunctions.DYN_VOLATILE_CONTENTS, inventory::forAllLoot);
        }
        return super.getDrops(state, builder);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide && player.isCreative() && world.getBlockEntity(pos) instanceof Inventory inventory) {
            inventory.dropAllSlots();
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    /**
     * Replacement for Forge's {@code removedByPlayer}. Called on the server from
     * {@link li.cil.oc.common.event.BlockBreakHandler} before a player breaks this block.
     *
     * @return false to prevent the block from being broken.
     */
    public boolean removedByPlayer(BlockState state, Level world, BlockPos pos, Player player) {
        return true;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        // GUI blocks open their menus with extra data via MenuRegistry.openExtendedMenu; the vanilla
        // path (Player.openMenu) does not support that.
        if (this instanceof GUI) return null;
        return super.getMenuProvider(state, world, pos);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return this instanceof li.cil.oc.common.block.traits.StateAware;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (this instanceof li.cil.oc.common.block.traits.StateAware) {
            return li.cil.oc.common.block.traits.StateAware.analogOutputSignal(world, pos);
        }
        return 0;
    }

    // ----------------------------------------------------------------------- //

    @Deprecated
    public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
        if (world.getBlockEntity(pos) instanceof Rotatable rotatable && rotatable.rotate(axis)) {
            world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (world.getBlockEntity(pos) instanceof Colored colored && Color.isDye(heldItem)) {
            colored.setColor(Color.rgbValues.get(Color.dyeColor(heldItem)));
            world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            if (!player.isCreative() && colored.consumesDye()) {
                heldItem.split(1);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        Vec3 loc = trace.getLocation();
        BlockPos hitPos = trace.getBlockPos();
        float x = (float) loc.x - hitPos.getX();
        float y = (float) loc.y - hitPos.getY();
        float z = (float) loc.z - hitPos.getZ();
        if (localOnBlockActivated(world, hitPos, player, hand, heldItem, trace.getDirection(), x, y, z)) {
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (this instanceof GUI gui) {
            if (!player.isCrouching()) {
                if (player instanceof ServerPlayer srvPlr && !world.isClientSide) {
                    gui.openGui(srvPlr, world, pos);
                }
                return true;
            }
        }
        return false;
    }
}
