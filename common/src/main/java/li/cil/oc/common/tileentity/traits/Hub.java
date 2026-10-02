package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.MovingAverage;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Scala vars became accessors: {@code maxQueueSize()/setMaxQueueSize}, {@code relayDelay()/setRelayDelay},
 * {@code relayAmount()/setRelayAmount}, {@code relayCooldown()/setRelayCooldown}; vals
 * {@code plugs()}, {@code queue()}, {@code packetsPerCycleAvg()}. The inner class {@code Plug} is
 * {@link Hub.Plug} (with an explicit {@code hub} reference).
 */
public interface Hub extends Environment, SidedEnvironment, Tickable {
    final class State {
        public volatile Plug[] plugs;
        public final ArrayDeque<Pair<Optional<Direction>, Packet>> queue = new ArrayDeque<>();
        public volatile int maxQueueSize;
        public volatile int relayDelay;
        public volatile int relayAmount;
        public volatile int relayCooldown = -1;
        // 20 cycles
        public final MovingAverage packetsPerCycleAvg = new MovingAverage(20);

        public State(Hub hub) {
            maxQueueSize = hub.queueBaseSize();
            relayDelay = hub.relayBaseDelay();
            relayAmount = hub.relayBaseAmount();
        }
    }

    String PlugsTag = Settings.namespace + "plugs";
    String QueueTag = Settings.namespace + "queue";
    String SideTag = "side";
    String RelayCooldownTag = Settings.namespace + "relayCooldown";

    /** Provided by {@link TileEntity}. */
    State hubState();

    @Override
    default Node node() {
        return null;
    }

    @Override
    default boolean isConnected() {
        for (Plug plug : plugs()) {
            if (plug != null && plug.node != null && plug.node.address() != null && plug.node.network() != null) return true;
        }
        return false;
    }

    default Plug[] plugs() {
        final State state = hubState();
        if (state.plugs == null) {
            synchronized (state) {
                if (state.plugs == null) {
                    final Direction[] sides = Direction.values();
                    final Plug[] plugs = new Plug[sides.length];
                    for (Direction side : sides) plugs[side.ordinal()] = createPlug(side);
                    state.plugs = plugs;
                }
            }
        }
        return state.plugs;
    }

    default ArrayDeque<Pair<Optional<Direction>, Packet>> queue() {
        return hubState().queue;
    }

    default int maxQueueSize() {
        return hubState().maxQueueSize;
    }

    default void setMaxQueueSize(int value) {
        hubState().maxQueueSize = value;
    }

    default int relayDelay() {
        return hubState().relayDelay;
    }

    default void setRelayDelay(int value) {
        hubState().relayDelay = value;
    }

    default int relayAmount() {
        return hubState().relayAmount;
    }

    default void setRelayAmount(int value) {
        hubState().relayAmount = value;
    }

    default int relayCooldown() {
        return hubState().relayCooldown;
    }

    default void setRelayCooldown(int value) {
        hubState().relayCooldown = value;
    }

    default MovingAverage packetsPerCycleAvg() {
        return hubState().packetsPerCycleAvg;
    }

    // ----------------------------------------------------------------------- //

    default int queueBaseSize() {
        return Settings.get().switchDefaultMaxQueueSize;
    }

    default int queueSizePerUpgrade() {
        return Settings.get().switchQueueSizeUpgrade;
    }

    default int relayBaseDelay() {
        return Settings.get().switchDefaultRelayDelay;
    }

    default double relayDelayPerUpgrade() {
        return Settings.get().switchRelayDelayUpgrade;
    }

    default int relayBaseAmount() {
        return Settings.get().switchDefaultRelayAmount;
    }

    default int relayAmountPerUpgrade() {
        return Settings.get().switchRelayAmountUpgrade;
    }

    // ----------------------------------------------------------------------- //

    /** Client side only. */
    @Override
    default boolean canConnect(Direction side) {
        return side != null;
    }

    @Override
    default Node sidedNode(Direction side) {
        return side != null ? plugs()[side.ordinal()].node : null;
    }

    // ----------------------------------------------------------------------- //

    static void onUpdateEntity(Hub self) {
        final State state = self.hubState();
        if (state.relayCooldown > 0) {
            state.relayCooldown -= 1;
        } else {
            state.relayCooldown = -1;
            final ArrayDeque<Pair<Optional<Direction>, Packet>> queue = state.queue;
            if (!queue.isEmpty()) {
                synchronized (queue) {
                    final int packetsToRely = Math.min(queue.size(), state.relayAmount);
                    state.packetsPerCycleAvg.add(packetsToRely);
                    for (int i = 0; i < packetsToRely; i++) {
                        final Pair<Optional<Direction>, Packet> entry = queue.poll();
                        if (entry == null) break;
                        self.relayPacket(entry.getLeft(), entry.getRight());
                    }
                    if (!queue.isEmpty()) {
                        state.relayCooldown = state.relayDelay - 1;
                    }
                }
            } else if (self.getLevel().getGameTime() % state.relayDelay == 0) {
                state.packetsPerCycleAvg.add(0);
            }
        }
    }

