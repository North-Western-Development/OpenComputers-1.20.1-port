package li.cil.oc.common.tileentity.traits;

import li.cil.oc.common.transfer.EnergyHandler;
import li.cil.oc.common.transfer.EnergyHandlerProvider;
import li.cil.oc.integration.platform.PowerAcceptorEnergyHandler;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * Accepts power from other mods: exposed by the loader modules as Forge Energy / Team Reborn
 * Energy through {@link EnergyHandlerProvider}.
 */
// TODO(port): integration - the AppliedEnergistics2 power trait was dropped.
public interface PowerAcceptor extends li.cil.oc.common.tileentity.traits.power.Common, EnergyHandlerProvider {
    @Nullable
    @Override
    default EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return new PowerAcceptorEnergyHandler(this, side);
    }
}
