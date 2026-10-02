package li.cil.oc.common.event;

import li.cil.oc.api.Network;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.WirelessNetworkCard;

public final class WirelessNetworkCardHandler {
    private WirelessNetworkCardHandler() {
    }

    public static void register() {
        EventBus.INSTANCE.register(RobotMoveEvent.Post.class, WirelessNetworkCardHandler::onMove);
    }

    public static void onMove(RobotMoveEvent.Post e) {
        Node machineNode = e.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.host() instanceof WirelessNetworkCard card) Network.updateWirelessNetwork(card);
        }
    }
}
