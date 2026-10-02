package li.cil.oc.api;

import net.minecraft.world.item.CreativeModeTab;

/**
 * Allows access to the creative tab used by OpenComputers.
 */
public final class CreativeTab {
    /**
     * The creative tab used by OpenComputers.
     * <p/>
     * Changed to the actual tab if OC is present. Preferably you do
     * <em>not</em> try to access this anyway when OpenComputers isn't
     * present (don't ship the API in your mod), so don't rely on this!
     * <p/>
     * Since 1.20.1 creative tabs are registry objects, so there is no
     * sensible vanilla fallback anymore: this is <tt>null</tt> until
     * OpenComputers has registered its tab.
     */
    public static CreativeModeTab instance = null;

    private CreativeTab() {
    }
}
