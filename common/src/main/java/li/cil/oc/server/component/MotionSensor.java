package li.cil.oc.server.component;

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
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.SideTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MotionSensor extends AbstractManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    private final int radius = 8;

    private double sensitivity = 0.4;

    private final Map<LivingEntity, Vec3> trackedEntities = new HashMap<>();

    public MotionSensor(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("motion_sensor").
            withConnector().
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Motion sensor",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Blinker M1K0",
                DeviceAttribute.Capacity, String.valueOf(radius)
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    private Level world() {
        return host.world();
    }

    private double x() {
        return host.xPosition();
    }

    private double y() {
        return host.yPosition();
    }

    private double z() {
        return host.zPosition();
    }

    private boolean isServer() {
        return world() != null ? !world().isClientSide : SideTracker.isServer();
    }

    @Override
    public boolean canUpdate() {
        return isServer();
    }

    @Override
    public void update() {
        super.update();
        if (world().getGameTime() % 10 == 0) {
            // Get a list of all living entities we could possibly detect, using a rough
            // bounding box check, then refining it using the actual distance and an
            // actual visibility check.
            final Set<LivingEntity> entities = new HashSet<>();
            for (LivingEntity entity : world().getEntitiesOfClass(LivingEntity.class, sensorBounds())) {
                if (entity.isAlive() && isInRange(entity) && isVisible(entity)) {
                    entities.add(entity);
                }
            }
            // Get rid of all tracked entities that are no longer visible.
            trackedEntities.keySet().retainAll(entities);
            // Check for which entities we should generate a signal.
            for (LivingEntity entity : entities) {
                final Vec3 prev = trackedEntities.get(entity);
                if (prev != null) {
                    // Known entity, check if it moved enough to trigger.
                    if (entity.distanceToSqr(prev.x, prev.y, prev.z) > sensitivity * sensitivity * 2) {
                        sendSignal(entity);
                    }
                }
                else {
                    // New, unknown entity, always trigger.
                    sendSignal(entity);
                }
                // Update tracked position.
                trackedEntities.put(entity, new Vec3(entity.getX(), entity.getY(), entity.getZ()));
            }
        }
    }

    private AABB sensorBounds() {
        return new AABB(
            x() + 0.5 - radius, y() + 0.5 - radius, z() + 0.5 - radius,
            x() + 0.5 + radius, y() + 0.5 + radius, z() + 0.5 + radius);
    }

    private boolean isInRange(LivingEntity entity) {
        return entity.distanceToSqr(x() + 0.5, y() + 0.5, z() + 0.5) <= radius * radius;
    }

    private boolean isClearPath(Vec3 target, Entity entity) {
        final Vec3 origin = new Vec3(x(), y(), z());
        final Vec3 path = target.subtract(origin).normalize();
        final Vec3 eye = origin.add(path);
        // Note: 1.20.1 ClipContext requires a non-null entity for its collision context.
        final HitResult trace = world().clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
        return trace.getType() == HitResult.Type.MISS;
    }

    private boolean isVisible(LivingEntity entity) {
        if (entity.getEffect(MobEffects.INVISIBILITY) != null) return false;
        // Note: it only working in lit conditions works and is neat, but this
        // is pseudo-infrared driven (it only works for *living* entities, after
        // all), so I think it makes more sense for it to work in the dark, too.
        /* entity.getBrightness(0) > 0.2 && */
        final Vec3 target = entity.position();
        return isClearPath(target, entity) || isClearPath(target.add(0.0D, entity.getEyeHeight(), 0.0D), entity);
    }

    private void sendSignal(LivingEntity entity) {
        if (Settings.get().inputUsername) {
            node().sendToReachable("computer.signal", "motion", entity.getX() - (x() + 0.5), entity.getY() - (y() + 0.5), entity.getZ() - (z() + 0.5), entity.getName().getString());
        }
        else {
            node().sendToReachable("computer.signal", "motion", entity.getX() - (x() + 0.5), entity.getY() - (y() + 0.5), entity.getZ() - (z() + 0.5));
        }
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():number -- Gets the current sensor sensitivity.")
    public Object[] getSensitivity(Context computer, Arguments args) {
        return ResultWrapper.result(sensitivity);
    }

    @Callback(direct = true, doc = "function(value:number):number -- Sets the sensor's sensitivity. Returns the old value.")
    public Object[] setSensitivity(Context computer, Arguments args) {
        final double oldValue = sensitivity;
        sensitivity = Math.max(0.2, args.checkDouble(0));
        return ResultWrapper.result(oldValue);
    }

    // ---------------------------------------------------------------------- //

    private static final String SensitivityTag = Settings.namespace + "sensitivity";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        sensitivity = nbt.getDouble(SensitivityTag);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        nbt.putDouble(SensitivityTag, sensitivity);
    }
}
