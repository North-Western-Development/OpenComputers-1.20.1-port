package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.common.tileentity.traits.ComponentInventory;
import li.cil.oc.common.tileentity.traits.Hub;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.PowerBalancer;
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.StateAware;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.integration.opencomputers.DriverRedstoneCard;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class Rack extends TileEntity implements PowerAcceptor, Hub, PowerBalancer, ComponentInventory, Rotatable, BundledRedstoneAware, Analyzable, li.cil.oc.api.internal.Rack, StateAware, MenuProvider {
    public boolean isRelayEnabled = false;
    public final CompoundTag[] lastData = new CompoundTag[getContainerSize()];
    public final boolean[] hasChanged = new boolean[getContainerSize()];

    // Map node connections for each installed mountable. Each mountable may
    // have up to four outgoing connections, with the first one always being
    // the "primary" connection, i.e. being a direct connection allowing
    // component access (i.e. actually connecting to that side of the rack).
    // The other nodes are "secondary" connections and merely transfer network
    // messages.
    // mountable -> connectable -> side
    @SuppressWarnings("unchecked")
    public final Optional<Direction>[][] nodeMapping = new Optional[getContainerSize()][4];
    public final Node[][] snifferNodes = new Node[getContainerSize()][3];

    private static final String IsRelayEnabledTag = Settings.namespace + "isRelayEnabled";
    private static final String NodeMappingTag = Settings.namespace + "nodeMapping";
    private static final String LastDataTag = Settings.namespace + "lastData";
    private static final String RackDataTag = Settings.namespace + "rackData";

    public Rack(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        java.util.Arrays.fill(hasChanged, true);
        for (Optional<Direction>[] mapping : nodeMapping) java.util.Arrays.fill(mapping, Optional.empty());
        // Scala: Array.fill(3)(newNode(...)) evaluates the expression per element.
        for (Node[] nodes : snifferNodes) {
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.Neighbors).create();
            }
        }
    }

    public void connect(int slot, int connectableIndex, Optional<Direction> side) {
        final Optional<Direction> newSide = side.isPresent() && side.get() != Direction.SOUTH ? side : Optional.empty();

        final Optional<Direction> oldSide = nodeMapping[slot][connectableIndex];
        if (oldSide.equals(newSide)) return;

        // Cut connection / remove sniffer node.
        final RackMountable mountable = getMountable(slot);
        if (mountable != null && oldSide.isPresent()) {
            if (connectableIndex == 0) {
                final Node node = mountable.node();
                final Node plug = sidedNode(toGlobal(oldSide.get()));
                if (node != null && plug != null) {
                    node.disconnect(plug);
                }
            } else {
                snifferNodes[slot][connectableIndex].remove();
            }
        }

        nodeMapping[slot][connectableIndex] = newSide;

        // Establish connection / add sniffer node.
        if (mountable != null && newSide.isPresent()) {
            if (connectableIndex == 0) {
                final Node node = mountable.node();
                final Node plug = sidedNode(toGlobal(newSide.get()));
                if (node != null && plug != null) {
                    node.connect(plug);
                }
            } else if (connectableIndex < mountable.getConnectableCount()) {
                final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
                if (connectable != null && connectable.node() != null) {
                    if (connectable.node().network() == null) {
                        li.cil.oc.api.Network.joinNewNetwork(connectable.node());
                    }
                    connectable.node().connect(snifferNodes[slot][connectableIndex]);
                }
            }
        }
    }

    private void reconnect(Direction plugSide) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final Optional<Direction>[] mapping = nodeMapping[slot];
            if (mapping[0].isPresent() && toGlobal(mapping[0].get()) == plugSide) {
                final RackMountable mountable = getMountable(slot);
                final Node busNode = sidedNode(plugSide);
                if (busNode != null && mountable != null && mountable.node() != null && busNode != mountable.node()) {
                    li.cil.oc.api.Network.joinNewNetwork(mountable.node());
                    busNode.connect(mountable.node());
                }
            } // else: Not connected to this side.
            for (int connectableIndex = 0; connectableIndex < 3; connectableIndex++) {
                if (mapping[connectableIndex].isPresent() && toGlobal(mapping[connectableIndex].get()) == plugSide) {
                    final RackMountable mountable = getMountable(slot);
                    if (mountable != null && connectableIndex < mountable.getConnectableCount()) {
                        final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
                        if (connectable != null && connectable.node() != null) {
                            if (connectable.node().network() == null) {
                                li.cil.oc.api.Network.joinNewNetwork(connectable.node());
                            }
                            connectable.node().connect(snifferNodes[slot][connectableIndex]);
                        }
                    }
                } // else: Not connected to this side.
            }
        }
    }

    public void sendPacketToMountables(Optional<Direction> sourceSide, Packet packet) {
        // When a message arrives on a bus, also send it to all secondary nodes
        // connected to it. Only deliver it to that very node, if it's not the
        // sender, to avoid loops.
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final Optional<Direction>[] mapping = nodeMapping[slot];
            for (int connectableIndex = 0; connectableIndex < 3; connectableIndex++) {
                final Optional<Direction> side = mapping[connectableIndex + 1];
                if (side.isPresent() && sourceSide.isPresent() && sourceSide.get() == toGlobal(side.get())) {
                    final RackMountable mountable = getMountable(slot);
                    if (mountable != null && connectableIndex < mountable.getConnectableCount()) {
                        final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
                        if (connectable != null) {
                            connectable.receivePacket(packet);
                        }
                    }
                } // else: Not connected to a bus.
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // Hub

    @Override
    public boolean tryEnqueuePacket(Optional<Direction> sourceSide, Packet packet) {
        sendPacketToMountables(sourceSide, packet);
        if (isRelayEnabled)
            return Hub.super.tryEnqueuePacket(sourceSide, packet);
        else
            return true;
    }

    @Override
    public void relayPacket(Optional<Direction> sourceSide, Packet packet) {
        if (isRelayEnabled)
            Hub.super.relayPacket(sourceSide, packet);
    }

    @Override
    public void onPlugConnect(Hub.Plug plug, Node node) {
        Hub.super.onPlugConnect(plug, node);
        connectComponents();
        reconnect(plug.side);
    }

    @Override
    public Node createNode(Hub.Plug plug) {
        return li.cil.oc.api.Network.newNode(plug, Visibility.Network)
            .withConnector(Settings.get().bufferDistributor)
            .create();
    }

    // ----------------------------------------------------------------------- //
    // Environment

    @Override
    public void dispose() {
        super.dispose();
        disconnectComponents();
    }

    @Override
    public void onMessage(Message message) {
        Hub.super.onMessage(message);
        if ("network.message".equals(message.name())) {
            final Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof Packet packet) {
                relayIfMessageFromConnectable(message, packet);
            }
        }
    }

    private void relayIfMessageFromConnectable(Message message, Packet packet) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final RackMountable mountable = getMountable(slot);
            if (mountable != null) {
                final Optional<Direction>[] mapping = nodeMapping[slot];
                for (int connectableIndex = 0; connectableIndex < 3; connectableIndex++) {
                    final Optional<Direction> side = mapping[connectableIndex + 1];
                    if (side.isPresent()) {
                        if (connectableIndex < mountable.getConnectableCount()) {
                            final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
                            if (connectable != null && connectable.node() == message.source()) {
                                sidedNode(toGlobal(side.get())).sendToReachable("network.message", packet);
                                relayToConnectablesOnSide(message, packet, side.get());
                                return;
                            }
                        }
                    } // else: Not connected to a bus.
                }
            }
        }
    }

    private void relayToConnectablesOnSide(Message message, Packet packet, Direction sourceSide) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final RackMountable mountable = getMountable(slot);
            if (mountable != null) {
                final Optional<Direction>[] mapping = nodeMapping[slot];
                for (int connectableIndex = 0; connectableIndex < 3; connectableIndex++) {
                    final Optional<Direction> side = mapping[connectableIndex + 1];
                    if (side.isPresent() && side.get() == sourceSide) {
                        if (connectableIndex < mountable.getConnectableCount()) {
                            final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
                            if (connectable != null && connectable.node() != message.source()) {
                                snifferNodes[slot][connectableIndex].sendToNeighbors("network.message", packet);
                            }
                        }
                    } // else: Not connected to a bus.
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // SidedEnvironment

    @Override
    public boolean canConnect(Direction side) {
        return side != facing();
    }

    @Override
    public Node sidedNode(Direction side) {
        return side != facing() ? Hub.super.sidedNode(side) : null;
    }

    // ----------------------------------------------------------------------- //
    // power.Common

    @Override
    public boolean hasConnector(Direction side) {
        return side != facing();
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return Optional.ofNullable(side != facing() ? (Connector) sidedNode(side) : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().serverRackRate;
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        final Optional<Integer> slot = slotAt(side, hitX, hitY, hitZ);
        if (slot.isPresent()) {
            final Optional<ManagedEnvironment>[] components = components();
            if (slot.get() < components.length && components[slot.get()].isPresent() && components[slot.get()].get() instanceof Analyzable analyzable) {
                return analyzable.onAnalyze(player, side, hitX, hitY, hitZ);
            }
            return null;
        }
        return new Node[]{sidedNode(side)};
    }

    // ----------------------------------------------------------------------- //
    // internal.Rack

    @Override
    public int indexOfMountable(RackMountable mountable) {
        final Optional<ManagedEnvironment>[] components = components();
        for (int i = 0; i < components.length; i++) {
            if (components[i].isPresent() && components[i].get() == mountable) return i;
        }
        return -1;
    }

    @Override
    public RackMountable getMountable(int slot) {
        final Optional<ManagedEnvironment>[] components = components();
        if (slot >= 0 && slot < components.length && components[slot].isPresent() && components[slot].get() instanceof RackMountable mountable) {
            return mountable;
        }
        return null;
    }

    @Override
    public CompoundTag getMountableData(int slot) {
        return lastData[slot];
    }

    @Override
    public void markChanged(int slot) {
        synchronized (hasChanged) {
            hasChanged[slot] = true;
        }
        setOutputEnabled(hasRedstoneCard());
    }

    // ----------------------------------------------------------------------- //
    // StateAware

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        final EnumSet<li.cil.oc.api.util.StateAware.State> result = EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
        for (Optional<ManagedEnvironment> component : components()) {
            if (component.isPresent() && component.get() instanceof RackMountable mountable) {
                result.addAll(mountable.getCurrentState());
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------- //
    // Rotatable

    @Override
    public void onRotationChanged() {
        Rotatable.super.onRotationChanged();
        checkRedstoneInputChanged();
    }

    // ----------------------------------------------------------------------- //
    // RedstoneAware

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        BundledRedstoneAware.super.onRedstoneInputChanged(args);
        for (Optional<ManagedEnvironment> component : components()) {
            if (component.isPresent() && component.get() instanceof RackMountable mountable && mountable.node() != null) {
                final RedstoneChangedEventArgs toLocalArgs = new RedstoneChangedEventArgs(toLocal(args.side), args.oldValue, args.newValue, args.color);
                mountable.node().sendToNeighbors("redstone.changed", toLocalArgs);
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // Container

    @Override
    public int getContainerSize() {
        return 4;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return driver != null && Slot.RackMountable.equals(driver.slot(stack));
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (isServer()) {
            setOutputEnabled(hasRedstoneCard());
            PacketSender.sendRackInventory(this);
        } else {
            final BlockState state = getLevel().getBlockState(getBlockPos());
            getLevel().sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    // ----------------------------------------------------------------------- //
    // MenuProvider

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Rack(ContainerTypes.RACK.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //
    // ComponentInventory

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) {
            for (int connectable = 0; connectable < 4; connectable++) {
                nodeMapping[slot][connectable] = Optional.empty();
            }
            lastData[slot] = null;
            hasChanged[slot] = true;
        }
        ComponentInventory.super.onItemAdded(slot, stack);
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        if (isServer()) {
            for (int connectable = 0; connectable < 4; connectable++) {
                nodeMapping[slot][connectable] = Optional.empty();
            }
            lastData[slot] = null;
        }
        ComponentInventory.super.onItemRemoved(slot, stack);
    }

    @Override
    public void connectItemNode(Node node) {
        // By default create a new network for mountables. They have to
        // be wired up manually (mapping is reset in onItemAdded).
        li.cil.oc.api.Network.joinNewNetwork(node);
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer() && isConnected()) {
            final List<Connector> connectors = new ArrayList<>();
            for (Direction side : Direction.values()) {
                if (sidedNode(side) instanceof Connector connector) connectors.add(connector);
            }
            final Optional<ManagedEnvironment>[] components = components();
            for (int slot = 0; slot < components.length; slot++) {
                if (components[slot].isPresent() && components[slot].get() instanceof RackMountable mountable) {
                    if (hasChanged[slot]) {
                        hasChanged[slot] = false;
                        lastData[slot] = mountable.getData();
                        PacketSender.sendRackMountableData(this, slot);
                        getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
                        // These are working state dependent, so recompute them.
                        setOutputEnabled(hasRedstoneCard());
                    }

                    // Power mountables without requiring them to be connected to the outside.
                    if (mountable.node() instanceof Connector connector) {
                        double remaining = Settings.get().serverRackRate;
                        for (Connector outside : connectors) {
                            if (remaining <= 0) break;
                            final double received = remaining + outside.changeBuffer(-remaining);
                            final double rejected = connector.changeBuffer(received);
                            outside.changeBuffer(rejected);
                            remaining -= received - rejected;
                        }
                    } // else: Nothing using energy.
                }
            }

            updateComponents();
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);

        isRelayEnabled = nbt.getBoolean(IsRelayEnabledTag);
        final IntArrayTag[] mappings = ExtendedNBT.toTagArray(nbt.getList(NodeMappingTag, Tag.TAG_INT_ARRAY), IntArrayTag.class);
        for (int slot = 0; slot < mappings.length && slot < nodeMapping.length; slot++) {
            final int[] buses = mappings[slot].getAsIntArray();
            for (int i = 0; i < buses.length && i < nodeMapping[slot].length; i++) {
                final int id = buses[i];
                nodeMapping[slot][i] = id < 0 || id == Direction.SOUTH.ordinal() ? Optional.empty() : Optional.of(Direction.from3DDataValue(id));
            }
        }

        // Kickstart initialization.
        redstoneAwareState().isOutputEnabled = hasRedstoneCard();
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);

        nbt.putBoolean(IsRelayEnabledTag, isRelayEnabled);
        final List<IntArrayTag> mappings = new ArrayList<>();
        for (Optional<Direction>[] buses : nodeMapping) {
            final int[] ids = new int[buses.length];
            for (int i = 0; i < buses.length; i++) ids[i] = buses[i].map(Direction::ordinal).orElse(-1);
            mappings.add(ExtendedNBT.toNbt(ids));
        }
        ExtendedNBT.setNewTagList(nbt, NodeMappingTag, mappings);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);

        final CompoundTag[] data = ExtendedNBT.toTagArray(nbt.getList(LastDataTag, Tag.TAG_COMPOUND), CompoundTag.class);
        System.arraycopy(data, 0, lastData, 0, Math.min(data.length, lastData.length));
        loadData(nbt.getCompound(RackDataTag));
        connectComponents();
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);

        final List<CompoundTag> data = new ArrayList<>();
        for (CompoundTag tag : lastData) data.add(tag == null ? new CompoundTag() : tag);
        ExtendedNBT.setNewTagList(nbt, LastDataTag, data);
        ExtendedNBT.setNewCompoundTag(nbt, RackDataTag, this::saveData);
    }

    // ----------------------------------------------------------------------- //

    public Optional<Integer> slotAt(Direction side, float hitX, float hitY, float hitZ) {
        if (side == facing()) {
            final int globalY = (int) (hitY * 16); // [0, 15]
            final int l = 2;
            final int h = 14;
            final int slot = ((15 - globalY) - l) * getContainerSize() / (h - l);
            return Optional.of(Math.max(0, Math.min(getContainerSize() - 1, slot)));
        } else return Optional.empty();
    }

    public boolean isWorking(RackMountable mountable) {
        return mountable.getCurrentState().contains(li.cil.oc.api.util.StateAware.State.IsWorking);
    }

    @SuppressWarnings("unchecked")
    public boolean hasRedstoneCard() {
        for (Optional<ManagedEnvironment> component : components()) {
            if (component.isPresent() && component.get() instanceof RackMountable mountable
                && mountable instanceof EnvironmentHost && mountable instanceof Container inventory && isWorking(mountable)) {
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    final ItemStack stack = inventory.getItem(slot);
                    if (DriverRedstoneCard.INSTANCE.worksWith(stack, (Class<? extends EnvironmentHost>) mountable.getClass())) return true;
                }
            }
        }
        return false;
    }
}
