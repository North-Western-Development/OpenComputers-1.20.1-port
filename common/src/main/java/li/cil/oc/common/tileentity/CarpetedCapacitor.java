package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.common.tileentity.traits.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CarpetedCapacitor extends Capacitor implements Tickable {
    private Map<String, String> deviceInfo;

    private final Random rng = new Random();
    private final double chance = Settings.get().carpetDamageChance;
    private long nextChanceTime = 0;

    public CarpetedCapacitor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Battery",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "CarpetedCapBank3x",
                DeviceAttribute.Capacity, String.valueOf(maxCapacity())
            );
        }
        return deviceInfo;
    }

    private double energyFromGroup(Set<LivingEntity> entities, double power) {
        if (entities.size() < 2) return 0;
        final Level world = getLevel();
        if (chance > 0 && nextChanceTime < world.getGameTime()) {
            for (LivingEntity ent : entities) {
                if (rng.nextDouble() < chance) {
                    ent.hurt(world.damageSources().generic(), 1);
                    ent.setLastHurtByMob(ent); // panic
                    ent.knockback(0, .25, 0);
                    // wait a minute before the next possible shock
                    nextChanceTime = world.getGameTime() + (20 * 60);
                    break;
                }
            }
        }
        return power;
    }

    @Override
    public void updateEntity() {
        final Level world = getLevel();
        if (node != null && (world.getGameTime() + hashCode()) % 20 == 0) {
            final Set<LivingEntity> entities = new LinkedHashSet<>();
            for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, capacitorPowerBounds())) {
                if (entity.isAlive()) entities.add(entity);
            }
            final Set<LivingEntity> sheep = new LinkedHashSet<>();
            final Set<LivingEntity> ocelots = new LinkedHashSet<>();
            for (LivingEntity entity : entities) {
                if (entity instanceof Sheep) sheep.add(entity);
                if (entity instanceof Ocelot) ocelots.add(entity);
            }
            final double sheepPower = energyFromGroup(sheep, Settings.get().sheepPower);
            final double ocelotPower = energyFromGroup(ocelots, Settings.get().ocelotPower);
            final double totalPower = sheepPower + ocelotPower;
            if (totalPower > 0) {
                node.changeBuffer(totalPower);
            }
        }
    }

    private AABB capacitorPowerBounds() {
        return position().offset(Direction.UP).bounds();
    }
}
