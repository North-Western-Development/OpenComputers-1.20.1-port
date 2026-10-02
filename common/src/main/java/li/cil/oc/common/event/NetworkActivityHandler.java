package li.cil.oc.common.event;

import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.common.tileentity.Case;
import li.cil.oc.server.component.Server;

public final class NetworkActivityHandler {
    private NetworkActivityHandler() {
    }

    public static void register() {
        EventBus.INSTANCE.register(NetworkActivityEvent.Server.class, NetworkActivityHandler::onNetworkActivity);
        EventBus.INSTANCE.register(NetworkActivityEvent.Client.class, NetworkActivityHandler::onNetworkActivity);
    }

    public static void onNetworkActivity(NetworkActivityEvent.Server e) {
        if (e.getBlockEntity() instanceof Rack t) {
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                if (t.getMountable(slot) instanceof Server server) {
                    boolean containsNode = server.componentSlot(e.getNode().address()) >= 0;
                    if (containsNode) {
                        server.lastNetworkActivity = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                }
            }
        }
    }

    public static void onNetworkActivity(NetworkActivityEvent.Client e) {
        if (e.getBlockEntity() instanceof Case t) {
            t.lastNetworkActivity = System.currentTimeMillis();
        }
    }
}
