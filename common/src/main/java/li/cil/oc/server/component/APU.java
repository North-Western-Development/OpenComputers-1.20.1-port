package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;

import java.util.HashMap;
import java.util.Map;

public class APU extends GraphicsCard {
    private Map<String, String> deviceInfo;

    public APU(int tier) {
        super(tier);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Processor);
            info.put(DeviceAttribute.Description, "APU");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "FlexiArch " + (tier + 1) + " Processor (Builtin Graphics)");
            info.put(DeviceAttribute.Capacity, capacityInfo());
            info.put(DeviceAttribute.Width, widthInfo());
            info.put(DeviceAttribute.Clock, (int) (Settings.get().callBudgets[tier] * 1000) + "+" + clockInfo());
            deviceInfo = info;
        }
        return deviceInfo;
    }
}
