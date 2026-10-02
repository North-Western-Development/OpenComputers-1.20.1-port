package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NetworkCard extends AbstractManagedEnvironment implements RackBusConnectable, DeviceInfo, li.cil.oc.server.component.traits.WakeMessageAware {
    public final EnvironmentHost host;

    protected final Visibility visibility;

    protected final Set<Integer> openPorts = new HashSet<>();

    // State of WakeMessageAware.
    private Optional<String> wakeMessage = Optional.empty();
    private boolean wakeMessageFuzzy = false;

    public NetworkCard(EnvironmentHost host) {
        this.host = host;
        this.visibility = host instanceof Rack ? Visibility.Neighbors : Visibility.Network;
        setNode(Network.newNode(this, visibility).
            withComponent("modem", Visibility.Neighbors).
            create());
    }

    @Override
    public Component node() {
        return (Component) super.node();
    }

    // wired network card is the 1st in the max ports list (before both wireless cards)
    protected int maxOpenPorts() {
        return Settings.get().maxOpenPorts[Tier.One];
    }

    @Override
    public Optional<String> wakeMessage() {
        return wakeMessage;
    }

    @Override
    public void setWakeMessage(Optional<String> value) {
        wakeMessage = value;
    }

    @Override
    public boolean wakeMessageFuzzy() {
        return wakeMessageFuzzy;
    }

    @Override
    public void setWakeMessageFuzzy(boolean value) {
        wakeMessageFuzzy = value;
    }

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Network,
                DeviceAttribute.Description, "Ethernet controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "42i520 (MPN-01)",
                DeviceAttribute.Version, "1.0",
                DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize),
                DeviceAttribute.Size, String.valueOf(maxOpenPorts()),
                DeviceAttribute.Width, String.valueOf(Settings.get().maxNetworkPacketParts)
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(port:number):boolean -- Opens the specified port. Returns true if the port was opened.")
    public Object[] open(Context context, Arguments args) throws java.io.IOException {
        final int port = checkPort(args.checkInteger(0));
        if (openPorts.contains(port)) return ResultWrapper.result(false);
        else if (openPorts.size() >= maxOpenPorts()) {
            throw new java.io.IOException("too many open ports");
        }
        else return ResultWrapper.result(openPorts.add(port));
    }

    @Callback(doc = "function([port:number]):boolean -- Closes the specified port (default: all ports). Returns true if ports were closed.")
    public Object[] close(Context context, Arguments args) {
        if (args.count() == 0) {
            final boolean closed = !openPorts.isEmpty();
            openPorts.clear();
            return ResultWrapper.result(closed);
        }
        else {
            final int port = checkPort(args.checkInteger(0));
            return ResultWrapper.result(openPorts.remove(port));
        }
    }

    @Callback(direct = true, doc = "function(port:number):boolean -- Whether the specified port is open.")
    public Object[] isOpen(Context context, Arguments args) {
        final int port = checkPort(args.checkInteger(0));
        return ResultWrapper.result(openPorts.contains(port));
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wireless networking capability.")
    public Object[] isWireless(Context context, Arguments args) {
        return ResultWrapper.result(false);
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wired networking capability.")
    public Object[] isWired(Context context, Arguments args) {
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(address:string, port:number, data...) -- Sends the specified data to the specified target.")
    public Object[] send(Context context, Arguments args) throws java.io.IOException {
        final String address = args.checkString(0);
        final int port = checkPort(args.checkInteger(1));
        final Packet packet = Network.newPacket(node().address(), address, port, drop(args, 2));
        doSend(packet);
        networkActivity();
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(port:number, data...) -- Broadcasts the specified data on the specified port.")
    public Object[] broadcast(Context context, Arguments args) throws java.io.IOException {
        final int port = checkPort(args.checkInteger(0));
        final Packet packet = Network.newPacket(node().address(), null, port, drop(args, 1));
        doBroadcast(packet);
        networkActivity();
        return ResultWrapper.result(true);
    }

    // Raw argument values (without the byte array to string conversion of Arguments.toArray).
    private static Object[] drop(Arguments args, int n) {
        final List<Object> result = new ArrayList<>();
        int i = 0;
        for (Object arg : args) {
            if (i++ >= n) result.add(arg);
        }
        return result.toArray();
    }

    protected void doSend(Packet packet) throws java.io.IOException {
        if (visibility == Visibility.Neighbors) node().sendToNeighbors("network.message", packet);
        else if (visibility == Visibility.Network) node().sendToReachable("network.message", packet);
        // else: Ignore.
    }

    protected void doBroadcast(Packet packet) throws java.io.IOException {
        if (visibility == Visibility.Neighbors) node().sendToNeighbors("network.message", packet);
        else if (visibility == Visibility.Network) node().sendToReachable("network.message", packet);
        // else: Ignore.
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            openPorts.clear();
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) && node().isNeighborOf(message.source()))
            openPorts.clear();
        if ("network.message".equals(message.name())) {
            final Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof Packet packet) {
                receivePacket(packet);
            }
        }
    }

    @Override
    public boolean isPacketAccepted(Packet packet, double distance) {
        if (li.cil.oc.server.component.traits.WakeMessageAware.super.isPacketAccepted(packet, distance)) {
            if (openPorts.contains(packet.port())) {
                networkActivity();
                return true;
            }
        }
        return false;
    }

    @Override
    public void receivePacket(Packet packet) {
        receivePacket(packet, 0, host);
    }

    // ----------------------------------------------------------------------- //

    private static final String OpenPortsTag = "openPorts";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        assert openPorts.isEmpty();
        for (int port : nbt.getIntArray(OpenPortsTag)) {
            openPorts.add(port);
        }
        loadWakeMessage(nbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);

        nbt.putIntArray(OpenPortsTag, openPorts.stream().mapToInt(Integer::intValue).toArray());
        saveWakeMessage(nbt);
    }

    // ----------------------------------------------------------------------- //

    protected int checkPort(int port) {
        if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number");
        else return port;
    }

    private void networkActivity() {
        if (host != null) {
            PacketSender.sendNetworkActivity(node(), host);
        }
    }
}
