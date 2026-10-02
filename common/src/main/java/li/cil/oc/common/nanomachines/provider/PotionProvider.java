package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PotionProvider extends ScalaProvider {
    public static final PotionProvider INSTANCE = new PotionProvider();

    // Lazy to give other mods a chance to register their potions.
    private Set<MobEffect> potionWhitelist;

    private PotionProvider() {
        super("c29e4eec-5a46-479a-9b3d-ad0f06da784a");
    }

    public Set<MobEffect> PotionWhitelist() {
        if (potionWhitelist == null) potionWhitelist = filterPotions(Settings.get().nanomachinePotionWhitelist);
        return potionWhitelist;
    }

    public static Set<MobEffect> filterPotions(Iterable<?> list) {
        final Set<MobEffect> result = new LinkedHashSet<>();
        for (Object entry : list) {
            MobEffect potion = null;
            if (entry instanceof String name) {
                final ResourceLocation location = ResourceLocation.tryParse(name);
                potion = location != null ? BuiltInRegistries.MOB_EFFECT.get(location) : null;
            } else if (entry instanceof ResourceLocation loc) {
                potion = BuiltInRegistries.MOB_EFFECT.get(loc);
            } else if (entry instanceof Number id) {
                potion = MobEffect.byId(id.intValue());
            }
            if (potion != null) result.add(potion);
        }
        return result;
    }

    public boolean isPotionEligible(MobEffect potion) {
        return potion != null && PotionWhitelist().contains(potion);
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        final List<Behavior> result = new ArrayList<>();
        for (MobEffect potion : BuiltInRegistries.MOB_EFFECT) {
            if (isPotionEligible(potion)) result.add(new PotionBehavior(potion, player));
        }
        return result;
    }

    @Override
    protected void writeBehaviorToNBT(Behavior behavior, CompoundTag nbt) {
        if (behavior instanceof PotionBehavior potionBehavior) {
            nbt.putString("potionId", String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(potionBehavior.potion)));
        }
        // else: Shouldn't happen, ever.
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        final String potionId = nbt.getString("potionId");
        final ResourceLocation location = ResourceLocation.tryParse(potionId);
        // TODO(port): Forge's registry returned a default (null-safe) value for unknown ids; we return null
        //  (no behavior) instead of a behavior wrapping a null effect.
        final MobEffect potion = location != null ? BuiltInRegistries.MOB_EFFECT.get(location) : null;
        return potion != null ? new PotionBehavior(potion, player) : null;
    }

    public static class PotionBehavior extends AbstractBehavior {
        public static final int Duration = 600;

        public final MobEffect potion;

        public PotionBehavior(MobEffect potion, Player player) {
            super(player);
            this.potion = potion;
        }

        public int amplifier(Player player) {
            return Nanomachines.getController(player).getInputCount(this) - 1;
        }

        @Override
        public String getNameHint() {
            final String id = potion.getDescriptionId();
            return id.startsWith("effect.") ? id.substring("effect.".length()) : id;
        }

        @Override
        public void onDisable(DisableReason reason) {
            player.removeEffect(potion);
        }

        @Override
        public void update() {
            player.addEffect(new MobEffectInstance(potion, Duration, amplifier(player), true, Settings.get().enableNanomachinePfx));
        }
    }
}
