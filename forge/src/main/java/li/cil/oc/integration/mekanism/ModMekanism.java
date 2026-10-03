package li.cil.oc.integration.mekanism;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

/**
 * Integration with Mekanism (mod id {@code mekanism}, Forge only): converts chemical stacks
 * (gases, infuse types, pigments, slurries) and exposes chemical tanks through adapters.
 */
public final class ModMekanism implements ModProxy {
    public static final ModMekanism INSTANCE = new ModMekanism();

    private ModMekanism() {
    }

    @Override
    public Mod getMod() {
        return Mods.Mekanism;
    }

    @Override
    public void initialize() {
        Driver.add(ConverterChemicalStack.INSTANCE);
        Driver.add(new DriverChemicalHandler());
    }
}
