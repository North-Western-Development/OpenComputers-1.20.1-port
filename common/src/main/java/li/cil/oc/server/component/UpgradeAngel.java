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

// Note-to-self: this has a component to allow the robot telling it has the
// upgrade.
public class UpgradeAngel extends AbstractManagedEnvironment implements DeviceInfo {
    public UpgradeAngel() {
        setNode(Network.newNode(this, Visibility.Network).
            create());
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Angel upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "FreePlacer (TM)",
                DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize)
            );
        }
        return deviceInfo;
    }
}
