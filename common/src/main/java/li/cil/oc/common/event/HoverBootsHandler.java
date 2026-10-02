package li.cil.oc.common.event;

import dev.architectury.event.events.common.TickEvent;
import li.cil.oc.Settings;
import li.cil.oc.common.item.HoverBoots;
import li.cil.oc.common.platform.PlatformHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hover boots behaviour. The per-tick part uses Architectury's player tick event; jumping and
 * falling are forwarded from {@link li.cil.oc.common.mixin.LivingEntityHoverBootsMixin}
 * (Forge's LivingJumpEvent / LivingFallEvent have no Architectury equivalent).
 */
public final class HoverBootsHandler {
    // Was stored in Forge's (non-persisted) player data; a transient map is equivalent.
    private static final Map<Player, Boolean> hasHoverBootsState = Collections.synchronizedMap(new WeakHashMap<>());

    private static final float DefaultStepHeight = 0.6f;

    private HoverBootsHandler() {
    }

    public static void register() {
        TickEvent.PLAYER_PRE.register(HoverBootsHandler::onPlayerTick);
    }

    public static void onPlayerTick(Player player) {
        if (PlatformHooks.isFakePlayer(player)) return;
        boolean hadHoverBoots = hasHoverBootsState.getOrDefault(player, false);
        boolean hasHoverBoots = false;
        if (!player.isCrouching()) {
            for (ItemStack stack : player.getInventory().armor) {
                if (!stack.isEmpty() && stack.getItem() instanceof HoverBoots boots) {
                    if (Settings.get().ignorePower) {
                        hasHoverBoots = true;
                    }
                    else {
                        if (player.onGround() && !player.isCreative() && player.level().getGameTime() % Settings.get().tickFrequency == 0) {
                            double velocity = player.getDeltaMovement().lengthSqr();
                            if (velocity > 0.015f) {
                                boots.charge(stack, -Settings.get().hoverBootMove, false);
                            }
                        }
                        hasHoverBoots = boots.getCharge(stack) > 0;
                    }
                    if (hasHoverBoots) break;
                }
            }
        }
        if (hasHoverBoots != hadHoverBoots) {
            hasHoverBootsState.put(player, hasHoverBoots);
            // Note: 1.16 restored 0.5 here; the 1.20.1 vanilla player step height is 0.6.
            player.setMaxUpStep(hasHoverBoots ? 1f : DefaultStepHeight);
        }
        if (hasHoverBoots && !player.onGround() && player.fallDistance < 5 && player.getDeltaMovement().y < 0) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.9, 1));
        }
    }

    /**
     * Called at the end of {@code LivingEntity.jumpFromGround}.
     */
    public static void onLivingJump(LivingEntity entity) {
        if (entity instanceof Player player && !PlatformHooks.isFakePlayer(player) && !player.isCrouching()) {
            ItemStack stack = findBoots(player);
            if (stack.isEmpty()) return;
            HoverBoots boots = (HoverBoots) stack.getItem();
            double hoverJumpCost = -Settings.get().hoverBootJump;
            boolean isCreative = Settings.get().ignorePower || player.isCreative();
            if (isCreative || boots.charge(stack, hoverJumpCost, true) == 0) {
                if (!isCreative) boots.charge(stack, hoverJumpCost, false);
                Vec3 motion = player.getDeltaMovement();
                if (player.isSprinting())
                    player.push(motion.x * 0.5, 0.4, motion.z * 0.5);
                else
                    player.push(0, 0.4, 0);
            }
        }
    }

    /**
     * Called at the start of {@code LivingEntity.causeFallDamage}; returns the (possibly reduced) fall distance.
     */
    public static float onLivingFall(LivingEntity entity, float distance) {
        if (distance > 3 && entity instanceof Player player && !PlatformHooks.isFakePlayer(player)) {
            ItemStack stack = findBoots(player);
            if (stack.isEmpty()) return distance;
            HoverBoots boots = (HoverBoots) stack.getItem();
            double hoverFallCost = -Settings.get().hoverBootAbsorb;
            boolean isCreative = Settings.get().ignorePower || player.isCreative();
            if (isCreative || boots.charge(stack, hoverFallCost, true) == 0) {
                if (!isCreative) boots.charge(stack, hoverFallCost, false);
                return distance * 0.3f;
            }
        }
        return distance;
    }

    private static ItemStack findBoots(Player player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty() && stack.getItem() instanceof HoverBoots) return stack;
        }
        return ItemStack.EMPTY;
    }
}
