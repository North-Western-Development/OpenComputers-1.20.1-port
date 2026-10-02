package li.cil.oc.integration.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Damage source that picks one of several death messages at random
 * ({@code death.attack.<msgId>.1 .. N}, optionally with a {@code .player} variant).
 * <p>
 * In 1.20.1 damage sources are backed by data-driven damage types; armor / magic
 * bypassing is configured via the {@code minecraft:bypasses_armor} /
 * {@code minecraft:bypasses_effects} damage type tags.
 */
public class DamageSourceWithRandomCause extends DamageSource {
    private final int numCauses;

    public DamageSourceWithRandomCause(Holder<DamageType> type, int numCauses) {
        super(type);
        this.numCauses = numCauses;
    }

    /**
     * Builds a damage source for the given damage type in the level's registries.
     */
    public static DamageSourceWithRandomCause create(Level level, ResourceKey<DamageType> key, int numCauses) {
        return new DamageSourceWithRandomCause(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), numCauses);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity damagee) {
        final LivingEntity damager = damagee.getKillCredit();
        final String format = "death.attack." + getMsgId() + "." + (damagee.level().random.nextInt(numCauses) + 1);
        final String withCauseFormat = format + ".player";
        if (damager != null && Language.getInstance().has(withCauseFormat)) {
            return Component.translatable(withCauseFormat, damagee.getDisplayName(), damager.getDisplayName());
        } else {
            return Component.translatable(format, damagee.getDisplayName());
        }
    }
}
