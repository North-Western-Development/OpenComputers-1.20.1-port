package li.cil.oc.server.network;

import li.cil.oc.api.network.Packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

// Just because the name is so fancy!
public final class QuantumNetwork {
    public static final Map<String, Set<QuantumNode>> tunnels = new HashMap<>();

    private QuantumNetwork() {
    }

    public static void add(QuantumNode card) {
        synchronized (tunnels) {
            tunnels.computeIfAbsent(card.tunnel(), k -> Collections.newSetFromMap(new WeakHashMap<>())).add(card);
        }
    }

    public static void remove(QuantumNode card) {
        synchronized (tunnels) {
            final Set<QuantumNode> set = tunnels.get(card.tunnel());
            if (set != null) set.remove(card);
        }
    }

    public static List<QuantumNode> getEndpoints(String tunnel) {
        synchronized (tunnels) {
            final Set<QuantumNode> set = tunnels.get(tunnel);
            if (set == null) return Collections.emptyList();
            final List<QuantumNode> result = new ArrayList<>(set.size());
            for (QuantumNode node : set) if (node != null) result.add(node);
            return result;
        }
    }

    public interface QuantumNode {
        String tunnel();

        void receivePacket(Packet packet);
    }
}
