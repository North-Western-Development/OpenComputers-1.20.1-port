package li.cil.oc.common.item.traits;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotRenderEvent.MountPoint;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.client.renderer.item.UpgradeRenderer;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.Tooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Base class of all OpenComputers items (the Scala trait was always mixed into
 * {@code Item}, so it became an abstract class). {@link li.cil.oc.common.item.HoverBoots}
 * is the only item that cannot extend it (it extends {@code ArmorItem}).
 * <p>
 * Tooltip chain (Scala linearization made explicit): {@link #appendHoverText} adds
 * the base tooltip, then the tier line for {@link ItemTier} items; subclasses
 * append their own lines after calling {@code super}.
 */
public abstract class SimpleItem extends Item implements li.cil.oc.api.driver.item.UpgradeRenderer {
    @Deprecated
    protected String unlocalizedName = getClass().getSimpleName().toLowerCase(Locale.ROOT);

    protected SimpleItem(Properties props) {
        super(props);
    }

    public ItemStack createItemStack(int amount) {
        return new ItemStack(this, amount);
    }

    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return "item.oc." + unlocalizedName;
    }

    /**
     * Formerly Forge's {@code doesSneakBypassUse}: when true, the block's {@code use} is
     * still invoked while the player is sneaking with this item. Renamed so it does not
     * accidentally override the Forge method; implemented for both loaders by
     * {@link li.cil.oc.common.item.ItemEvents}.
     */
    public boolean sneakBypassesUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.DiskDrive;
    }

    /**
     * Formerly Forge's {@code onItemUseFirst}: called before the block is activated.
     * Driven by {@link li.cil.oc.common.item.ItemEvents} (Architectury RIGHT_CLICK_BLOCK).
     */
    @Deprecated
    public InteractionResult onItemUseFirst(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Deprecated
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        final ItemStack stack = ctx.getItemInHand();
        if (stack != null) {
            final Level world = ctx.getLevel();
            final BlockPosition pos = BlockPosition.apply(ctx.getClickedPos(), world);
            final Vec3 hitPos = ctx.getClickLocation();
            final boolean success = onItemUse(stack, ctx.getPlayer(), pos, ctx.getClickedFace(),
                (float) (hitPos.x - pos.x), (float) (hitPos.y - pos.y), (float) (hitPos.z - pos.z));
            return success ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
        }
        return super.useOn(ctx);
    }

    @Deprecated
    public boolean onItemUse(ItemStack stack, Player player, BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
        return false;
    }

    @Deprecated
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (stack != null) {
            return use(stack, world, player);
        }
        return super.use(world, player, hand);
    }

    @Deprecated
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }

    protected int tierFromDriver(ItemStack stack) {
        final li.cil.oc.api.driver.DriverItem driver = li.cil.oc.api.Driver.driverFor(stack);
        return driver != null ? driver.tier(stack) : 0;
    }

    protected Optional<String> tooltipName() {
        return Optional.ofNullable(unlocalizedName);
    }

    protected List<Object> tooltipData() {
        return Collections.emptyList();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        final Optional<String> name = tooltipName();
        if (name.isPresent()) {
            for (String curr : Tooltip.get(name.get(), tooltipData().toArray())) {
                tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
            }
            tooltipExtended(stack, tooltip);
        } else {
            for (String curr : Tooltip.get(getClass().getSimpleName().toLowerCase(Locale.ROOT))) {
                tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
            }
        }
        tooltipCosts(stack, tooltip);
        // traits.ItemTier (was a stacked trait override calling super first).
        if (this instanceof ItemTier && flag.isAdvanced()) {
            tooltip.add(Component.literal(Localization.Tooltip.Tier(tierFromDriver(stack) + 1)).setStyle(Tooltip.DefaultStyle));
        }
    }

    // For stuff that goes to the normal 'extended' tooltip, before the costs.
    protected void tooltipExtended(ItemStack stack, List<Component> tooltip) {
    }

    protected void tooltipCosts(ItemStack stack, List<Component> tooltip) {
        appendNodeAddress(stack, tooltip);
    }

    public static void appendNodeAddress(ItemStack stack, List<Component> tooltip) {
        if (stack.hasTag() && stack.getTag().contains(Settings.namespace + "data")) {
            final CompoundTag data = stack.getTag().getCompound(Settings.namespace + "data");
            if (data.contains("node") && data.getCompound("node").contains("address")) {
                tooltip.add(Component.literal("§8" + data.getCompound("node").getString("address").substring(0, 13) + "...§7"));
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // Durability bar helpers (formerly Forge showDurabilityBar / getDurabilityForDisplay).

    /** Fraction of the bar that is "used up", like Forge's getDurabilityForDisplay. */
    public static int barWidth(double durabilityForDisplay) {
        final double d = Double.isNaN(durabilityForDisplay) ? 1.0 : Math.max(0.0, Math.min(1.0, durabilityForDisplay));
        return (int) Math.round(13.0 - d * 13.0);
    }

    /** Forge's default getRGBDurabilityForDisplay. */
    public static int barColor(double durabilityForDisplay) {
        final double d = Double.isNaN(durabilityForDisplay) ? 1.0 : Math.max(0.0, Math.min(1.0, durabilityForDisplay));
        return Mth.hsvToRgb((float) Math.max(0.0, 1.0 - d) / 3.0F, 1.0F, 1.0F);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public String computePreferredMountPoint(ItemStack stack, Robot robot, Set<String> availableMountPoints) {
        return UpgradeRenderer.preferredMountPoint(stack, availableMountPoints);
    }

    @Override
    public void render(PoseStack matrix, MultiBufferSource buffer, ItemStack stack, MountPoint mountPoint, Robot robot, float pt) {
        UpgradeRenderer.render(matrix, buffer, stack, mountPoint);
    }
}
