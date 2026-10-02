package li.cil.oc.common.event;

import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeAngel;

public final class AngelUpgradeHandler {
    private AngelUpgradeHandler() {
    }

    public static void register() {
        EventBus.INSTANCE.register(RobotPlaceInAirEvent.class, AngelUpgradeHandler::onPlaceInAir);
    }

    public static void onPlaceInAir(RobotPlaceInAirEvent e) {
        Node machineNode = e.agent.machine().node();
        boolean allowed = false;
        for (Node node : machineNode.reachableNodes()) {
            if (node.canBeReachedFrom(machineNode) && node.host() instanceof UpgradeAngel) {
                allowed = true;
                break;
            }
        }
        e.setAllowed(allowed);
    }
}
