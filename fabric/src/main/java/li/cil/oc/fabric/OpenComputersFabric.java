package li.cil.oc.fabric;

import li.cil.oc.OpenComputers;
import net.fabricmc.api.ModInitializer;

public final class OpenComputersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OpenComputers.init();
        FabricStorageProviders.register();
    }
}
