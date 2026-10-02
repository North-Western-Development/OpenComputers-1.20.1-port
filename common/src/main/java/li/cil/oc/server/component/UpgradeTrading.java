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
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.component.traits.WorldAware;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UpgradeTrading extends AbstractManagedEnvironment implements WorldAware, DeviceInfo {
    public final EnvironmentHost host;

    public UpgradeTrading(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("trading").
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
                DeviceAttribute.Description, "Trading upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Capitalism H.O. 1200T"
            );
        }
        return deviceInfo;
    }

    @Override
    public BlockPosition position() {
        return BlockPosition.apply(host);
    }

    public double maxRange() {
        return Settings.get().tradingRange;
    }

    public boolean isInRange(Entity entity) {
        return new Vec3(entity.getX(), entity.getY(), entity.getZ()).distanceTo(position().toVec3()) <= maxRange();
    }

    @Callback(doc = "function():table -- Returns a table of trades in range as userdata objects.")
    public Object[] getTrades(Context context, Arguments args) {
        final List<Entity> merchants = new ArrayList<>();
        for (Entity entity : entitiesInBounds(Entity.class, position().bounds().inflate(maxRange(), maxRange(), maxRange()))) {
            if (isInRange(entity) && entity instanceof Merchant) merchants.add(entity);
        }
        int nextId = 1;
        final Map<UUID, Integer> idMap = new HashMap<>();
        final List<UUID> ids = new ArrayList<>();
        for (Entity merchant : merchants) ids.add(merchant.getUUID());
        ids.sort(Comparator.naturalOrder());
        for (UUID id : ids) {
            idMap.put(id, nextId);
            nextId += 1;
        }
        // sorting the result is not necessary, but will help the merchant trades line up nicely by merchant
        merchants.sort(Comparator.comparing(Entity::getUUID));
        final List<Trade> trades = new ArrayList<>();
        for (Entity entity : merchants) {
            final Merchant merchant = (Merchant) entity;
            for (int index = 0; index < merchant.getOffers().size(); index++) {
                trades.add(new Trade(this, merchant, index, idMap.get(entity.getUUID())));
            }
        }
        return ResultWrapper.result((Object) trades.toArray(new Trade[0]));
    }
}
