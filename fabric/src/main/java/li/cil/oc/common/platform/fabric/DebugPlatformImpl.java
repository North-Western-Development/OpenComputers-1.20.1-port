package li.cil.oc.common.platform.fabric;

import li.cil.oc.server.command.DebugCommands;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

public final class DebugPlatformImpl {
    private DebugPlatformImpl() {
    }

    public static void registerProtectionHooks() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
            DebugCommands.isProtected(entity) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
            DebugCommands.isProtected(level, hit.getBlockPos()) ? InteractionResult.FAIL : InteractionResult.PASS);
    }
}
