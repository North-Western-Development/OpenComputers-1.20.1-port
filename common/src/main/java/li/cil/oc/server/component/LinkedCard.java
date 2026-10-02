package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.server.network.QuantumNetwork;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LinkedCard extends AbstractManagedEnvironment implements QuantumNetwork.QuantumNode, DeviceInfo, li.cil.oc.server.component.traits.WakeMessageAware {
    public String tunnel = "creative";

    // State of WakeMessageAware.
    private Optional<String> wakeMessage = Optional.empty();
    private boolean wakeMessageFuzzy = false;

    public LinkedCard() {
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("tunnel", Visibility.Neighbors).
            withConnector().
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    @Override
    public String tunnel() {
        return tunnel;
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
                DeviceAttribute.Description, "Quantumnet controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "HyperLink IV: Ender Edition",
                DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize),
                DeviceAttribute.Width, String.valueOf(Settings.get().maxNetworkPacketParts)
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(data...) -- Sends the specified data to the card this one is linked to.")
    public Object[] send(Context context, Arguments args) {
        final List<QuantumNetwork.QuantumNode> endpoints = new ArrayList<>();
        for (QuantumNetwork.QuantumNode endpoint : QuantumNetwork.getEndpoints(tunnel)) {
            if (endpoint != this) endpoints.add(endpoint);
        }
        // Iterate the arguments manually instead of using the Arguments' toArray (which converts byte arrays to Strings).
        final List<Object> data = new ArrayList<>();
        for (Object arg : args) data.add(arg);
        final Packet packet = Network.newPacket(node().address(), null, 0, data.toArray());
        if (node().tryChangeBuffer(-(packet.size() / 32.0 + Settings.get().wirelessCostPerRange[Tier.Two] * Settings.get().maxWirelessRange[Tier.Two] * 5))) {
            for (QuantumNetwork.QuantumNode endpoint : endpoints) {
                endpoint.receivePacket(packet);
            }
            return ResultWrapper.result(true);
        }
        else return ResultWrapper.result(ResultWrapper.unit, "not enough energy");
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum packet size (config setting).")
    public Object[] maxPacketSize(Context context, Arguments args) {
        return ResultWrapper.result(Settings.get().maxNetworkPacketSize);
    }

    @Override
    public void receivePacket(Packet packet) {
        receivePacket(packet, 0, null);
    }

    @Callback(direct = true, doc = "function():string -- Gets this link card's shared channel address")
    public Object[] getChannel(Context context, Arguments args) {
        return ResultWrapper.result(this.tunnel);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node()) {
            QuantumNetwork.add(this);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            QuantumNetwork.remove(this);
        }
    }

    // ----------------------------------------------------------------------- //

    private static final String TunnelTag = Settings.namespace + "tunnel";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        if (nbt.contains(TunnelTag)) {
            tunnel = nbt.getString(TunnelTag);
        }
        loadWakeMessage(nbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        nbt.putString(TunnelTag, tunnel);
        saveWakeMessage(nbt);
    }
}
