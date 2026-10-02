package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

public final class Redstone {
    private Redstone() {
    }

    public static class Vanilla extends RedstoneVanilla {
        public final RedstoneAware redstone;

        public <T extends RedstoneAware & EnvironmentHost> Vanilla(T redstone) {
            this.redstone = redstone;
        }

        @Override
        public RedstoneAware redstone() {
            return redstone;
        }
    }

    public static class Bundled extends RedstoneBundled {
        public final BundledRedstoneAware redstone;

        public <T extends BundledRedstoneAware & EnvironmentHost> Bundled(T redstone) {
            this.redstone = redstone;
        }

        @Override
        public BundledRedstoneAware redstone() {
            return redstone;
        }
    }

    public static class Wireless extends RedstoneSignaller implements RedstoneWireless {
        public final EnvironmentHost redstone;

        public int wirelessFrequency = 0;
        public boolean wirelessInput = false;
        public boolean wirelessOutput = false;

        public Wireless(EnvironmentHost redstone) {
            this.redstone = redstone;
        }

        public EnvironmentHost redstone() {
            return redstone;
        }

        // RedstoneWireless state.
        @Override public int wirelessFrequency() { return wirelessFrequency; }
        @Override public void setWirelessFrequency(int value) { wirelessFrequency = value; }
        @Override public boolean wirelessInput() { return wirelessInput; }
        @Override public void setWirelessInput(boolean value) { wirelessInput = value; }
        @Override public boolean wirelessOutput() { return wirelessOutput; }
        @Override public void setWirelessOutput(boolean value) { wirelessOutput = value; }

        @Override
        public void onConnect(Node node) {
            super.onConnect(node);
            onWirelessConnect(node);
        }

        @Override
        public void onDisconnect(Node node) {
            super.onDisconnect(node);
            onWirelessDisconnect(node);
        }

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            loadWirelessData(nbt);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            saveWirelessData(nbt);
        }
    }

    public static class VanillaWireless extends RedstoneVanilla implements RedstoneWireless {
        public final RedstoneAware redstone;

        public int wirelessFrequency = 0;
        public boolean wirelessInput = false;
        public boolean wirelessOutput = false;

        public <T extends RedstoneAware & EnvironmentHost> VanillaWireless(T redstone) {
            this.redstone = redstone;
        }

        @Override
        public RedstoneAware redstone() {
            return redstone;
        }

        // Linearization: RedstoneWireless comes last, so its device info wins.
        @Override
        public Map<String, String> getDeviceInfo() {
            return wirelessDeviceInfo();
        }

        // RedstoneWireless state.
        @Override public int wirelessFrequency() { return wirelessFrequency; }
        @Override public void setWirelessFrequency(int value) { wirelessFrequency = value; }
        @Override public boolean wirelessInput() { return wirelessInput; }
        @Override public void setWirelessInput(boolean value) { wirelessInput = value; }
        @Override public boolean wirelessOutput() { return wirelessOutput; }
        @Override public void setWirelessOutput(boolean value) { wirelessOutput = value; }

        @Override
        public void onConnect(Node node) {
            super.onConnect(node);
            onWirelessConnect(node);
        }

        @Override
        public void onDisconnect(Node node) {
            super.onDisconnect(node);
            onWirelessDisconnect(node);
        }

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            loadWirelessData(nbt);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            saveWirelessData(nbt);
        }
    }

    public static class BundledWireless extends RedstoneBundled implements RedstoneWireless {
        public final BundledRedstoneAware redstone;

        public int wirelessFrequency = 0;
        public boolean wirelessInput = false;
        public boolean wirelessOutput = false;

        public <T extends BundledRedstoneAware & EnvironmentHost> BundledWireless(T redstone) {
            this.redstone = redstone;
        }

        @Override
        public BundledRedstoneAware redstone() {
            return redstone;
        }

        private Map<String, String> deviceInfo;

        @Override
        public Map<String, String> getDeviceInfo() {
            if (deviceInfo == null) {
                deviceInfo = Map.of(
                    DeviceAttribute.Class, DeviceClass.Communication,
                    DeviceAttribute.Description, "Combined redstone controller",
                    DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                    DeviceAttribute.Product, "Rx900-M",
                    DeviceAttribute.Capacity, "65536",
                    DeviceAttribute.Width, "16"
                );
            }
            return deviceInfo;
        }

        // RedstoneWireless state.
        @Override public int wirelessFrequency() { return wirelessFrequency; }
        @Override public void setWirelessFrequency(int value) { wirelessFrequency = value; }
        @Override public boolean wirelessInput() { return wirelessInput; }
        @Override public void setWirelessInput(boolean value) { wirelessInput = value; }
        @Override public boolean wirelessOutput() { return wirelessOutput; }
        @Override public void setWirelessOutput(boolean value) { wirelessOutput = value; }

        @Override
        public void onConnect(Node node) {
            super.onConnect(node);
            onWirelessConnect(node);
        }

        @Override
        public void onDisconnect(Node node) {
            super.onDisconnect(node);
            onWirelessDisconnect(node);
        }

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            loadWirelessData(nbt);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            saveWirelessData(nbt);
        }
    }
}
