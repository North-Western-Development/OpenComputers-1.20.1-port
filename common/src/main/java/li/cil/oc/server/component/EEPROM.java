package li.cil.oc.server.component;

import com.google.common.hash.Hashing;
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
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

import static li.cil.oc.util.ResultWrapper.result;

public class EEPROM extends AbstractManagedEnvironment implements DeviceInfo {
    public final ComponentConnector node;

    public byte[] codeData = new byte[0];

    public byte[] volatileData = new byte[0];

    public boolean readonly = false;

    public String label = "EEPROM";

    public EEPROM() {
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Neighbors).
                withComponent("eeprom", Visibility.Neighbors).
                withConnector().
                create();
        setNode(node);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    public String checksum() {
        return Hashing.crc32().hashBytes(codeData).toString();
    }

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Memory);
            info.put(DeviceAttribute.Description, "EEPROM");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "FlashStick2k");
            info.put(DeviceAttribute.Capacity, Integer.toString(Settings.get().eepromSize));
            info.put(DeviceAttribute.Size, Integer.toString(Settings.get().eepromSize));
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] get(Context context, Arguments args) {
        return result(codeData);
    }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object[] set(Context context, Arguments args) {
        if (readonly) {
            return result(null, "storage is readonly");
        }
        if (!node.tryChangeBuffer(-Settings.get().eepromWriteCost)) {
            return result(null, "not enough energy");
        }
        final byte[] newData = args.optByteArray(0, new byte[0]);
        if (newData.length > Settings.get().eepromSize) throw new IllegalArgumentException("not enough space");
        codeData = newData;
        context.pause(2); // deliberately slow to discourage use as normal storage medium
        return null;
    }

    @Callback(direct = true, doc = "function():string -- Get the label of the EEPROM.")
    public Object[] getLabel(Context context, Arguments args) {
        return result(label);
    }

    @Callback(doc = "function(data:string):string -- Set the label of the EEPROM.")
    public Object[] setLabel(Context context, Arguments args) {
        if (readonly) {
            return result(null, "storage is readonly");
        }
        label = args.optString(0, "EEPROM").trim();
        if (label.length() > 24) label = label.substring(0, 24);
        if (label.length() == 0) label = "EEPROM";
        return result(label);
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getSize(Context context, Arguments args) {
        return result(Settings.get().eepromSize);
    }

    @Callback(direct = true, doc = "function():string -- Get the checksum of the data on this EEPROM.")
    public Object[] getChecksum(Context context, Arguments args) {
        return result(checksum());
    }

    @Callback(direct = true, doc = "function(checksum:string):boolean -- Make this EEPROM readonly if it isn't already. This process cannot be reversed!")
    public Object[] makeReadonly(Context context, Arguments args) {
        if (args.checkString(0).equals(checksum())) {
            readonly = true;
            return result(true);
        } else return result(null, "incorrect checksum");
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getDataSize(Context context, Arguments args) {
        return result(Settings.get().eepromDataSize);
    }

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] getData(Context context, Arguments args) {
        return result(volatileData);
    }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object[] setData(Context context, Arguments args) {
        if (!node.tryChangeBuffer(-Settings.get().eepromWriteCost)) {
            return result(null, "not enough energy");
        }
        final byte[] newData = args.optByteArray(0, new byte[0]);
        if (newData.length > Settings.get().eepromDataSize) throw new IllegalArgumentException("not enough space");
        volatileData = newData;
        context.pause(1); // deliberately slow to discourage use as normal storage medium
        return null;
    }

    // ----------------------------------------------------------------------- //

    private static final String EEPROMTag = Settings.namespace + "eeprom";
    private static final String LabelTag = Settings.namespace + "label";
    private static final String ReadonlyTag = Settings.namespace + "readonly";
    private static final String UserdataTag = Settings.namespace + "userdata";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        codeData = nbt.getByteArray(EEPROMTag);
        if (nbt.contains(LabelTag)) {
            label = nbt.getString(LabelTag);
        }
        readonly = nbt.getBoolean(ReadonlyTag);
        volatileData = nbt.getByteArray(UserdataTag);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        nbt.putByteArray(EEPROMTag, codeData);
        nbt.putString(LabelTag, label);
        nbt.putBoolean(ReadonlyTag, readonly);
        nbt.putByteArray(UserdataTag, volatileData);
    }
}
