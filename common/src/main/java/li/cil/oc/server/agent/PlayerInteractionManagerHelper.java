package li.cil.oc.server.agent;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.mixin.ServerPlayerGameModeAccessor;
import li.cil.oc.common.platform.AgentPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

public final class PlayerInteractionManagerHelper {
    private PlayerInteractionManagerHelper() {
    }

    /**
     * Experience captured from blocks broken by an agent with an experience
     * upgrade; non-null only while {@link #blockRemoving} runs on this thread.
     * Filled by {@code li.cil.oc.common.mixin.BlockPopExperienceMixin}.
     */
    private static final ThreadLocal<int[]> capturedExperience = new ThreadLocal<>();

    /**
     * Called from {@code Block.popExperience}; returns true if the experience
     * was captured by an agent (and must not be dropped).
     */
    public static boolean tryCaptureExperience(int amount) {
        final int[] captured = capturedExperience.get();
        if (captured == null) return false;
        captured[0] += amount;
        return true;
    }

    private static boolean isDestroyingBlock(Player player) {
        try {
            return ((ServerPlayerGameModeAccessor) player.gameMode).oc$isDestroyingBlock();
        } catch (Exception e) {
            return true;
        }
    }

    private static void handleBlockBreakAction(Player player, BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction side) {
        player.gameMode.handleBlockBreakAction(pos, action, side, player.level().getMaxBuildHeight(), 0);
    }

    public static boolean onBlockClicked(Player player, BlockPos pos, Direction side) {
        if (isDestroyingBlock(player)) {
            handleBlockBreakAction(player, pos, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side);
        }
        handleBlockBreakAction(player, pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side);
        return isDestroyingBlock(player);
    }

    public static boolean updateBlockRemoving(Player player) {
        if (!isDestroyingBlock(player))
            return false;
        player.gameMode.tick();
        return isDestroyingBlock(player);
    }

    private static boolean hasExperienceUpgrade(Player player) {
        final Node machineNode = player.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node != null && node.canBeReachedFrom(machineNode) &&
                    (node.host() instanceof li.cil.oc.common.item.UpgradeExperience ||
                            node.host() instanceof li.cil.oc.server.component.UpgradeExperience)) {
                return true;
            }
        }
        return false;
    }

    // returns exp gained from removing the block, -1 if block not removed
    // redone here because the interaction manager just drops the xp on the ground
    public static int blockRemoving(Player player, BlockPos pos) {
        if (!isDestroyingBlock(player)) {
            return -1;
        }

        // Formerly a Forge PlayerEvent.BreakSpeed listener forcing the break to
        // complete, and a BlockEvent.BreakEvent listener capturing experience.
        AgentPlatform.ensureInstantBreakSupport();
        final int[] captured = hasExperienceUpgrade(player) ? new int[1] : null;
        final int[] previousCapture = capturedExperience.get();
        player.instantBreak = true;
        capturedExperience.set(captured);
        try {
            handleBlockBreakAction(player, pos, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, null);
            return captured != null ? captured[0] : 0;
        } catch (Exception e) {
            OpenComputers.log.info("an exception was thrown while trying to call blockRemoving: " + e.getMessage());
            handleBlockBreakAction(player, pos, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, null);
            return -1;
        } finally {
            player.instantBreak = false;
            capturedExperience.set(previousCapture);
        }
    }
}
