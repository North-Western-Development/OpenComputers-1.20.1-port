package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.EventHandler;
import li.cil.oc.integration.util.WirelessRedstone;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * Scala trait mixed into classes with different superclasses → interface.
 * <p>
 * Implementing classes (always {@link RedstoneSignaller} subclasses) hold the state
 * behind the accessors and must explicitly chain the lifecycle hooks:
 * {@code onConnect → onWirelessConnect}, {@code onDisconnect → onWirelessDisconnect},
 * {@code loadData → loadWirelessData}, {@code saveData → saveWirelessData} (each after
 * the super call), and return {@link #wirelessDeviceInfo()} from {@code getDeviceInfo}.
 * <p>
 * The Scala trait's abstract {@code redstone: EnvironmentHost} member was dropped from
 * the interface (it clashes with {@link RedstoneVanilla#redstone()} and was never used
 * through this type); implementations still expose {@code redstone()}.
 */
public interface RedstoneWireless extends DeviceInfo {
    Node node();

    int wirelessFrequency();

    void setWirelessFrequency(int value);

    boolean wirelessInput();

    void setWirelessInput(boolean value);

    boolean wirelessOutput();

    void setWirelessOutput(boolean value);

    // ----------------------------------------------------------------------- //

    Map<String, String> WIRELESS_DEVICE_INFO = Map.of(
        DeviceAttribute.Class, DeviceClass.Communication,
        DeviceAttribute.Description, "Wireless redstone controller",
        DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
        DeviceAttribute.Product, "Rw400-M",
        DeviceAttribute.Capacity, "1",
        DeviceAttribute.Width, "1"
    );

    default Map<String, String> wirelessDeviceInfo() {
        return WIRELESS_DEVICE_INFO;
    }

    @Override
    default Map<String, String> getDeviceInfo() {
        return wirelessDeviceInfo();
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():number -- Get the wireless redstone input.")
    default Object[] getWirelessInput(Context context, Arguments args) {
        setWirelessInput(WirelessRedstone.getInput(this));
        return ResultWrapper.result(wirelessInput());
    }

    @Callback(direct = true, doc = "function():boolean -- Get the wireless redstone output.")
    default Object[] getWirelessOutput(Context context, Arguments args) {
        return ResultWrapper.result(wirelessOutput());
    }

    @Callback(doc = "function(value:boolean):boolean -- Set the wireless redstone output.")
    default Object[] setWirelessOutput(Context context, Arguments args) {
        final boolean oldValue = wirelessOutput();
        final boolean newValue = args.checkBoolean(0);

        if (oldValue != newValue) {
            setWirelessOutput(newValue);

            WirelessRedstone.updateOutput(this);

            if (Settings.get().redstoneDelay > 0)
                context.pause(Settings.get().redstoneDelay);
        }

        return ResultWrapper.result(oldValue);
    }

    @Callback(direct = true, doc = "function():number -- Get the currently set wireless redstone frequency.")
    default Object[] getWirelessFrequency(Context context, Arguments args) {
        return ResultWrapper.result(wirelessFrequency());
    }

    @Callback(doc = "function(frequency:number):number -- Set the wireless redstone frequency to use.")
    default Object[] setWirelessFrequency(Context context, Arguments args) {
        final int oldValue = wirelessFrequency();
        final int newValue = args.checkInteger(0);

        if (oldValue != newValue) {
            WirelessRedstone.removeReceiver(this);
            WirelessRedstone.removeTransmitter(this);

            setWirelessFrequency(newValue);
            setWirelessInput(false);
            setWirelessOutput(false);

            WirelessRedstone.addReceiver(this);

            context.pause(0.5);
        }

        return ResultWrapper.result(oldValue);
    }

    // ----------------------------------------------------------------------- //

    default void onWirelessConnect(Node node) {
        if (node == this.node()) {
            EventHandler.scheduleWirelessRedstone(this);
        }
    }

    default void onWirelessDisconnect(Node node) {
        if (node == this.node()) {
            WirelessRedstone.removeReceiver(this);
            WirelessRedstone.removeTransmitter(this);
            setWirelessOutput(false);
            setWirelessFrequency(0);
        }
    }

    // ----------------------------------------------------------------------- //

    String WirelessFrequencyTag = "wirelessFrequency";
    String WirelessInputTag = "wirelessInput";
    String WirelessOutputTag = "wirelessOutput";

    default void loadWirelessData(CompoundTag nbt) {
        setWirelessFrequency(nbt.getInt(WirelessFrequencyTag));
        setWirelessInput(nbt.getBoolean(WirelessInputTag));
        setWirelessOutput(nbt.getBoolean(WirelessOutputTag));
    }

    default void saveWirelessData(CompoundTag nbt) {
        nbt.putInt(WirelessFrequencyTag, wirelessFrequency());
        nbt.putBoolean(WirelessInputTag, wirelessInput());
        nbt.putBoolean(WirelessOutputTag, wirelessOutput());
    }
}
