package li.cil.oc.integration.platform;

import li.cil.oc.api.Driver;
import li.cil.oc.api.IMC;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

/**
 * Loader-generic transfer integration (formerly {@code integration.minecraftforge.ModMinecraftForge}):
 * energy storage driver and item charging through the platform energy API.
 * <p>
 * Exposing OC's own power acceptors to other mods is done by the loader modules
 * (Forge capabilities / Team Reborn Energy) for every block entity implementing
 * {@link li.cil.oc.common.transfer.EnergyHandlerProvider}, see {@link PowerAcceptorEnergyHandler}.
 * Item / fluid drivers for other mods' storages live in {@code integration.minecraft}
 * ({@code DriverInventory}, {@code DriverFluidHandler}).
 */
public final class ModPlatform implements ModProxy {
    public static final ModPlatform INSTANCE = new ModPlatform();

    private ModPlatform() {
    }

    /**
     * Always enabled, like the vanilla integration.
     */
    @Override
    public Mod getMod() {
        return Mods.Minecraft;
    }

    @Override
    public void initialize() {
        IMC.registerItemCharge("Platform",
                "li.cil.oc.integration.platform.ItemEnergyCharge.canCharge",
                "li.cil.oc.integration.platform.ItemEnergyCharge.charge");
        Driver.add(DriverEnergyStorage.INSTANCE);
    }
}
