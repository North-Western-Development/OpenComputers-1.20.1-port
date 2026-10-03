package li.cil.oc.integration.jade;

import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

/**
 * Jade (successor of WAILA / Hwyla) integration. Everything is wired through Jade's own plugin
 * discovery ({@link JadePlugin}: Forge {@code @WailaPlugin} scan, Fabric {@code "jade"} entrypoint),
 * so there is nothing to do here; the proxy only makes the integration show up in {@link Mods}.
 */
public final class ModJade implements ModProxy {
    public static final ModJade INSTANCE = new ModJade();

    private ModJade() {
    }

    @Override
    public Mod getMod() {
        return Mods.Jade;
    }
}