    default boolean tryEnqueuePacket(Optional<Direction> sourceSide, Packet packet) {
        final State state = hubState();
        synchronized (state.queue) {
            if (packet.ttl() > 0 && state.queue.size() < state.maxQueueSize) {
                state.queue.add(Pair.of(sourceSide, packet.hop()));
                if (state.relayCooldown < 0) {
                    state.relayCooldown = state.relayDelay - 1;
                }
                return true;
            } else return false;
        }
    }

    default void relayPacket(Optional<Direction> sourceSide, Packet packet) {
        for (Direction side : Direction.values()) {
            if (sourceSide.isEmpty() || sourceSide.get() != side) {
                final Node node = sidedNode(side);
                if (node != null) {
                    node.sendToReachable("network.message", packet);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(Hub self, CompoundTag nbt) {
        final Plug[] plugs = self.plugs();
        final CompoundTag[] plugTags = ExtendedNBT.toTagArray(nbt.getList(PlugsTag, Tag.TAG_COMPOUND), CompoundTag.class);
        for (int index = 0; index < plugTags.length && index < plugs.length; index++) {
            plugs[index].node.loadData(plugTags[index]);
        }
        final ArrayDeque<Pair<Optional<Direction>, Packet>> queue = self.queue();
        synchronized (queue) {
            ExtendedNBT.<CompoundTag>foreach(nbt.getList(QueueTag, Tag.TAG_COMPOUND), tag -> {
                final Optional<Direction> side = ExtendedNBT.getDirection(tag, SideTag);
                final Packet packet = li.cil.oc.api.Network.newPacket(tag);
                queue.add(Pair.of(side, packet));
            });
        }
        if (nbt.contains(RelayCooldownTag)) {
            self.setRelayCooldown(nbt.getInt(RelayCooldownTag));
        }
    }

    static void onSaveForServer(Hub self, CompoundTag nbt) {
        final ArrayDeque<Pair<Optional<Direction>, Packet>> queue = self.queue();
        synchronized (queue) {
            // Side check for Waila (and other mods that may call this client side).
            if (self.isServer()) {
                final List<CompoundTag> plugTags = new ArrayList<>();
                for (Plug plug : self.plugs()) {
                    final CompoundTag plugNbt = new CompoundTag();
                    if (plug.node != null)
                        plug.node.saveData(plugNbt);
                    plugTags.add(plugNbt);
                }
                ExtendedNBT.setNewTagList(nbt, PlugsTag, plugTags);
                final List<CompoundTag> queueTags = new ArrayList<>();
                for (Pair<Optional<Direction>, Packet> entry : queue) {
                    final CompoundTag tag = new CompoundTag();
                    ExtendedNBT.setDirection(tag, SideTag, entry.getLeft());
                    entry.getRight().saveData(tag);
                    queueTags.add(tag);
                }
                ExtendedNBT.setNewTagList(nbt, QueueTag, queueTags);
                final int relayCooldown = self.relayCooldown();
                if (relayCooldown > 0) {
                    nbt.putInt(RelayCooldownTag, relayCooldown);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    default Plug createPlug(Direction side) {
        return new Plug(this, side);
    }

    class Plug implements li.cil.oc.api.network.Environment {
        public final Hub hub;
        public final Direction side;
        public final Node node;

        public Plug(Hub hub, Direction side) {
            this.hub = hub;
            this.side = side;
            this.node = hub.createNode(this);
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onMessage(Message message) {
            if (isPrimary()) {
                hub.onPlugMessage(this, message);
            }
        }

        @Override
        public void onConnect(Node node) {
            hub.onPlugConnect(this, node);
        }

        @Override
        public void onDisconnect(Node node) {
            hub.onPlugDisconnect(this, node);
        }

        public boolean isPrimary() {
            final Plug[] plugs = hub.plugs();
            for (Plug plug : plugs) {
                if (plug.node.network() == node.network()) return plug == this;
            }
            return false;
        }

        public List<Plug> plugsInOtherNetworks() {
            final List<Plug> result = new ArrayList<>();
            for (Plug plug : hub.plugs()) {
                if (plug.node.network() != node.network()) result.add(plug);
            }
            return result;
        }
    }

    default void onPlugConnect(Plug plug, Node node) {
    }

    default void onPlugDisconnect(Plug plug, Node node) {
    }

    default void onPlugMessage(Plug plug, Message message) {
        if ("network.message".equals(message.name())) {
            for (Plug other : plugs()) {
                if (other.node == message.source()) return;
            }
            final Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof Packet packet) {
                tryEnqueuePacket(Optional.ofNullable(plug.side), packet);
            }
        }
    }

    default Node createNode(Plug plug) {
        return li.cil.oc.api.Network.newNode(plug, Visibility.Network).create();
    }
}
