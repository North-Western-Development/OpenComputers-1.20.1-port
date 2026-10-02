package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.EventHandler;
import li.cil.oc.server.component.traits.WorldAware;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UpgradeLeash extends AbstractManagedEnvironment implements WorldAware, DeviceInfo {
    public final Entity host;

    public final int MaxLeashedEntities = 8;

    public final Set<UUID> leashedEntities = new HashSet<>();

    public UpgradeLeash(Entity host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("leash").
            create());
    }

    @Override
    public Component node() {
        return (Component) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Leash",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "FlockControl (FC-3LS)",
                DeviceAttribute.Capacity, String.valueOf(MaxLeashedEntities)
            );
        }
        return deviceInfo;
    }

    @Override
    public BlockPosition position() {
        return BlockPosition.apply(host);
    }

    @Callback(doc = "function(side:number):boolean -- Tries to put an entity on the specified side of the device onto a leash.")
    public Object[] leash(Context context, Arguments args) {
        if (leashedEntities.size() >= MaxLeashedEntities) return ResultWrapper.result(ResultWrapper.unit, "too many leashed entities");
        final Direction side = ExtendedArguments.checkSideAny(args, 0);
        final AABB nearBounds = position().bounds();
        final AABB farBounds = nearBounds.move(side.getStepX() * 2.0, side.getStepY() * 2.0, side.getStepZ() * 2.0);
        final AABB bounds = nearBounds.minmax(farBounds);
        for (Mob entity : entitiesInBounds(Mob.class, bounds)) {
            if (entity.canBeLeashed(fakePlayer())) {
                entity.setLeashedTo(host, true);
                leashedEntities.add(entity.getUUID());
                context.pause(0.1);
                return ResultWrapper.result(true);
            }
        }
        return ResultWrapper.result(ResultWrapper.unit, "no unleashed entity");
    }

    @Callback(doc = "function() -- Unleashes all currently leashed entities.")
    public Object[] unleash(Context context, Arguments args) {
        unleashAll();
        return null;
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            unleashAll();
        }
    }

    private void unleashAll() {
        for (Mob entity : entitiesInBounds(Mob.class, position().bounds().inflate(5, 5, 5))) {
            if (leashedEntities.contains(entity.getUUID()) && entity.getLeashHolder() == host) {
                entity.dropLeash(true, false);
            }
        }
        leashedEntities.clear();
    }

    private static final String LeashedEntitiesTag = "leashedEntities";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        final ListTag list = nbt.getList(LeashedEntitiesTag, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            leashedEntities.add(UUID.fromString(list.getString(i)));
        }
        // Re-acquire leashed entities. Need to do this manually because leashed
        // entities only remember their leashee if it's an LivingEntity...
        EventHandler.scheduleServer(() -> {
            final Set<UUID> foundEntities = new HashSet<>();
            for (Mob entity : entitiesInBounds(Mob.class, position().bounds().inflate(5, 5, 5))) {
                if (leashedEntities.contains(entity.getUUID())) {
                    entity.setLeashedTo(host, true);
                    foundEntities.add(entity.getUUID());
                }
            }
            final Set<UUID> missing = new HashSet<>(leashedEntities);
            missing.removeAll(foundEntities);
            if (!missing.isEmpty()) {
                OpenComputers.log.info("Could not find " + missing.size() + " leashed entities after loading!");
                leashedEntities.removeAll(missing);
            }
        });
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        final ListTag list = new ListTag();
        for (UUID uuid : leashedEntities) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        nbt.put(LeashedEntitiesTag, list);
    }
}
