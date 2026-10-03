package li.cil.oc.integration.appeng;

import li.cil.oc.api.Driver;
import li.cil.oc.api.IMC;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

/**
 * Applied Energistics 2 (15.x for 1.20.1, Forge and Fabric) integration.
 * <p>
 * Dropped compared to 1.16:
 * <ul>
 * <li>Whitelisting OC's print block entity for spatial IO: AE2 15 moves every block entity with its
 *     default strategy unless it is in the {@code ae2:blacklisted/spatial} tag, so nothing to do.</li>
 * <li>The AE power trait for OC blocks (drawing AE power from an ME network): it would require OC
 *     blocks to host AE2 grid nodes; OC accepts FE (Forge) / TR Energy (Fabric) instead.</li>
 * </ul>
 */
public final class ModAppEng implements ModProxy {
    public static final ModAppEng INSTANCE = new ModAppEng();

    private ModAppEng() {
    }

    @Override
    public Mod getMod() {
        return Mods.AppliedEnergistics2;
    }

    @Override
    public void initialize() {
        IMC.registerWrenchTool("li.cil.oc.integration.appeng.EventHandlerAE2.useWrench");
        IMC.registerWrenchToolCheck("li.cil.oc.integration.appeng.EventHandlerAE2.isWrench");

        Driver.add(DriverController.INSTANCE);
        Driver.add(DriverExportBus.INSTANCE);
        Driver.add(DriverImportBus.INSTANCE);
        Driver.add(DriverPartInterface.INSTANCE);
        Driver.add(DriverBlockInterface.INSTANCE);

        Driver.add(ConverterCellInventory.INSTANCE);

        Driver.add(DriverController.Provider.INSTANCE);
        Driver.add(DriverExportBus.Provider.INSTANCE);
        Driver.add(DriverImportBus.Provider.INSTANCE);
        Driver.add(DriverPartInterface.Provider.INSTANCE);
        Driver.add(DriverBlockInterface.Provider.INSTANCE);
    }
}
