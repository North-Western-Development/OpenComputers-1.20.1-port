package li.cil.oc.server.agent;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import javax.annotation.Nullable;

/**
 * Connection without a channel, used for the agents' fake players. Swallows
 * all packets (the vanilla implementation would queue them up forever).
 */
public final class FakeNetworkManager extends Connection {
    public static final FakeNetworkManager INSTANCE = new FakeNetworkManager();

    public FakeNetworkManager() {
        super(PacketFlow.CLIENTBOUND);
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
    }
}
