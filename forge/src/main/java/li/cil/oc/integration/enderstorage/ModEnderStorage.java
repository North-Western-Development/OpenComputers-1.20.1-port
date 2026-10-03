package li.cil.oc.integration.enderstorage;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

/**
 * Integration with EnderStorage (mod id {@code enderstorage}, Forge only): read and set the
 * frequency of ender chests and ender tanks through adapters.
 */
public final class ModEnderStorage implements ModProxy {
    public static final ModEnderStorage INSTANCE = new ModEnderStorage();

    private ModEnderStorage() {
    }

    @Override
    public Mod getMod() {
        return Mods.EnderStorage;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverFrequencyOwner());
    }
}
