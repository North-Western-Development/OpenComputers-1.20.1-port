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
// The 1.16 AppliedEnergistics2 power trait (OC blocks as AE grid nodes drawing AE power) was dropped:
// it would need every power-accepting OC block to host an AE2 managed grid node (IInWorldGridNodeHost),
// while FE / TR Energy, which OC accepts, cover power transfer on both loaders.
public interface PowerAcceptor extends li.cil.oc.common.tileentity.traits.power.Common, EnergyHandlerProvider {
    @Nullable
    @Override
    default EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return new PowerAcceptorEnergyHandler(this, side);
    }
}
