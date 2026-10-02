package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.Map;

public class Memory extends AbstractManagedEnvironment implements DeviceInfo {
    public final int tier;

    public Memory(int tier) {
        this.tier = tier;
        setNode(Network.newNode(this, Visibility.Neighbors).
            create());
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Memory,
                DeviceAttribute.Description, "Memory bank",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Multipurpose RAM Type",
                DeviceAttribute.Clock, String.valueOf((int) (Settings.get().callBudgets[tier] * 1000))
            );
        }
        return deviceInfo;
    }
}
