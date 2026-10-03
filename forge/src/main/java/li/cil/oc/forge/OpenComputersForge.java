package li.cil.oc.forge;

import dev.architectury.platform.forge.EventBuses;
import li.cil.oc.OpenComputers;
import li.cil.oc.integration.Mods;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(OpenComputers.ID)
public final class OpenComputersForge {
    public OpenComputersForge() {
        final var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(OpenComputers.ID, modBus);

        // Forge-only optional integrations (loaded only when the mod is present).
        Mods.registerOptional(Mods.IDs.Mekanism, "li.cil.oc.integration.mekanism.ModMekanism");
        Mods.registerOptional(Mods.IDs.EnderStorage, "li.cil.oc.integration.enderstorage.ModEnderStorage");
        Mods.registerOptional(Mods.IDs.ProjectRedTransmission, "li.cil.oc.integration.projectred.ModProjectRed");

        OpenComputers.init();
        MinecraftForge.EVENT_BUS.register(ForgeCapabilityProviders.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            OpenComputers.initClient();
        }
    }
}
