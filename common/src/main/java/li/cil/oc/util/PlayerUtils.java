package li.cil.oc.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    /**
     * Name of the tag the persisted data is stored under in the player's save data.
     */
    public static final String PERSISTED_NBT_TAG = "OpenComputersPersisted";

    /**
     * Implemented on {@link Player} by {@code li.cil.oc.common.mixin.PlayerPersistedDataMixin};
     * replaces Forge's {@code getPersistentData().getCompound(PERSISTED_NBT_TAG)}, i.e. data
     * that is saved with the player and survives death (copied over by
     * {@code li.cil.oc.common.mixin.ServerPlayerPersistedDataMixin}).
     */
    public interface PersistedDataHolder {
        CompoundTag oc$getPersistedData();

        void oc$setPersistedData(CompoundTag data);
    }

    public static CompoundTag persistedData(Player player) {
        return ((PersistedDataHolder) player).oc$getPersistedData();
    }

    public static void spawnParticleAround(Player player, ParticleOptions effectType, double chance) {
        final RandomSource rng = player.level().random;
        if (chance >= 1 || rng.nextDouble() < chance) {
            final AABB bounds = player.getBoundingBox();
            final double x = bounds.minX + (bounds.maxX - bounds.minX) * rng.nextDouble() * 1.5;
            final double y = bounds.minY + (bounds.maxY - bounds.minY) * rng.nextDouble() * 0.5;
            final double z = bounds.minZ + (bounds.maxZ - bounds.minZ) * rng.nextDouble() * 1.5;
            player.level().addParticle(effectType, x, y, z, 0, 0, 0);
        }
    }

    public static void spawnParticleAround(Player player, ParticleOptions effectType) {
        spawnParticleAround(player, effectType, 1.0);
    }
}
