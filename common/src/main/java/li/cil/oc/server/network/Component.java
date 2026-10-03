package li.cil.oc.server.network;

import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.item.data.NodeData;
import li.cil.oc.server.driver.CompoundBlockEnvironment;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.server.machine.Callbacks;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.util.SideTracker;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable component node. Port note: the Scala private state ({@code _visibility},
 * the lazy {@code callbacks} / {@code hosts}) is held by the implementation classes
 * in {@link Network}; this interface accesses it via {@link #setVisibilityState},
 * {@link #callbacks()} and {@link #hosts()}.
 */
public interface Component extends li.cil.oc.api.network.Component, Node {
    /**
     * Raw setter for the current visibility (Scala {@code _visibility = value}).
     */
    void setVisibilityState(Visibility value);

    /**
     * Lazily computed {@code Callbacks(host)}.
     */
    Map<String, Callbacks.Callback> callbacks();

    /**
     * Lazily computed via {@link #computeHosts(Environment, Map)}.
     */
    Map<String, Optional<Object>> hosts();

    static Map<String, Optional<Object>> computeHosts(Environment host, Map<String, Callbacks.Callback> callbacks) {
        final Map<String, Optional<Object>> result = new HashMap<>();
        if (host instanceof CompoundBlockEnvironment multi) {
            for (Map.Entry<String, Callbacks.Callback> entry : callbacks.entrySet()) {
                final String method = entry.getKey();
                final Callbacks.Callback callback = entry.getValue();
                Optional<Object> found = Optional.empty();
                if (callback instanceof Callbacks.ComponentCallback component) {
                    for (Pair<String, ManagedEnvironment> pair : multi.environments) {
                        final ManagedEnvironment environment = pair.getRight();
                        if (environment.getClass() == component.method.getDeclaringClass()) {
                            found = Optional.of(environment);
                            break;
                        }
                    }
                    if (found.isEmpty()) {
                        // Callbacks declared in superclasses or as interface default methods (Scala
                        // traits used to compile to forwarders in the class itself).
                        for (Pair<String, ManagedEnvironment> pair : multi.environments) {
                            if (component.method.getDeclaringClass().isInstance(pair.getRight())) {
                                found = Optional.of(pair.getRight());
                                break;
                            }
                        }
                    }
                } else if (callback instanceof Callbacks.PeripheralCallback peripheral) {
                    for (Pair<String, ManagedEnvironment> pair : multi.environments) {
                        final ManagedEnvironment environment = pair.getRight();
                        if (environment instanceof ManagedPeripheral managedPeripheral &&
                                Arrays.asList(managedPeripheral.methods()).contains(peripheral.annotation.value())) {
                            found = Optional.of(environment);
                            break;
                        }
                    }
                }
                result.put(method, found);
            }
        } else {
            for (String method : callbacks.keySet()) {
                result.put(method, Optional.of(host));
            }
        }
        return result;
    }

    @Override
    default void setVisibility(Visibility value) {
        if (value.ordinal() > reachability().ordinal()) {
            throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + name() +
                    "' node with reachability '" + reachability() + "'. It will be limited to the node's reachability.");
        }
        if (SideTracker.isServer()) {
            if (network() != null) {
                final Visibility current = visibility();
                if (current == Visibility.Neighbors) {
                    if (value == Visibility.Network) addTo(reachableNodes());
                    else if (value == Visibility.None) removeFrom(neighbors());
                } else if (current == Visibility.Network) {
                    if (value == Visibility.Neighbors) {
                        final Set<li.cil.oc.api.network.Node> neighborSet = new HashSet<>();
                        neighbors().forEach(neighborSet::add);
                        final java.util.List<li.cil.oc.api.network.Node> filtered = new java.util.ArrayList<>();
                        for (li.cil.oc.api.network.Node node : reachableNodes()) {
                            if (!neighborSet.contains(node)) filtered.add(node);
                        }
                        removeFrom(filtered);
                    } else if (value == Visibility.None) removeFrom(reachableNodes());
                } else if (current == Visibility.None) {
                    if (value == Visibility.Neighbors) addTo(neighbors());
                    else if (value == Visibility.Network) addTo(reachableNodes());
                }
            }
            setVisibilityState(value);
        }
    }

    @Override
    default boolean canBeSeenFrom(li.cil.oc.api.network.Node other) {
        final Visibility visibility = visibility();
        if (visibility == Visibility.Network) return canBeReachedFrom(other);
        if (visibility == Visibility.Neighbors) return isNeighborOf(other);
        return false;
    }

    private void addTo(Iterable<li.cil.oc.api.network.Node> nodes) {
        for (li.cil.oc.api.network.Node node : nodes) {
            if (node.host() instanceof Machine machine) machine.addComponent(this);
        }
    }

    private void removeFrom(Iterable<li.cil.oc.api.network.Node> nodes) {
        for (li.cil.oc.api.network.Node node : nodes) {
            if (node.host() instanceof Machine machine) machine.removeComponent(this);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    default Collection<String> methods() {
        return callbacks().keySet();
    }

    @Override
    default li.cil.oc.api.machine.Callback annotation(String method) {
        final Callbacks.Callback callback = callbacks().get(method);
        if (callback == null) {
            // Scala threw a (checked) NoSuchMethodException here.
            throw li.cil.oc.server.network.Network.sneakyThrow(new NoSuchMethodException());
        }
        return callback.annotation;
    }

    @Override
    default Object[] invoke(String method, Context context, Object... arguments) throws Exception {
        final Callbacks.Callback callback = callbacks().get(method);
        if (callback == null) throw new NoSuchMethodException();
        final Optional<Object> environment = hosts().get(method);
        if (environment == null || environment.isEmpty()) throw new NoSuchMethodException();
        return Registry.INSTANCE.convert(callback.apply(environment.get(), context, new ArgumentsImpl(arguments)));
    }

    // ----------------------------------------------------------------------- //

    @Override
    default void loadData(CompoundTag nbt) {
        Node.super.loadData(nbt);
        if (nbt.contains(NodeData.VisibilityTag)) {
            setVisibilityState(Visibility.values()[nbt.getInt(NodeData.VisibilityTag)]);
        }
    }

    @Override
    default void saveData(CompoundTag nbt) {
        Node.super.saveData(nbt);
        nbt.putInt(NodeData.VisibilityTag, visibility().ordinal());
    }
}
