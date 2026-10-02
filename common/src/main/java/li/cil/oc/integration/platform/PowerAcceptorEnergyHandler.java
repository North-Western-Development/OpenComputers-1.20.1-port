package li.cil.oc.integration.platform;

import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.transfer.EnergyHandler;
import li.cil.oc.integration.util.Power;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * Exposes an OpenComputers power acceptor's buffer as a platform energy storage
 * (Forge Energy / Team Reborn Energy). Power acceptors implement
 * {@link li.cil.oc.common.transfer.EnergyHandlerProvider} by returning
 * {@code new PowerAcceptorEnergyHandler(this, side)}; the loader modules then expose it.
 * Formerly {@code integration.minecraftforge.EventHandlerMinecraftForge.Provider.EnergyStorageImpl}.
 */
public final class PowerAcceptorEnergyHandler implements EnergyHandler {
    private final PowerAcceptor tile;
    @Nullable
    private final Direction side;

    public PowerAcceptorEnergyHandler(PowerAcceptor tile, @Nullable Direction side) {
        this.tile = tile;
        this.side = side;
    }

    @Override
    public long getEnergyStored() {
        return Power.toFE(tile.globalBuffer(side));
    }

    @Override
    public long getMaxEnergyStored() {
        return Power.toFE(tile.globalBufferSize(side));
    }

    @Override
    public boolean canReceive() {
        return tile.canConnectPower(side);
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        return Power.toFE(tile.tryChangeBuffer(side, Power.fromFE(maxReceive), !simulate));
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        return 0;
    }
}
