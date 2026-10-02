package li.cil.oc.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.Loot;
import li.cil.oc.common.component.TerminalServer;

/**
 * Client-only part of {@link EventHandler}. Kept out of the common class so the
 * dedicated server never resolves client classes.
 */
public final class ClientEventHandler {
    private ClientEventHandler() {
    }

    public static void register() {
        ClientTickEvent.CLIENT_PRE.register(client -> EventHandler.runPendingClient());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> clientLoggedIn());
        // Formerly the client side branch of WorldEvent.Unload.
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> TerminalServer.loaded.clear());
    }

    private static void clientLoggedIn() {
        li.cil.oc.client.renderer.PetRenderer.isInitialized = false;
        li.cil.oc.client.renderer.PetRenderer.hidden.clear();
        Loot.disksForClient.clear();
        Loot.disksForCyclingClient.clear();

        Sound.startLoop(null, "computer_running", 0f, 0);
        EventHandler.scheduleServer(() -> Sound.stopLoop(null));
    }
}
