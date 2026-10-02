package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Packet;
import net.minecraft.nbt.CompoundTag;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Stateful trait: implementing classes hold the state, e.g.
 * <pre>
 * private Optional&lt;String&gt; wakeMessage = Optional.empty();
 * private boolean wakeMessageFuzzy = false;
 * public Optional&lt;String&gt; wakeMessage() { return wakeMessage; }
 * public void setWakeMessage(Optional&lt;String&gt; value) { wakeMessage = value; }
 * public boolean wakeMessageFuzzy() { return wakeMessageFuzzy; }
 * public void setWakeMessageFuzzy(boolean value) { wakeMessageFuzzy = value; }
 * </pre>
 */
public interface WakeMessageAware extends NetworkAware {
    String WakeMessageTag = "wakeMessage";

    String WakeMessageFuzzyTag = "wakeMessageFuzzy";

    Optional<String> wakeMessage();

    void setWakeMessage(Optional<String> value);

    boolean wakeMessageFuzzy();

    void setWakeMessageFuzzy(boolean value);

    @Callback(direct = true, doc = "function():string, boolean -- Get the current wake-up message.")
    default Object[] getWakeMessage(Context context, Arguments args) {
        return new Object[]{wakeMessage().orElse(null), wakeMessageFuzzy()};
    }

    @Callback(doc = "function(message:string[, fuzzy:boolean]):string -- Set the wake-up message and whether to ignore additional data/parameters.")
    default Object[] setWakeMessage(Context context, Arguments args) {
        final Optional<String> oldMessage = wakeMessage();
        final boolean oldFuzzy = wakeMessageFuzzy();

        if (args.optAny(0, null) == null)
            setWakeMessage(Optional.empty());
        else
            setWakeMessage(Optional.of(args.checkString(0)));
        setWakeMessageFuzzy(args.optBoolean(1, wakeMessageFuzzy()));

        return new Object[]{oldMessage.orElse(null), oldFuzzy};
    }

    default boolean isPacketAccepted(Packet packet, double distance) {
        return true;
    }

    default void receivePacket(Packet packet, double distance, EnvironmentHost host) {
        if (!Objects.equals(packet.source(), node().address()) && (packet.destination() == null || packet.destination().equals(node().address()))) {
            if (isPacketAccepted(packet, distance)) {
                final Object[] data = packet.data();
                final Object[] signal = new Object[4 + data.length];
                signal[0] = "modem_message";
                signal[1] = packet.source();
                signal[2] = packet.port();
                signal[3] = distance;
                System.arraycopy(data, 0, signal, 4, data.length);
                node().sendToReachable("computer.signal", signal);
            }

            // Accept wake-up messages regardless of port because we close all ports
            // when our computer shuts down.
            final Object[] data = packet.data();
            boolean wakeup = false;
            if (data.length > 0 && (data.length == 1 || wakeMessageFuzzy())) {
                final Object first = data[0];
                final String message;
                if (first instanceof byte[] bytes) message = new String(bytes, StandardCharsets.UTF_8);
                else if (first instanceof String s) message = s;
                else message = null;
                wakeup = message != null && wakeMessage().map(message::equals).orElse(false);
            }
            if (wakeup) {
                if (host instanceof Context ctx) ctx.start();
                else node().sendToNeighbors("computer.start");
            }
        }
    }

    default void loadWakeMessage(CompoundTag nbt) {
        if (nbt.contains(WakeMessageTag)) {
            setWakeMessage(Optional.of(nbt.getString(WakeMessageTag)));
        }
        setWakeMessageFuzzy(nbt.getBoolean(WakeMessageFuzzyTag));
    }

    default void saveWakeMessage(CompoundTag nbt) {
        wakeMessage().ifPresent(m -> nbt.putString(WakeMessageTag, m));
        nbt.putBoolean(WakeMessageFuzzyTag, wakeMessageFuzzy());
    }
}
