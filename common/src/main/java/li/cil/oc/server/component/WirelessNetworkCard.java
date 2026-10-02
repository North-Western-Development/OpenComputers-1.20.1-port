package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.Tier;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.Map;

public abstract class WirelessNetworkCard extends NetworkCard implements WirelessEndpoint {
    public double strength;

    public WirelessNetworkCard(EnvironmentHost host) {
        super(host);
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("modem", Visibility.Neighbors).
            withConnector().
            create());
        strength = maxWirelessRange();
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    protected abstract double wirelessCostPerRange();

    protected abstract double maxWirelessRange();

    protected abstract boolean shouldSendWiredTraffic();

    public BlockPosition position() {
        return BlockPosition.apply(host);
    }

    @Override
    public int x() {
        return position().x;
    }

    @Override
    public int y() {
        return position().y;
    }

    @Override
    public int z() {
        return position().z;
    }

    @Override
    public Level world() {
        return host.world();
    }

    @Override
    public void receivePacket(Packet packet, WirelessEndpoint source) {
        final double dx = (source.x() + 0.5) - host.xPosition();
        final double dy = (source.y() + 0.5) - host.yPosition();
        final double dz = (source.z() + 0.5) - host.zPosition();
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        receivePacket(packet, distance, host);
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when sending messages.")
    public Object[] getStrength(Context context, Arguments args) {
        return ResultWrapper.result(strength);
    }

    @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when sending messages.")
    public Object[] setStrength(Context context, Arguments args) {
        strength = Math.max(0, Math.min(args.checkDouble(0), maxWirelessRange()));
        return ResultWrapper.result(strength);
    }

    @Override
    public Object[] isWireless(Context context, Arguments args) {
        return ResultWrapper.result(true);
    }

    @Override
    public Object[] isWired(Context context, Arguments args) {
        return ResultWrapper.result(shouldSendWiredTraffic());
    }

    @Override
    protected void doSend(Packet packet) throws IOException {
        if (strength > 0) {
            checkPower();
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (shouldSendWiredTraffic())
            super.doSend(packet);
    }

    @Override
    protected void doBroadcast(Packet packet) throws IOException {
        if (strength > 0) {
            checkPower();
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (shouldSendWiredTraffic())
            super.doBroadcast(packet);
    }

    private void checkPower() throws IOException {
        final double cost = wirelessCostPerRange();
        if (cost > 0 && !Settings.get().ignorePower) {
            if (!node().tryChangeBuffer(-strength * cost)) {
                throw new IOException("not enough energy");
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (world().getGameTime() % 20 == 0) {
            Network.updateWirelessNetwork(this);
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node()) {
            Network.joinWirelessNetwork(this);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node() || !ExtendedWorld.isLoaded(world(), position())) {
            Network.leaveWirelessNetwork(this);
        }
    }

    // ----------------------------------------------------------------------- //

    private static final String StrengthTag = "strength";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        if (nbt.contains(StrengthTag)) {
            strength = Math.min(Math.max(nbt.getDouble(StrengthTag), 0), maxWirelessRange());
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        nbt.putDouble(StrengthTag, strength);
    }

    // ----------------------------------------------------------------------- //

    public static class Tier1 extends WirelessNetworkCard {
        public Tier1(EnvironmentHost host) {
            super(host);
        }

        @Override
        protected double wirelessCostPerRange() {
            return Settings.get().wirelessCostPerRange[Tier.One];
        }

        @Override
        protected double maxWirelessRange() {
            return Settings.get().maxWirelessRange[Tier.One];
        }

        // wired network card is before wireless cards in max port list
        @Override
        protected int maxOpenPorts() {
            return Settings.get().maxOpenPorts[Tier.One + 1];
        }

        @Override
        protected boolean shouldSendWiredTraffic() {
            return false;
        }

        // ----------------------------------------------------------------------- //

        private Map<String, String> deviceInfo;

        @Override
        public Map<String, String> getDeviceInfo() {
            if (deviceInfo == null) {
                deviceInfo = Map.of(
                    DeviceAttribute.Class, DeviceClass.Network,
                    DeviceAttribute.Description, "Wireless ethernet controller",
                    DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                    DeviceAttribute.Product, "39i110 (LPPW-01)",
                    DeviceAttribute.Version, "1.0",
                    DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize),
                    DeviceAttribute.Size, String.valueOf(maxOpenPorts()),
                    DeviceAttribute.Width, String.valueOf(maxWirelessRange())
                );
            }
            return deviceInfo;
        }

        @Override
        public boolean isPacketAccepted(Packet packet, double distance) {
            if (distance <= maxWirelessRange() && (distance > 0 || shouldSendWiredTraffic())) {
                return super.isPacketAccepted(packet, distance);
            }
            else {
                return false;
            }
        }
    }

    public static class Tier2 extends Tier1 {
        public Tier2(EnvironmentHost host) {
            super(host);
        }

        @Override
        protected double wirelessCostPerRange() {
            return Settings.get().wirelessCostPerRange[Tier.Two];
        }

        @Override
        protected double maxWirelessRange() {
            return Settings.get().maxWirelessRange[Tier.Two];
        }

        // wired network card is before wireless cards in max port list
        @Override
        protected int maxOpenPorts() {
            return Settings.get().maxOpenPorts[Tier.Two + 1];
        }

        @Override
        protected boolean shouldSendWiredTraffic() {
            return true;
        }

        // ----------------------------------------------------------------------- //

        private Map<String, String> deviceInfo;

        @Override
        public Map<String, String> getDeviceInfo() {
            if (deviceInfo == null) {
                deviceInfo = Map.of(
                    DeviceAttribute.Class, DeviceClass.Network,
                    DeviceAttribute.Description, "Wireless ethernet controller",
                    DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                    DeviceAttribute.Product, "62i230 (MPW-01)",
                    DeviceAttribute.Version, "2.0",
                    DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize),
                    DeviceAttribute.Size, String.valueOf(maxOpenPorts()),
                    DeviceAttribute.Width, String.valueOf(maxWirelessRange())
                );
            }
            return deviceInfo;
        }
    }
}
