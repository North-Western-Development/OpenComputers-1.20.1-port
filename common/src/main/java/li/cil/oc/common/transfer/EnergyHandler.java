package li.cil.oc.common.transfer;

/**
 * Loader-agnostic energy storage, modelled after Forge's {@code IEnergyStorage}.
 * Units are Forge Energy (FE); on Fabric this maps 1:1 onto Team Reborn Energy (E).
 */
public interface EnergyHandler {
    /** @return amount accepted. */
    long receiveEnergy(long maxReceive, boolean simulate);

    /** @return amount extracted. */
    long extractEnergy(long maxExtract, boolean simulate);

    long getEnergyStored();

    long getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
