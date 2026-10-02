package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class Screen extends RedstoneAware {
    public final int tier;

    public Screen(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipBody(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        Pair<Integer, Integer> resolution = Settings.screenResolutionsByTier[tier];
        int depth = PackedColor.Depth.bits(Settings.screenDepthsByTier[tier]);
        addLines(tooltip, Tooltip.get(getClass().getSimpleName().toLowerCase(), resolution.getLeft(), resolution.getRight(), depth));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.tileentity.Screen(TileEntityTypes.SCREEN.get(), pos, state, tier);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Screen screen) {
            screen.delayUntilCheckForMultiBlock = 0;
        }
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        return rightClick(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ, false);
    }

    public boolean rightClick(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem,
                              Direction side, float hitX, float hitY, float hitZ, boolean force) {
        if (Wrench.holdsApplicableWrench(player, pos) && ArrayUtils.contains(getValidRotations(world, pos), side) && !force) return false;
        if (Items.get(heldItem) != null && Items.get(heldItem) == Items.get(Constants.ItemName.Analyzer)) return false;
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Screen screen) {
            if (screen.hasKeyboard() && (force || player.isCrouching() == screen.origin.invertTouchMode)) {
                // Yep, this GUI is actually purely client side (to trigger it from
                // the server we would have to give screens a "container", which we
                // do not want).
                if (world.isClientSide) ClientOnly.showGui(screen);
                return true;
            }
            if (screen.tier > 0 && side == screen.facing()) {
                if (world.isClientSide && ClientOnly.isLocalPlayer(player)) {
                    return screen.click(hitX, hitY, hitZ);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide) {
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Screen screen && screen.tier > 0 && screen.facing() == Direction.UP) {
                screen.walk(entity);
            }
            else super.stepOn(world, pos, state, entity);
        }
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (world.isClientSide && entity instanceof Arrow arrow && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Screen screen && screen.tier > 0) {
            double hitX = Math.max(0, Math.min(1, arrow.getX() - pos.getX()));
            double hitY = Math.max(0, Math.min(1, arrow.getY() - pos.getY()));
            double hitZ = Math.max(0, Math.min(1, arrow.getZ() - pos.getZ()));
            double absX = Math.abs(hitX - 0.5);
            double absY = Math.abs(hitY - 0.5);
            double absZ = Math.abs(hitZ - 0.5);
            Direction side;
            if (absX > absY && absX > absZ) {
                side = hitX < 0.5 ? Direction.WEST : Direction.EAST;
            }
            else if (absY > absZ) {
                side = hitY < 0.5 ? Direction.DOWN : Direction.UP;
            }
            else {
                side = hitZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
            }
            if (side == screen.facing()) {
                screen.shot(arrow);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Direction[] getValidRotations(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Screen screen) {
            Direction facing = screen.facing();
            if (facing == Direction.UP || facing == Direction.DOWN) return Direction.values();
            return Arrays.stream(Direction.values()).filter(d -> d != facing && d != facing.getOpposite()).toArray(Direction[]::new);
        }
        return super.getValidRotations(world, pos);
    }

    // Separate class so client classes are only loaded when actually used (on the client).
    private static final class ClientOnly {
        static void showGui(li.cil.oc.common.tileentity.Screen screen) {
            li.cil.oc.common.tileentity.Screen origin = screen.origin;
            Minecraft.getInstance().setScreen(new li.cil.oc.client.gui.Screen(origin.buffer(), screen.tier > 0,
                () -> origin.hasKeyboard(), () -> origin.buffer().isRenderingEnabled()));
        }

        static boolean isLocalPlayer(Player player) {
            return player == Minecraft.getInstance().player;
        }
    }
}
