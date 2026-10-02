package li.cil.oc.fabric;

import li.cil.oc.OpenComputers;
import net.fabricmc.api.ClientModInitializer;

public final class OpenComputersFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OpenComputers.initClient();
    }
}
