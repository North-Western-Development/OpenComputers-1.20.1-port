package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.common.platform.IntegrationPlatform;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DisintegrationProvider extends ScalaProvider {
    public static final DisintegrationProvider INSTANCE = new DisintegrationProvider();

    private DisintegrationProvider() {
        super("c4e7e3c2-8069-4fbb-b08e-74b1bddcdfe7");
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        return Collections.singletonList(new DisintegrationBehavior(player));
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new DisintegrationBehavior(player);
    }

    public static class DisintegrationBehavior extends AbstractBehavior {
        public Map<BlockPosition, SlowBreakInfo> breakingMap = new HashMap<>();
        public Map<BlockPosition, SlowBreakInfo> breakingMapNew = new HashMap<>();

        public DisintegrationBehavior(Player player) {
            super(player);
        }

        // Note: intentionally not overriding getNameHint. Gotta find this one manually!

        @Override
        public void onDisable(DisableReason reason) {
            final Level world = player.level();
            for (BlockPosition pos : breakingMap.keySet()) {
                ExtendedWorld.destroyBlockInWorldPartially(world, pos.hashCode(), pos, -1);
            }
            breakingMap.clear();
        }

        @Override
        public void update() {
            final Level world = player.level();
            if (world.isClientSide) return;
            if (PlatformHooks.isFakePlayer(player)) return; // Nope
            if (!(player instanceof ServerPlayer playerMP)) return; // Not available for fake players, sorry :P

            final long now = world.getGameTime();

            // Check blocks in range.
            final BlockPosition blockPos = BlockPosition.apply(player);
            final int actualRange = Settings.get().nanomachineDisintegrationRange * Nanomachines.getController(player).getInputCount(this);
            for (int x = -actualRange; x <= actualRange; x++) {
                for (int y = 0; y <= actualRange * 2; y++) {
                    for (int z = -actualRange; z <= actualRange; z++) {
                        final BlockPosition pos = BlockPosition.apply(blockPos.offset(x, y, z));
                        final SlowBreakInfo existing = breakingMap.get(pos);
                        if (existing != null) {
                            if (existing.checkTool(player)) {
                                breakingMapNew.put(pos, existing);
                                existing.update(world, player, now);
                            }
                            // else: Tool changed, pretend block doesn't exist for this tick.
                        } else {
                            final boolean allowed = IntegrationPlatform.canAttackBlock(player, pos.toBlockPos(), player.getDirection());
                            final boolean placingRestricted = world.getLevelData() instanceof ServerLevelData srvInfo
                                    ? srvInfo.getGameType().isBlockPlacingRestricted()
                                    : true; // Means it's not a server world (somehow).
                            final boolean adventureOk = !placingRestricted || player.mayUseItemAt(pos.toBlockPos(), null, player.getItemInHand(InteractionHand.MAIN_HAND));
                            if (allowed && adventureOk && !ExtendedWorld.isAirBlock(world, pos)) {
                                final BlockState blockState = world.getBlockState(pos.toBlockPos());
                                final float hardness = blockState.getDestroyProgress(player, world, pos.toBlockPos());
                                if (hardness > 0) {
                                    final int timeToBreak = (int) (1 / hardness);
                                    if (timeToBreak < 20 * 30) {
                                        final ItemStack tool = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                                        final SlowBreakInfo info = new SlowBreakInfo(now, now + timeToBreak, pos, tool, blockState);
                                        ExtendedWorld.destroyBlockInWorldPartially(world, pos.hashCode(), pos, 0);
                                        breakingMapNew.put(pos, info);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Handle completed breaks.
            for (Map.Entry<BlockPosition, SlowBreakInfo> entry : breakingMap.entrySet()) {
                if (entry.getValue().timeBroken < now) {
                    breakingMapNew.remove(entry.getKey());
                    entry.getValue().finish(world, playerMP);
                }
            }

            // Handle aborted / incomplete breaks.
            final Set<BlockPosition> aborted = new HashSet<>(breakingMap.keySet());
            aborted.removeAll(breakingMapNew.keySet());
            for (BlockPosition pos : aborted) {
                ExtendedWorld.destroyBlockInWorldPartially(world, pos.hashCode(), pos, -1);
            }

            final Map<BlockPosition, SlowBreakInfo> tmp = breakingMap;
            tmp.clear();
            breakingMap = breakingMapNew;
            breakingMapNew = tmp;
        }
    }

    public static class SlowBreakInfo {
        public final long timeStarted;
        public final long timeBroken;
        public final BlockPosition pos;
        /** Copy of the tool used when starting to break; {@link ItemStack#EMPTY} for none. */
        public final ItemStack originalTool;
        public final BlockState blockState;

        public int lastDamageSent = 0;

        public SlowBreakInfo(long timeStarted, long timeBroken, BlockPosition pos, ItemStack originalTool, BlockState blockState) {
            this.timeStarted = timeStarted;
            this.timeBroken = timeBroken;
            this.pos = pos;
            this.originalTool = originalTool;
            this.blockState = blockState;
        }

        public boolean checkTool(Player player) {
            final ItemStack currentTool = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!currentTool.isEmpty() && !originalTool.isEmpty()) {
                return currentTool.getItem() == originalTool.getItem() && (currentTool.isDamageableItem() || currentTool.getDamageValue() == originalTool.getDamageValue());
            }
            return currentTool.isEmpty() && originalTool.isEmpty();
        }

        public void update(Level world, Player player, long now) {
            final long timeTotal = timeBroken - timeStarted;
            if (timeTotal > 0) {
                final long timeTaken = now - timeStarted;
                final long damage = 10 * timeTaken / timeTotal;
                if (damage != lastDamageSent) {
                    lastDamageSent = (int) damage;
                    ExtendedWorld.destroyBlockInWorldPartially(world, pos.hashCode(), pos, lastDamageSent);
                }
            }
        }

        public void finish(Level world, ServerPlayer player) {
            final boolean sameBlock = world.getBlockState(pos.toBlockPos()) == blockState;
            if (sameBlock) {
                ExtendedWorld.destroyBlockInWorldPartially(world, pos.hashCode(), pos, -1);
                if (player.gameMode.destroyBlock(pos.toBlockPos())) {
                    ExtendedWorld.playAuxSFX(world, 2001, pos, Block.getId(blockState));
                }
            }
        }
    }
}
