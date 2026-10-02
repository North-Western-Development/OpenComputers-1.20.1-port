package li.cil.oc.common.transfer;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * Implemented by OpenComputers block entities that accept energy from other
 * mods (power acceptors). The loader modules expose the returned handler as
 * Forge {@code ENERGY} capability / Team Reborn Energy {@code EnergyStorage}.
 */
public interface EnergyHandlerProvider {
    @Nullable
    EnergyHandler getEnergyHandler(@Nullable Direction side);
}
