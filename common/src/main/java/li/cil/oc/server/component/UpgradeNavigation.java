package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.NavigationUpgradeData;
import li.cil.oc.common.tileentity.Waypoint;
import li.cil.oc.server.network.Waypoints;
import li.cil.oc.util.BlockPosition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static li.cil.oc.util.ResultWrapper.result;

public class UpgradeNavigation extends AbstractManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public final NavigationUpgradeData data = new NavigationUpgradeData();

    public <T extends EnvironmentHost & Rotatable> UpgradeNavigation(T host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("navigation", Visibility.Neighbors).
            withConnector().
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    private Rotatable rotatable() {
        return (Rotatable) host;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            String capacity;
            try {
                capacity = String.valueOf(data.getSize(host.world()));
            } catch (Exception e) {
                capacity = "0";
            }
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Navigation upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "PathFinder v3",
                DeviceAttribute.Capacity, capacity
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():number, number, number -- Get the current relative position of the robot.")
    public Object[] getPosition(Context context, Arguments args) throws Exception {
        final MapItemSavedData info = data.mapData(host.world());
        final int size = data.getSize(host.world());
        final double relativeX = host.xPosition() - info.centerX;
        final double relativeZ = host.zPosition() - info.centerZ;

        if (Math.abs(relativeX) <= size / 2.0 && Math.abs(relativeZ) <= size / 2.0)
            return result(relativeX, host.yPosition(), relativeZ);
        else
            return result(null, "out of range");
    }

    @Callback(doc = "function():number -- Get the current orientation of the robot.")
    public Object[] getFacing(Context context, Arguments args) {
        return result(rotatable().facing().ordinal());
    }

    @Callback(doc = "function():number -- Get the operational range of the navigation upgrade.")
    public Object[] getRange(Context context, Arguments args) throws Exception {
        return result(data.getSize(host.world()) / 2);
    }

    @Callback(doc = "function(range:number):table -- Find waypoints in the specified range.")
    public Object[] findWaypoints(Context context, Arguments args) {
        final double range = Math.min(Math.max(args.checkDouble(0), 0), Settings.get().maxWirelessRange[Tier.Two]);
        if (range <= 0) return result((Object) new Object[0]);
        if (!node().tryChangeBuffer(-range * Settings.get().wirelessCostPerRange[Tier.Two] * 0.25))
            return result(null, "not enough energy");
        context.pause(0.5);
        final BlockPosition position = BlockPosition.apply(host);
        final Vec3 positionVec = position.toVec3();
        final double rangeSq = range * range;
        final List<Map<String, Object>> found = new ArrayList<>();
        for (Waypoint waypoint : Waypoints.findWaypoints(position, range)) {
            final BlockPosition waypointPos = BlockPosition.apply(waypoint.getBlockPos(), waypoint.getLevel());
            if (positionVec.distanceToSqr(waypointPos.x + 0.5, waypointPos.y + 0.5, waypointPos.z + 0.5) > rangeSq) {
                continue;
            }
            final Vec3 delta = waypointPos.offset(waypoint.facing()).toVec3().subtract(positionVec);
            final Map<String, Object> entry = new HashMap<>();
            entry.put("position", new double[]{delta.x, delta.y, delta.z});
            entry.put("redstone", waypoint.maxInput());
            entry.put("label", waypoint.label);
            entry.put("address", waypoint.node().address());
            found.add(entry);
        }
        return result((Object) found.toArray());
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof li.cil.oc.api.machine.Machine machine) {
            final Object[] payload = message.data();
            if (machine.host() instanceof Tablet && payload.length == 8 &&
                payload[0] instanceof CompoundTag nbt && payload[3] instanceof BlockPosition blockPos) {
                try {
                    final MapItemSavedData info = data.mapData(host.world());
                    nbt.putInt("posX", blockPos.x - info.centerX);
                    nbt.putInt("posY", blockPos.y);
                    nbt.putInt("posZ", blockPos.z - info.centerZ);
                } catch (Exception ignored) {
                    // No map data, nothing to report.
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        data.loadData(nbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        data.saveData(nbt);
    }
}
