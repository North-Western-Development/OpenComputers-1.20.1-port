package li.cil.oc.server.network;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.Objects;

/**
 * Mutable node. Port note: Scala's {@code final var address} / {@code final var network}
 * are the API accessors {@link #address()} / {@link #network()} plus the setters
 * {@link #setAddress(String)} / {@link #setNetwork(Network)}; the state lives in the
 * implementation classes in {@link li.cil.oc.server.network.Network}.
 * The former {@code NodeVarargPart} trait is folded into this interface.
 */
public interface Node extends li.cil.oc.api.network.Node {
    void setAddress(String address);

    void setNetwork(Network network);

    @Override
    default boolean canBeReachedFrom(li.cil.oc.api.network.Node other) {
        final Visibility reachability = reachability();
        if (reachability == Visibility.Neighbors) return isNeighborOf(other);
        if (reachability == Visibility.Network) return isInSameNetwork(other);
        return false;
    }

    @Override
    default boolean isNeighborOf(li.cil.oc.api.network.Node other) {
        if (!isInSameNetwork(other)) return false;
        for (li.cil.oc.api.network.Node neighbor : network().neighbors(this)) {
            if (Objects.equals(neighbor, other)) return true;
        }
        return false;
    }

    @Override
    default Iterable<li.cil.oc.api.network.Node> reachableNodes() {
        final Network network = network();
        if (network == null) return Collections.emptyList();
        return network.nodes(this);
    }

    @Override
    default Iterable<li.cil.oc.api.network.Node> neighbors() {
        final Network network = network();
        if (network == null) return Collections.emptyList();
        return network.neighbors(this);
    }

    // A node should be added to a network before it can connect to a node
    // but, sometimes other mods try to create nodes and connect them before
    // the network is ready. We don't desire those things to crash here.
    // With typical nodes we are talking about components here
    // which will be connected anyways when the network is created
    @Override
    default void connect(li.cil.oc.api.network.Node node) {
        final Network network = network();
        if (network != null) network.connect(this, node);
    }

    @Override
    default void disconnect(li.cil.oc.api.network.Node node) {
        final Network network = network();
        if (network != null && isInSameNetwork(node)) network.disconnect(this, node);
    }

    @Override
    default void remove() {
        final Network network = network();
        if (network != null) network.remove(this);
    }

    private boolean isInSameNetwork(li.cil.oc.api.network.Node other) {
        return network() != null && other != null && network() == other.network();
    }

    // ----------------------------------------------------------------------- //

    default void onConnect(li.cil.oc.api.network.Node node) {
        try {
            host().onConnect(node);
        } catch (Throwable e) {
            OpenComputers.log.warn("A component of type '" + host().getClass().getName() + "' threw an error while being connected to the component network.", e);
        }
    }

    default void onDisconnect(li.cil.oc.api.network.Node node) {
        try {
            host().onDisconnect(node);
        } catch (Throwable e) {
            OpenComputers.log.warn("A component of type '" + host().getClass().getName() + "' threw an error while being disconnected from the component network.", e);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    default void loadData(CompoundTag nbt) {
        if (nbt.contains("address")) {
            final String newAddress = nbt.getString("address");
            if (!Strings.isNullOrEmpty(newAddress) && !newAddress.equals(address())) {
                if (network() instanceof li.cil.oc.server.network.Network.Wrapper wrapper) {
                    wrapper.network.remap(this, newAddress);
                } else {
                    setAddress(newAddress);
                }
            }
        }
    }

    @Override
    default void saveData(CompoundTag nbt) {
        if (address() != null) {
            nbt.putString("address", address());
        }
    }

    // ----------------------------------------------------------------------- //
    // Former NodeVarargPart.

    @Override
    default void sendToAddress(String target, String name, Object... data) {
        final Network network = network();
        if (network != null) network.sendToAddress(this, target, name, data);
    }

    @Override
    default void sendToNeighbors(String name, Object... data) {
        final Network network = network();
        if (network != null) network.sendToNeighbors(this, name, data);
    }

    @Override
    default void sendToReachable(String name, Object... data) {
        final Network network = network();
        if (network != null) network.sendToReachable(this, name, data);
    }

    @Override
    default void sendToVisible(String name, Object... data) {
        final Network network = network();
        if (network != null) network.sendToVisible(this, name, data);
    }
}
