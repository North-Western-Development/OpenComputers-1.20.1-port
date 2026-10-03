package li.cil.oc.integration.computercraft;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

/**
 * Integration with CC: Tweaked (mod id {@code computercraft}).
 */
public final class ModComputerCraft implements ModProxy {
    public static final ModComputerCraft INSTANCE = new ModComputerCraft();

    private ModComputerCraft() {
    }

    @Override
    public Mod getMod() {
        return Mods.ComputerCraft;
    }

    @Override
    public void initialize() {
        PeripheralProvider.init();

        Driver.add(DriverComputerCraftMedia.INSTANCE);
        Driver.add(new DriverPeripheral());

        Driver.add(ConverterLuaObject.INSTANCE);
    }
}
