package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.HashMap;
import java.util.Map;

public class CPU extends AbstractManagedEnvironment implements DeviceInfo {
    public final int tier;

    public final Node node;

    public CPU(int tier) {
        this.tier = tier;
        this.node = Network.newNode(this, Visibility.Neighbors).
                create();
        setNode(node);
    }

    @Override
    public Node node() {
        return node;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Processor);
            info.put(DeviceAttribute.Description, "CPU");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "FlexiArch " + (tier + 1) + " Processor");
            info.put(DeviceAttribute.Clock, Integer.toString((int) (Settings.get().callBudgets[tier] * 1000)));
            deviceInfo = info;
        }
        return deviceInfo;
    }
}
