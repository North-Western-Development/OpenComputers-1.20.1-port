package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.util.PlayerUtils;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ParticleProvider extends ScalaProvider {
    public static final ParticleProvider INSTANCE = new ParticleProvider();

    public static final SimpleParticleType[] ParticleTypeList = new SimpleParticleType[]{
            ParticleTypes.FIREWORK,
            ParticleTypes.SMOKE,
            ParticleTypes.WITCH,
            ParticleTypes.NOTE,
            ParticleTypes.ENCHANT,
            ParticleTypes.FLAME,
            ParticleTypes.LAVA,
            ParticleTypes.SPLASH,
            ParticleTypes.ITEM_SLIME,
            ParticleTypes.HEART,
            ParticleTypes.HAPPY_VILLAGER
    };

    private ParticleProvider() {
        super("b48c4bbd-51bb-4915-9367-16cff3220e4b");
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        final List<Behavior> result = new ArrayList<>();
        for (SimpleParticleType type : ParticleTypeList) result.add(new ParticleBehavior(type, player));
        return result;
    }

    // Port note: 1.16.5 stored the numeric Forge registry id under "effectName"; registry ids are not
    // stable across loaders, so the registry name is stored as a string now (old ints are still read).
    @Override
    protected void writeBehaviorToNBT(Behavior behavior, CompoundTag nbt) {
        if (behavior instanceof ParticleBehavior particles) {
            nbt.putString("effectName", String.valueOf(BuiltInRegistries.PARTICLE_TYPE.getKey(particles.effectType)));
        }
        // else: Wat.
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        final ParticleType<?> effectType;
        if (nbt.contains("effectName", Tag.TAG_STRING)) {
            effectType = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.tryParse(nbt.getString("effectName")));
        } else {
            effectType = BuiltInRegistries.PARTICLE_TYPE.byId(nbt.getInt("effectName"));
        }
        return new ParticleBehavior(effectType instanceof SimpleParticleType simple ? simple : ParticleTypes.SMOKE, player);
    }

    public static class ParticleBehavior extends AbstractBehavior {
        public SimpleParticleType effectType;

        public ParticleBehavior(SimpleParticleType effectType, Player player) {
            super(player);
            this.effectType = effectType;
        }

        @Override
        public String getNameHint() {
            final ResourceLocation key = BuiltInRegistries.PARTICLE_TYPE.getKey(effectType);
            return "particles." + (key != null ? key.getPath() : "unknown");
        }

        @Override
        public void update() {
            final Level world = player.level();
            if (world.isClientSide && Settings.get().enableNanomachinePfx) {
                PlayerUtils.spawnParticleAround(player, effectType, Nanomachines.getController(player).getInputCount(this) * 0.25);
            }
        }
    }
}
