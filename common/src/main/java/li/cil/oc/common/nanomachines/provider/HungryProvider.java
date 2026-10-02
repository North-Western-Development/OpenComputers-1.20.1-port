package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.integration.util.DamageSourceWithRandomCause;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class HungryProvider extends ScalaProvider {
    public static final HungryProvider INSTANCE = new HungryProvider();

    public static final int FillCount = 10; // Create a bunch of these to have a higher chance of one being picked / available.

    /**
     * Data-driven damage type (data/opencomputers/damage_type/nanomachines_hungry.json); bypasses
     * armor and effects via the vanilla damage type tags.
     */
    public static final ResourceKey<DamageType> HungryDamageType = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("opencomputers", "nanomachines_hungry"));

    private HungryProvider() {
        super("d697c24a-014c-4773-a288-23084a59e9e8");
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        final List<Behavior> result = new ArrayList<>();
        for (int i = 0; i < FillCount; i++) result.add(new HungryBehavior(player));
        return result;
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new HungryBehavior(player);
    }

    public static class HungryBehavior extends AbstractBehavior {
        public HungryBehavior(Player player) {
            super(player);
        }

        @Override
        public void onDisable(DisableReason reason) {
            if (reason == DisableReason.OutOfEnergy) {
                player.hurt(DamageSourceWithRandomCause.create(player.level(), HungryDamageType, 3), Settings.get().nanomachinesHungryDamage);
                Nanomachines.getController(player).changeBuffer(Settings.get().nanomachinesHungryEnergyRestored);
            }
        }
    }
}
