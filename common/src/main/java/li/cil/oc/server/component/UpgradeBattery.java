package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.Map;

public class UpgradeBattery extends AbstractManagedEnvironment implements DeviceInfo {
    public final int tier;

    public UpgradeBattery(int tier) {
        this.tier = tier;
        setNode(Network.newNode(this, Visibility.Network).
            withConnector(Settings.get().bufferCapacitorUpgrades[tier]).
            create());
    }

    @Override
    public Connector node() {
        return (Connector) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Battery",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Unlimited Power (Almost Ed.)",
                DeviceAttribute.Capacity, String.valueOf(Settings.get().bufferCapacitorUpgrades[tier])
            );
        }
        return deviceInfo;
    }
}
