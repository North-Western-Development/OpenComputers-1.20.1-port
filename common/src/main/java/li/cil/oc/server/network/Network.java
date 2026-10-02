package li.cil.oc.server.network;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.server.machine.Callbacks;
import li.cil.oc.util.Color;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * Port notes: Scala had a private class {@code Network} (the node graph) and its
 * companion {@code object Network extends NetworkAPI}. The API object is this
 * class ({@link #INSTANCE}, assign it to {@code api.API.network}); the graph is
 * the package private nested class {@link Graph}. Node implementations created by
 * the builders are the nested {@code *Impl} classes.
 */
public final class Network implements NetworkAPI {
    public static final Network INSTANCE = new Network();

    private Network() {
    }

    /**
     * Throws any throwable without declaring it (used where Scala threw checked
     * exceptions from methods whose Java API signature does not declare them).
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    // Looking at this again after some time, the similarity to const in C++ is somewhat uncanny.
    static final class Graph implements Distributor {
        private final Map<String, Vertex> data;

        double globalBuffer = 0.0;

        double globalBufferSize = 0.0;

        private final List<Connector> connectors = new ArrayList<>();

        private Wrapper wrapper;

        private Graph(Map<String, Vertex> data) {
            this.data = data;
            for (Vertex node : data.values()) {
                if (node.data instanceof Connector connector) addConnector(connector);
                node.data.setNetwork(wrapper());
            }
        }

        Graph(Node node) {
            this(new LinkedHashMap<>());
            addNew(node);
            node.onConnect(node);
        }

        private Wrapper wrapper() {
            if (wrapper == null) wrapper = new Wrapper(this);
            return wrapper;
        }

        @Override
        public double globalBuffer() {
            return globalBuffer;
        }

        @Override
        public void setGlobalBuffer(double value) {
            globalBuffer = value;
        }

        @Override
        public double globalBufferSize() {
            return globalBufferSize;
        }

        @Override
        public void setGlobalBufferSize(double value) {
            globalBufferSize = value;
        }

        // Called by nodes when they want to change address from loading.
        void remap(Node remappedNode, String newAddress) {
            final Vertex node = data.get(remappedNode.address());
            if (node == null) throw new AssertionError("Node believes it belongs to a network it doesn't.");
            final List<Vertex> neighbors = new ArrayList<>();
            for (Edge edge : node.edges) neighbors.add(edge.other(node));
            node.data.remove();
            node.data.setAddress(newAddress);
            while (data.containsKey(node.data.address())) {
                node.data.setAddress(UUID.randomUUID().toString());
            }
            if (neighbors.isEmpty())
                addNew(node.data);
            else
                for (Vertex neighbor : neighbors) neighbor.data.connect(node.data);
        }

        // ------------------------------------------------------------------- //

        boolean connect(Node nodeA, Node nodeB) {
            if (nodeA == null) throw new NullPointerException("nodeA");
            if (nodeB == null) throw new NullPointerException("nodeB");

            if (nodeA == nodeB) throw new IllegalArgumentException(
                    "Cannot connect a node to itself.");

            final boolean containsA = contains(nodeA);
            final boolean containsB = contains(nodeB);

            if (!containsA && !containsB) throw new IllegalArgumentException(
                    "At least one of the nodes must already be in this network.");

            if (containsA && containsB) {
                final Vertex oldNodeA = node(nodeA);
                final Vertex oldNodeB = node(nodeB);
                // Both nodes already exist in the network but there is a new connection.
                // This can happen if a new node sequentially connects to multiple nodes
                // in an existing network, e.g. in a setup like so:
                // O O   Where O is an old node, and N is the new Node. It would connect
                // O N   to the node above and left to it (in no particular order).
                if (oldNodeA.edges.stream().noneMatch(e -> e.isBetween(oldNodeA, oldNodeB))) {
                    assert oldNodeB.edges.stream().noneMatch(e -> e.isBetween(oldNodeA, oldNodeB));
                    new Edge(oldNodeA, oldNodeB);
                    if (oldNodeA.data.reachability() == Visibility.Neighbors)
                        oldNodeB.data.onConnect(oldNodeA.data);
                    if (oldNodeB.data.reachability() == Visibility.Neighbors)
                        oldNodeA.data.onConnect(oldNodeB.data);
                    return true;
                } else return false; // That connection already exists.
            } else if (containsA) return add(node(nodeA), nodeB);
            else return add(node(nodeB), nodeA);
        }

        boolean disconnect(Node nodeA, Node nodeB) {
            if (nodeA == nodeB) throw new IllegalArgumentException(
                    "Cannot disconnect a node from itself.");

            final boolean containsA = contains(nodeA);
            final boolean containsB = contains(nodeB);

            if (!containsA || !containsB) throw new IllegalArgumentException(
                    "Both of the nodes must be in this network.");

            final Vertex oldNodeA = node(nodeA);
            final Vertex oldNodeB = node(nodeB);

            final Optional<Edge> found = oldNodeA.edges.stream().filter(e -> e.isBetween(oldNodeA, oldNodeB)).findFirst();
            if (found.isPresent()) {
                final Edge edge = found.get();
                handleSplit(edge.remove());
                if (edge.left.data.reachability() == Visibility.Neighbors)
                    edge.right.data.onDisconnect(edge.left.data);
                if (edge.right.data.reachability() == Visibility.Neighbors)
                    edge.left.data.onDisconnect(edge.right.data);
                return true;
            } else return false; // That connection doesn't exists.
        }

        boolean remove(Node node) {
            final Vertex entry = data.remove(node.address());
            if (entry == null) return false;
            if (node instanceof Connector connector) removeConnector(connector);
            node.setNetwork(null);
            final List<Map<String, Vertex>> subGraphs = entry.remove();
            final List<li.cil.oc.api.network.Node> targets = new ArrayList<>();
            targets.add(node);
            final Visibility reachability = entry.data.reachability();
            if (reachability == Visibility.Neighbors) {
                for (Edge edge : entry.edges) targets.add(edge.other(entry).data);
            } else if (reachability == Visibility.Network) {
                for (Map<String, Vertex> subGraph : subGraphs) {
                    for (Vertex vertex : subGraph.values()) targets.add(vertex.data);
                }
            }
            handleSplit(subGraphs);
            for (li.cil.oc.api.network.Node target : targets) ((Node) target).onDisconnect(node);
            return true;
        }

        // ------------------------------------------------------------------- //

        li.cil.oc.api.network.Node node(String address) {
            final Vertex node = data.get(address);
            return node != null ? node.data : null;
        }

        List<li.cil.oc.api.network.Node> nodes() {
            final List<li.cil.oc.api.network.Node> result = new ArrayList<>(data.size());
            for (Vertex vertex : data.values()) result.add(vertex.data);
            return result;
        }

        List<li.cil.oc.api.network.Node> reachableNodes(li.cil.oc.api.network.Node reference) {
            final Set<li.cil.oc.api.network.Node> referenceNeighbors = new HashSet<>(neighbors(reference));
            final List<li.cil.oc.api.network.Node> result = new ArrayList<>();
            for (li.cil.oc.api.network.Node node : nodes()) {
                if (node != reference && (node.reachability() == Visibility.Network ||
                        (node.reachability() == Visibility.Neighbors && referenceNeighbors.contains(node)))) {
                    result.add(node);
                }
            }
            return result;
        }

        List<li.cil.oc.api.network.Node> reachingNodes(li.cil.oc.api.network.Node reference) {
            if (reference.reachability() == Visibility.Network) {
                return nodes().stream().filter(node -> node != reference).collect(Collectors.toList());
            } else if (reference.reachability() == Visibility.Neighbors) {
                final Set<li.cil.oc.api.network.Node> referenceNeighbors = new HashSet<>(neighbors(reference));
                return nodes().stream().filter(node -> node != reference && referenceNeighbors.contains(node)).collect(Collectors.toList());
            } else return new ArrayList<>();
        }

        List<li.cil.oc.api.network.Node> neighbors(li.cil.oc.api.network.Node node) {
            final Vertex n = data.get(node.address());
            if (n != null && n.data == node) {
                final List<li.cil.oc.api.network.Node> result = new ArrayList<>(n.edges.size());
                for (Edge edge : n.edges) result.add(edge.other(n).data);
                return result;
            }
            throw new IllegalArgumentException("Node must be in this network.");
        }

        // ------------------------------------------------------------------- //

        void sendToAddress(li.cil.oc.api.network.Node source, String target, String name, Object... args) {
            if (source.network() != wrapper())
                throw new IllegalArgumentException("Source node must be in this network.");
            final Vertex node = data.get(target);
            if (node != null && node.data.canBeReachedFrom(source)) {
                send(source, Collections.singletonList(node.data), name, args);
            }
        }

        void sendToNeighbors(li.cil.oc.api.network.Node source, String name, Object... args) {
            if (source.network() != wrapper())
                throw new IllegalArgumentException("Source node must be in this network.");
            send(source, neighbors(source).stream().filter(n -> n.reachability() != Visibility.None).collect(Collectors.toList()), name, args);
        }

        void sendToReachable(li.cil.oc.api.network.Node source, String name, Object... args) {
            if (source.network() != wrapper())
                throw new IllegalArgumentException("Source node must be in this network.");
            send(source, reachableNodes(source), name, args);
        }

        void sendToVisible(li.cil.oc.api.network.Node source, String name, Object... args) {
            if (source.network() != wrapper())
                throw new IllegalArgumentException("Source node must be in this network.");
            final List<li.cil.oc.api.network.Node> targets = new ArrayList<>();
            for (li.cil.oc.api.network.Node node : reachableNodes(source)) {
                if (node instanceof li.cil.oc.api.network.Component component && component.canBeSeenFrom(source)) {
                    targets.add(component);
                }
            }
            send(source, targets, name, args);
        }

        // ------------------------------------------------------------------- //

        private boolean contains(Node node) {
            return node.network() == wrapper() && data.containsKey(node.address());
        }

        private Vertex node(li.cil.oc.api.network.Node node) {
            final Vertex vertex = data.get(node.address());
            if (vertex == null) throw new java.util.NoSuchElementException("key not found: " + node.address());
            return vertex;
        }

        private Vertex addNew(Node node) {
            final Vertex newNode = new Vertex(node);
            if (node.address() == null || data.containsKey(node.address()))
                node.setAddress(UUID.randomUUID().toString());
            data.put(node.address(), newNode);
            if (node instanceof Connector connector) addConnector(connector);
            node.setNetwork(wrapper());
            return newNode;
        }

        private boolean add(Vertex oldNode, Node addedNode) {
            // Queue onConnect calls to avoid side effects from callbacks.
            final List<Pair<li.cil.oc.api.network.Node, List<li.cil.oc.api.network.Node>>> connects = new ArrayList<>();
            // Check if the other node is new or if we have to merge networks.
            if (addedNode.network() == null) {
                final Vertex newNode = addNew(addedNode);
                new Edge(oldNode, newNode);
                final Visibility reachability = addedNode.reachability();
                if (reachability == Visibility.None) {
                    connects.add(Pair.of(addedNode, List.of(addedNode)));
                } else if (reachability == Visibility.Neighbors) {
                    final List<li.cil.oc.api.network.Node> targets = new ArrayList<>();
                    targets.add(addedNode);
                    targets.addAll(neighbors(addedNode));
                    connects.add(Pair.of(addedNode, targets));
                    for (li.cil.oc.api.network.Node node : reachingNodes(addedNode)) connects.add(Pair.of(node, List.of(addedNode)));
                } else if (reachability == Visibility.Network) {
                    // Explicitly send to the added node itself first.
                    final List<li.cil.oc.api.network.Node> targets = new ArrayList<>();
                    targets.add(addedNode);
                    for (li.cil.oc.api.network.Node node : nodes()) if (node != addedNode) targets.add(node);
                    connects.add(Pair.of(addedNode, targets));
                    for (li.cil.oc.api.network.Node node : reachingNodes(addedNode)) connects.add(Pair.of(node, List.of(addedNode)));
                }

                // added node may load more internal nodes
                addedNode.onConnect(addedNode);
                final List<li.cil.oc.api.network.Node> visibleNodes = nodes().stream().filter(n -> n.reachability() == Visibility.Network).collect(Collectors.toList());
                for (li.cil.oc.api.network.Node node : visibleNodes) connects.add(Pair.of(node, nodes()));
            } else {
                final Graph otherNetwork = ((Wrapper) addedNode.network()).network;

                // If the other network contains nodes with addresses used in our local
                // network we'll have to re-assign those... since dynamically handling
                // changes to one's address is not expected of nodes / hosts, we have to
                // remove and reconnect the nodes. This is a pretty shitty solution, and
                // may break things slightly here and there (e.g. if this is the node of
                // a running machine the computer will most likely crash), but it should
                // never happen in normal operation anyway. It *can* happen when NBT
                // editing stuff or using mods to clone blocks (e.g. WorldEdit).
                final List<Vertex> duplicates = new ArrayList<>();
                for (Map.Entry<String, Vertex> entry : otherNetwork.data.entrySet()) {
                    if (data.containsKey(entry.getKey())) duplicates.add(entry.getValue());
                }
                final Graph otherNetworkAfterReaddress;
                if (duplicates.isEmpty()) {
                    otherNetworkAfterReaddress = otherNetwork;
                } else {
                    for (Vertex vertex : duplicates) {
                        final Node node = vertex.data;
                        final List<Node> neighbors = new ArrayList<>();
                        for (Edge edge : vertex.edges) neighbors.add(edge.other(vertex).data);

                        String newAddress;
                        do {
                            newAddress = UUID.randomUUID().toString();
                        } while (data.containsKey(newAddress) || otherNetwork.data.containsKey(newAddress));

                        // This may lead to splits, which is the whole reason we have to
                        // check the network of the other nodes after the readdressing.
                        node.remove();
                        node.setAddress(newAddress);
                        INSTANCE.joinNewNetwork(node);

                        if (newAddress.equals(node.address())) {
                            for (Node neighbor : neighbors) {
                                if (neighbor.network() != null) neighbor.connect(node);
                            }
                        } else {
                            OpenComputers.log.error("I can't see this happening any other way than someone directly setting node addresses, which they shouldn't. So yeah. Shit'll be borked. Deal with it.");
                            node.remove(); // well screw you then
                        }
                    }

                    otherNetworkAfterReaddress = ((Wrapper) duplicates.get(0).data.network()).network;
                }

                // The address change can theoretically cause the node to be kicked from
                // its old network (via onConnect callbacks), so we make sure it's still
                // in the same network. If it isn't we start over.
                if (addedNode.network() != null && ((Wrapper) addedNode.network()).network == otherNetworkAfterReaddress) {
                    if (addedNode.reachability() == Visibility.Neighbors)
                        connects.add(Pair.of(addedNode, List.of(oldNode.data)));
                    if (oldNode.data.reachability() == Visibility.Neighbors)
                        connects.add(Pair.of(oldNode.data, List.of(addedNode)));

                    final List<li.cil.oc.api.network.Node> oldNodes = nodes();
                    final List<li.cil.oc.api.network.Node> newNodes = otherNetworkAfterReaddress.nodes();
                    final List<li.cil.oc.api.network.Node> oldVisibleNodes = oldNodes.stream().filter(n -> n.reachability() == Visibility.Network).collect(Collectors.toList());
                    final List<li.cil.oc.api.network.Node> newVisibleNodes = newNodes.stream().filter(n -> n.reachability() == Visibility.Network).collect(Collectors.toList());

                    for (li.cil.oc.api.network.Node node : newVisibleNodes) connects.add(Pair.of(node, oldNodes));
                    for (li.cil.oc.api.network.Node node : oldVisibleNodes) connects.add(Pair.of(node, newNodes));

                    data.putAll(otherNetworkAfterReaddress.data);
                    connectors.addAll(otherNetworkAfterReaddress.connectors);
                    globalBuffer += otherNetworkAfterReaddress.globalBuffer;
                    globalBufferSize += otherNetworkAfterReaddress.globalBufferSize;
                    for (Vertex node : otherNetworkAfterReaddress.data.values()) {
                        if (node.data instanceof Connector connector) connector.setDistributor(Optional.of(wrapper()));
                        node.data.setNetwork(wrapper());
                    }
                    otherNetworkAfterReaddress.data.clear();
                    otherNetworkAfterReaddress.connectors.clear();

                    new Edge(oldNode, node(addedNode));
                } else return add(oldNode, addedNode);
            }

            for (Pair<li.cil.oc.api.network.Node, List<li.cil.oc.api.network.Node>> entry : connects) {
                for (li.cil.oc.api.network.Node target : entry.getRight()) ((Node) target).onConnect(entry.getLeft());
            }

            return true;
        }

        private void handleSplit(List<Map<String, Vertex>> subGraphs) {
            if (subGraphs.size() > 1) {
                final List<List<Node>> nodes = new ArrayList<>();
                final List<List<Node>> visibleNodes = new ArrayList<>();
                for (Map<String, Vertex> subGraph : subGraphs) {
                    final List<Node> graphNodes = new ArrayList<>();
                    for (Vertex vertex : subGraph.values()) graphNodes.add(vertex.data);
                    nodes.add(graphNodes);
                    visibleNodes.add(graphNodes.stream().filter(n -> n.reachability() == Visibility.Network).collect(Collectors.toList()));
                }

                data.clear();
                connectors.clear();
                globalBuffer = 0;
                globalBufferSize = 0;
                data.putAll(subGraphs.get(0));
                for (Vertex node : data.values()) {
                    if (node.data instanceof Connector connector) addConnector(connector);
                }
                for (Map<String, Vertex> subGraph : subGraphs.subList(1, subGraphs.size())) {
                    new Graph(subGraph);
                }

                for (int indexA = 0; indexA < subGraphs.size(); indexA++) {
                    final List<Node> nodesA = nodes.get(indexA);
                    final List<Node> visibleNodesA = visibleNodes.get(indexA);
                    for (int indexB = indexA + 1; indexB < subGraphs.size(); indexB++) {
                        final List<Node> nodesB = nodes.get(indexB);
                        final List<Node> visibleNodesB = visibleNodes.get(indexB);
                        for (Node node : visibleNodesA) for (Node other : nodesB) other.onDisconnect(node);
                        for (Node node : visibleNodesB) for (Node other : nodesA) other.onDisconnect(node);
                    }
                }
            }
        }

        private void send(li.cil.oc.api.network.Node source, Iterable<li.cil.oc.api.network.Node> targets, String name, Object... args) {
            final Message message = new Message(source, name, args);
            for (li.cil.oc.api.network.Node target : targets) target.host().onMessage(message);
        }

        // ------------------------------------------------------------------- //

        @Override
        public void addConnector(Connector connector) {
            if (connector.localBufferSize() > 0) {
                assert !connectors.contains(connector);
                connectors.add(connector);
                globalBuffer += connector.localBuffer();
                globalBufferSize += connector.localBufferSize();
            }
            connector.setDistributor(Optional.of(wrapper()));
        }

        @Override
        public void removeConnector(Connector connector) {
            if (connector.localBufferSize() > 0) {
                assert connectors.contains(connector);
                connectors.remove(connector);
                globalBuffer -= connector.localBuffer();
                globalBufferSize -= connector.localBufferSize();
            }
        }

        @Override
        public double changeBuffer(double delta) {
            if (delta == 0) return 0;
            else if (Settings.get().ignorePower) {
                if (delta < 0) return 0;
                else /* if (delta > 0) */ return delta;
            } else synchronized (this) {
                final double oldBuffer = globalBuffer;
                globalBuffer = Math.min(Math.max(globalBuffer + delta, 0), globalBufferSize);
                if (globalBuffer == oldBuffer) {
                    return delta;
                }
                if (delta < 0) {
                    double remaining = -delta;
                    for (Connector connector : connectors) {
                        if (remaining <= 0) break;
                        if (connector.localBuffer() > 0) {
                            if (connector.localBuffer() < remaining) {
                                remaining -= connector.localBuffer();
                                connector.setLocalBuffer(0);
                            } else {
                                connector.setLocalBuffer(connector.localBuffer() - remaining);
                                remaining = 0;
                            }
                        }
                    }
                    return -remaining;
                } else /* if (delta > 0) */ {
                    double remaining = delta;
                    for (Connector connector : connectors) {
                        if (remaining <= 0) break;
                        if (connector.localBuffer() < connector.localBufferSize()) {
                            final double space = connector.localBufferSize() - connector.localBuffer();
                            if (space < remaining) {
                                remaining -= space;
                                connector.setLocalBuffer(connector.localBufferSize());
                            } else {
                                connector.setLocalBuffer(connector.localBuffer() + remaining);
                                remaining = 0;
                            }
                        }
                    }
                    return remaining;
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void joinOrCreateNetwork(BlockGetter world, BlockPos pos) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity != null && !tileEntity.isRemoved() && tileEntity.getLevel() != null && !tileEntity.getLevel().isClientSide) {
            for (Direction side : Direction.values()) {
                final BlockPos npos = tileEntity.getBlockPos().relative(side);
                if (tileEntity.getLevel().isLoaded(npos)) {
                    final Optional<li.cil.oc.api.network.Node> localNode = getNetworkNode(tileEntity, side);
                    final BlockEntity neighborTileEntity = tileEntity.getLevel().getBlockEntity(npos);
                    final Optional<li.cil.oc.api.network.Node> neighborNode = getNetworkNode(neighborTileEntity, side.getOpposite());
                    if (localNode.isPresent() && localNode.get() instanceof Node node) {
                        if (neighborNode.isPresent() && neighborNode.get() instanceof Node neighbor && neighbor != node && neighbor.network() != null) {
                            final boolean canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity);
                            final boolean canConnectIM = canConnectFromSideIM(tileEntity, side) && canConnectFromSideIM(neighborTileEntity, side.getOpposite());
                            if (canConnectColor && canConnectIM) neighbor.connect(node);
                            else node.disconnect(neighbor);
                        }
                        if (node.network() == null) {
                            joinNewNetwork(node);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void joinOrCreateNetwork(BlockEntity tileEntity) {
        if (tileEntity != null) {
            final Level world = tileEntity.getLevel();
            final BlockPos pos = tileEntity.getBlockPos();
            if (world != null && pos != null) {
                joinOrCreateNetwork(world, pos);
            }
        }
    }

    @Override
    public void joinNewNetwork(li.cil.oc.api.network.Node node) {
        if (node instanceof Node mutableNode && mutableNode.network() == null) {
            new Graph(mutableNode);
        }
    }

    public Optional<li.cil.oc.api.network.Node> getNetworkNode(BlockEntity tileEntity, Direction side) {
        // Port note: replaces the Forge environment capabilities with instanceof checks.
        if (tileEntity != null) {
            if (tileEntity instanceof SidedEnvironment host) {
                return Optional.ofNullable(host.sidedNode(side));
            }

            if (tileEntity instanceof Environment host) {
                return Optional.ofNullable(host.node());
            }
        }

        return Optional.empty();
    }

    private int getConnectionColor(BlockEntity tileEntity) {
        if (tileEntity instanceof li.cil.oc.api.internal.Colored colored && colored.controlsConnectivity()) {
            return colored.getColor();
        }

        return Color.rgbValues.get(DyeColor.LIGHT_GRAY);
    }

    private boolean canConnectBasedOnColor(BlockEntity te1, BlockEntity te2) {
        final int c1 = getConnectionColor(te1);
        final int c2 = getConnectionColor(te2);
        final int lightGray = Color.rgbValues.get(DyeColor.LIGHT_GRAY);
        return c1 == c2 || c1 == lightGray || c2 == lightGray;
    }

    private boolean canConnectFromSideIM(BlockEntity tileEntity, Direction side) {
        // TODO(port): integration - Immibis microblocks (traits.ImmibisMicroblock) side checks dropped.
        return true;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void joinWirelessNetwork(WirelessEndpoint endpoint) {
        WirelessNetwork.add(endpoint);
    }

    @Override
    public void updateWirelessNetwork(WirelessEndpoint endpoint) {
        WirelessNetwork.update(endpoint);
    }

    @Override
    public void leaveWirelessNetwork(WirelessEndpoint endpoint) {
        WirelessNetwork.remove(endpoint);
    }

    @Override
    public void leaveWirelessNetwork(WirelessEndpoint endpoint, ResourceKey<Level> dimension) {
        WirelessNetwork.remove(endpoint, dimension);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void sendWirelessPacket(WirelessEndpoint source, double strength, li.cil.oc.api.network.Packet packet) {
        for (WirelessEndpoint endpoint : WirelessNetwork.computeReachableFrom(source, strength)) {
            endpoint.receivePacket(packet, source);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Network.NodeBuilder newNode(Environment host, Visibility reachability) {
        return new NodeBuilder(host, reachability);
    }

    @Override
    public Packet newPacket(String source, String destination, int port, Object[] data) {
        final Packet packet = new Packet(source, destination, port, data);
        // We do the size check here instead of in the constructor of the packet
        // itself to avoid errors when loading packets.
        if (packet.size > Settings.get().maxNetworkPacketSize) {
            throw new IllegalArgumentException("packet too big (max " + Settings.get().maxNetworkPacketSize + ")");
        }
        return packet;
    }

    @Override
    public Packet newPacket(CompoundTag nbt) {
        final String source = nbt.getString("source");
        // Port note: inverted check kept from the original implementation.
        final String destination =
                nbt.contains("dest") ? null : nbt.getString("dest");
        final int port = nbt.getInt("port");
        final int ttl = nbt.getInt("ttl");
        final Object[] data = new Object[nbt.getInt("dataLength")];
        for (int i = 0; i < data.length; i++) {
            if (nbt.contains("data" + i)) {
                final Tag tag = nbt.get("data" + i);
                if (tag instanceof ByteTag t) data[i] = t.getAsByte() == 1;
                else if (tag instanceof IntTag t) data[i] = t.getAsInt();
                else if (tag instanceof DoubleTag t) data[i] = t.getAsDouble();
                else if (tag instanceof StringTag t) data[i] = t.getAsString();
                else if (tag instanceof ByteArrayTag t) data[i] = t.getAsByteArray();
                else throw new IllegalArgumentException("Unexpected tag type in network packet: " + tag);
            } else data[i] = null;
        }
        return new Packet(source, destination, port, data, ttl);
    }

    public BooleanSupplier isServer = SideTracker::isServer;

    public static class NodeBuilder implements Builder.NodeBuilder {
        public final Environment _host;
        public final Visibility _reachability;

        public NodeBuilder(Environment _host, Visibility _reachability) {
            this._host = _host;
            this._reachability = _reachability;
        }

        @Override
        public Network.ComponentBuilder withComponent(String name, Visibility visibility) {
            return new Network.ComponentBuilder(_host, _reachability, name, visibility);
        }

        @Override
        public Network.ComponentBuilder withComponent(String name) {
            return withComponent(name, _reachability);
        }

        @Override
        public Network.ConnectorBuilder withConnector(double bufferSize) {
            return new Network.ConnectorBuilder(_host, _reachability, bufferSize);
        }

        @Override
        public Network.ConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        public li.cil.oc.api.network.Node create() {
            if (INSTANCE.isServer.getAsBoolean()) return new NodeImpl(_host, _reachability);
            else return null;
        }
    }

    public static class ComponentBuilder implements Builder.ComponentBuilder {
        public final Environment _host;
        public final Visibility _reachability;
        public final String _name;
        public final Visibility _visibility;

        public ComponentBuilder(Environment _host, Visibility _reachability, String _name, Visibility _visibility) {
            this._host = _host;
            this._reachability = _reachability;
            this._name = _name;
            this._visibility = _visibility;
        }

        @Override
        public Network.ComponentConnectorBuilder withConnector(double bufferSize) {
            return new Network.ComponentConnectorBuilder(_host, _reachability, _name, _visibility, bufferSize);
        }

        @Override
        public Network.ComponentConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        public li.cil.oc.api.network.Component create() {
            if (INSTANCE.isServer.getAsBoolean()) {
                final ComponentImpl node = new ComponentImpl(_host, _reachability, _name);
                node.setVisibility(_visibility);
                return node;
            } else return null;
        }
    }

    public static class ConnectorBuilder implements Builder.ConnectorBuilder {
        public final Environment _host;
        public final Visibility _reachability;
        public final double _bufferSize;

        public ConnectorBuilder(Environment _host, Visibility _reachability, double _bufferSize) {
            this._host = _host;
            this._reachability = _reachability;
            this._bufferSize = _bufferSize;
        }

        @Override
        public Network.ComponentConnectorBuilder withComponent(String name, Visibility visibility) {
            return new Network.ComponentConnectorBuilder(_host, _reachability, name, visibility, _bufferSize);
        }

        @Override
        public Network.ComponentConnectorBuilder withComponent(String name) {
            return withComponent(name, _reachability);
        }

        @Override
        public li.cil.oc.api.network.Connector create() {
            if (INSTANCE.isServer.getAsBoolean()) {
                final ConnectorImpl node = new ConnectorImpl(_host, _reachability);
                node.setLocalBufferSizeRaw(_bufferSize);
                return node;
            } else return null;
        }
    }

    public static class ComponentConnectorBuilder implements Builder.ComponentConnectorBuilder {
        public final Environment _host;
        public final Visibility _reachability;
        public final String _name;
        public final Visibility _visibility;
        public final double _bufferSize;

        public ComponentConnectorBuilder(Environment _host, Visibility _reachability, String _name, Visibility _visibility, double _bufferSize) {
            this._host = _host;
            this._reachability = _reachability;
            this._name = _name;
            this._visibility = _visibility;
            this._bufferSize = _bufferSize;
        }

        @Override
        public li.cil.oc.api.network.ComponentConnector create() {
            if (INSTANCE.isServer.getAsBoolean()) {
                final ComponentConnectorImpl node = new ComponentConnectorImpl(_host, _reachability, _name);
                node.setLocalBufferSizeRaw(_bufferSize);
                node.setVisibility(_visibility);
                return node;
            } else return null;
        }
    }

    // ----------------------------------------------------------------------- //
    // Node implementations (Scala: anonymous `new X with NodeVarargPart`).

    static class NodeImpl implements Node {
        private final Environment host;
        private final Visibility reachability;
        private volatile String address = null;
        private volatile li.cil.oc.api.network.Network network = null;

        NodeImpl(Environment host, Visibility reachability) {
            this.host = host;
            this.reachability = reachability;
        }

        @Override
        public Environment host() {
            return host;
        }

        @Override
        public Visibility reachability() {
            return reachability;
        }

        @Override
        public String address() {
            return address;
        }

        @Override
        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public li.cil.oc.api.network.Network network() {
            return network;
        }

        @Override
        public void setNetwork(li.cil.oc.api.network.Network network) {
            this.network = network;
        }

        @Override
        public String toString() {
            return "Node(" + address + ", " + host + ")";
        }
    }

    static class ComponentImpl extends NodeImpl implements Component {
        private final String name;
        private Visibility visibility = Visibility.None;
        private Map<String, Callbacks.Callback> callbacks;
        private Map<String, Optional<Object>> hosts;

        ComponentImpl(Environment host, Visibility reachability, String name) {
            super(host, reachability);
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Visibility visibility() {
            return visibility;
        }

        @Override
        public void setVisibilityState(Visibility value) {
            visibility = value;
        }

        @Override
        public synchronized Map<String, Callbacks.Callback> callbacks() {
            if (callbacks == null) callbacks = Callbacks.apply(host());
            return callbacks;
        }

        @Override
        public synchronized Map<String, Optional<Object>> hosts() {
            if (hosts == null) hosts = Component.computeHosts(host(), callbacks());
            return hosts;
        }

        @Override
        public String toString() {
            return super.toString() + "@" + name;
        }
    }

    static class ConnectorImpl extends NodeImpl implements Connector {
        private double localBufferSize = 0.0;
        private double localBuffer = 0.0;
        private Optional<Distributor> distributor = Optional.empty();

        ConnectorImpl(Environment host, Visibility reachability) {
            super(host, reachability);
        }

        @Override
        public double localBuffer() {
            return localBuffer;
        }

        @Override
        public void setLocalBuffer(double value) {
            localBuffer = value;
        }

        @Override
        public double localBufferSize() {
            return localBufferSize;
        }

        @Override
        public void setLocalBufferSizeRaw(double value) {
            localBufferSize = value;
        }

        @Override
        public Optional<Distributor> distributor() {
            return distributor;
        }

        @Override
        public void setDistributor(Optional<Distributor> value) {
            distributor = value;
        }
    }

    static class ComponentConnectorImpl extends ComponentImpl implements ComponentConnector {
        private double localBufferSize = 0.0;
        private double localBuffer = 0.0;
        private Optional<Distributor> distributor = Optional.empty();

        ComponentConnectorImpl(Environment host, Visibility reachability, String name) {
            super(host, reachability, name);
        }

        @Override
        public double localBuffer() {
            return localBuffer;
        }

        @Override
        public void setLocalBuffer(double value) {
            localBuffer = value;
        }

        @Override
        public double localBufferSize() {
            return localBufferSize;
        }

        @Override
        public void setLocalBufferSizeRaw(double value) {
            localBufferSize = value;
        }

        @Override
        public Optional<Distributor> distributor() {
            return distributor;
        }

        @Override
        public void setDistributor(Optional<Distributor> value) {
            distributor = value;
        }
    }

    // ----------------------------------------------------------------------- //

    private static final class Vertex {
        final Node data;
        final List<Edge> edges = new ArrayList<>();

        Vertex(Node data) {
            this.data = data;
        }

        List<Map<String, Vertex>> remove() {
            for (Edge edge : edges) edge.other(this).edges.remove(edge);
            final List<Vertex> seeds = new ArrayList<>();
            for (Edge edge : edges) seeds.add(edge.other(this));
            return searchGraphs(seeds);
        }

        @Override
        public String toString() {
            return data + " [" + edges.size() + "]";
        }
    }

    private static final class Edge {
        final Vertex left;
        final Vertex right;

        Edge(Vertex left, Vertex right) {
            this.left = left;
            this.right = right;
            left.edges.add(this);
            right.edges.add(this);
        }

        Vertex other(Vertex side) {
            return side == left ? right : left;
        }

        boolean isBetween(Vertex a, Vertex b) {
            return (a == left && b == right) || (b == left && a == right);
        }

        List<Map<String, Vertex>> remove() {
            left.edges.remove(this);
            right.edges.remove(this);
            return searchGraphs(Arrays.asList(left, right));
        }

        // Scala case class equality.
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Edge other && other.left == left && other.right == right;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(left) * 31 + System.identityHashCode(right);
        }
    }

    private static List<Map<String, Vertex>> searchGraphs(List<Vertex> seeds) {
        final Set<Vertex> seen = new HashSet<>();
        final List<Map<String, Vertex>> result = new ArrayList<>();
        for (Vertex seed : seeds) {
            if (seen.contains(seed)) continue;
            final Map<String, Vertex> addressed = new LinkedHashMap<>();
            final ArrayDeque<Vertex> queue = new ArrayDeque<>();
            queue.add(seed);
            while (!queue.isEmpty()) {
                final Vertex node = queue.poll();
                seen.add(node);
                addressed.put(node.data.address(), node);
                for (Edge edge : node.edges) {
                    final Vertex n = edge.other(node);
                    if (!seen.contains(n) && !queue.contains(n)) queue.add(n);
                }
            }
            result.add(addressed);
        }
        return result;
    }

    // ----------------------------------------------------------------------- //

    private static final class Message implements li.cil.oc.api.network.Message {
        private final li.cil.oc.api.network.Node source;
        private final String name;
        private final Object[] data;
        public boolean isCanceled = false;

        Message(li.cil.oc.api.network.Node source, String name, Object[] data) {
            this.source = source;
            this.name = name;
            this.data = data;
        }

        @Override
        public li.cil.oc.api.network.Node source() {
            return source;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Object[] data() {
            return data;
        }

        @Override
        public void cancel() {
            isCanceled = true;
        }
    }

    // ----------------------------------------------------------------------- //

    public static class Packet implements li.cil.oc.api.network.Packet {
        public String source;
        public String destination;
        public int port;
        public Object[] data;
        public int ttl;
        public final int size;

        public Packet(String source, String destination, int port, Object[] data) {
            this(source, destination, port, data, 5);
        }

        public Packet(String source, String destination, int port, Object[] data, int ttl) {
            this.source = source;
            this.destination = destination;
            this.port = port;
            this.data = data;
            this.ttl = ttl;
            this.size = computeSize(data);
        }

        private static int computeSize(Object[] values) {
            if (values == null) return 0;
            if (values.length > Settings.get().maxNetworkPacketParts) {
                throw new IllegalArgumentException("packet has too many parts");
            }
            int acc = 0;
            for (Object arg : values) {
                if (arg == null || arg == ResultWrapper.unit) acc += 4;
                else if (arg instanceof Boolean) acc += 4;
                else if (arg instanceof Byte) acc += 4;
                else if (arg instanceof Short) acc += 4;
                else if (arg instanceof Integer) acc += 4;
                else if (arg instanceof Float) acc += 8;
                else if (arg instanceof Double) acc += 8;
                else if (arg instanceof String value) acc += Math.max(value.length(), 1);
                else if (arg instanceof byte[] value) acc += Math.max(value.length, 1);
                else
                    throw new IllegalArgumentException("unsupported data type: " + arg + " (" + arg.getClass().getCanonicalName() + ")");
            }
            return values.length * 2 + acc;
        }

        @Override
        public String source() {
            return source;
        }

        @Override
        public String destination() {
            return destination;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public Object[] data() {
            return data;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int ttl() {
            return ttl;
        }

        @Override
        public Packet hop() {
            return new Packet(source, destination, port, data, ttl - 1);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            nbt.putString("source", source);
            if (destination != null && !destination.isEmpty()) {
                nbt.putString("dest", destination);
            }
            nbt.putInt("port", port);
            nbt.putInt("ttl", ttl);
            nbt.putInt("dataLength", data.length);
            for (int i = 0; i < data.length; i++) {
                final Object value = data[i];
                if (value == null || value == ResultWrapper.unit) continue;
                if (value instanceof Boolean v) nbt.putBoolean("data" + i, v);
                else if (value instanceof Integer v) nbt.putInt("data" + i, v);
                else if (value instanceof Double v) nbt.putDouble("data" + i, v);
                else if (value instanceof String v) nbt.putString("data" + i, v);
                else if (value instanceof byte[] v) nbt.putByteArray("data" + i, v);
                else OpenComputers.log.warn("Unexpected type while saving network packet: " + value.getClass().getName());
            }
        }

        @Override
        public String toString() {
            return "{source = " + source + ", destination = " + destination + ", port = " + port + ", data = [" +
                    Arrays.stream(data).map(String::valueOf).collect(Collectors.joining(", ")) + "}]}";
        }
    }

    // ----------------------------------------------------------------------- //

    static class Wrapper implements li.cil.oc.api.network.Network, Distributor {
        final Graph network;

        Wrapper(Graph network) {
            this.network = network;
        }

        @Override
        public boolean connect(li.cil.oc.api.network.Node nodeA, li.cil.oc.api.network.Node nodeB) {
            return network.connect((Node) nodeA, (Node) nodeB);
        }

        @Override
        public boolean disconnect(li.cil.oc.api.network.Node nodeA, li.cil.oc.api.network.Node nodeB) {
            return network.disconnect((Node) nodeA, (Node) nodeB);
        }

        @Override
        public boolean remove(li.cil.oc.api.network.Node node) {
            return network.remove((Node) node);
        }

        @Override
        public li.cil.oc.api.network.Node node(String address) {
            return network.node(address);
        }

        @Override
        public Iterable<li.cil.oc.api.network.Node> nodes() {
            return network.nodes();
        }

        @Override
        public Iterable<li.cil.oc.api.network.Node> nodes(li.cil.oc.api.network.Node reference) {
            return network.reachableNodes(reference);
        }

        @Override
        public Iterable<li.cil.oc.api.network.Node> neighbors(li.cil.oc.api.network.Node node) {
            return network.neighbors(node);
        }

        @Override
        public void sendToAddress(li.cil.oc.api.network.Node source, String target, String name, Object... data) {
            network.sendToAddress(source, target, name, data);
        }

        @Override
        public void sendToNeighbors(li.cil.oc.api.network.Node source, String name, Object... data) {
            network.sendToNeighbors(source, name, data);
        }

        @Override
        public void sendToReachable(li.cil.oc.api.network.Node source, String name, Object... data) {
            network.sendToReachable(source, name, data);
        }

        @Override
        public void sendToVisible(li.cil.oc.api.network.Node source, String name, Object... data) {
            network.sendToVisible(source, name, data);
        }

        @Override
        public double globalBuffer() {
            return network.globalBuffer;
        }

        @Override
        public void setGlobalBuffer(double value) {
            network.globalBuffer = value;
        }

        @Override
        public double globalBufferSize() {
            return network.globalBufferSize;
        }

        @Override
        public void setGlobalBufferSize(double value) {
            network.globalBufferSize = value;
        }

        @Override
        public void addConnector(Connector connector) {
            network.addConnector(connector);
        }

        @Override
        public void removeConnector(Connector connector) {
            network.removeConnector(connector);
        }

        @Override
        public double changeBuffer(double delta) {
            return network.changeBuffer(delta);
        }
    }
}
