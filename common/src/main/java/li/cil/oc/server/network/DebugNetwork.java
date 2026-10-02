package li.cil.oc.server.network;

import li.cil.oc.api.network.Packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public final class DebugNetwork {
    public static final Set<DebugNode> cards = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private DebugNetwork() {
    }

    public static void add(DebugNode card) {
        cards.add(card);
    }

    public static void remove(DebugNode card) {
        cards.remove(card);
    }

    public static Optional<DebugNode> getEndpoint(String tunnel) {
        final ArrayList<DebugNode> snapshot;
        synchronized (cards) {
            snapshot = new ArrayList<>(cards);
        }
        for (DebugNode card : snapshot) {
            if (card != null && Objects.equals(card.address(), tunnel)) return Optional.of(card);
        }
        return Optional.empty();
    }

    public interface DebugNode {
        String address();

        void receivePacket(Packet packet);
    }
}
