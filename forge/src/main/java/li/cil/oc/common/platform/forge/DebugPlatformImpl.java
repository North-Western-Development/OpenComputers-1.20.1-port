package li.cil.oc.common.platform.forge;

import li.cil.oc.server.command.DebugCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public final class DebugPlatformImpl {
    private DebugPlatformImpl() {
    }

    public static void registerProtectionHooks() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.EntityInteract.class, event -> {
            if (DebugCommands.isProtected(event.getTarget())) event.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, event -> {
            if (DebugCommands.isProtected(event.getLevel(), event.getPos())) event.setCanceled(true);
        });
    }
}
