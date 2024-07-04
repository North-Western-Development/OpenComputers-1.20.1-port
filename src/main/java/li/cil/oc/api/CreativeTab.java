package li.cil.oc.api;


import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Allows access to the creative tab used by OpenComputers.
 */
public final class CreativeTab {
    /**
     * The creative tab used by OpenComputers.
     * <br>
     * Changed to the actual tab if OC is present. Preferably you do
     * <em>not</em> try to access this anyway when OpenComputers isn't
     * present (don't ship the API in your mod), so don't rely on this!
     */
    public static ResourceKey<CreativeModeTab> instance = CreativeModeTabs.REDSTONE_BLOCKS;

    private CreativeTab() {
    }
}
