package li.cil.oc.integration;

import dev.architectury.platform.Platform;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.integration.minecraft.ModMinecraft;
import li.cil.oc.integration.opencomputers.ModOpenComputers;
import li.cil.oc.integration.platform.ModPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry of the built-in mod integrations.
 * <p>
 * Lifecycle (all static):
 * <ul>
 * <li>{@link #preInit()} - called from {@code OpenComputers.init()} after settings were loaded.</li>
 * <li>{@link #init()} - called from {@code common.Proxy.init()} (common setup); registers drivers etc.</li>
 * </ul>
 * Optional third-party integrations are listed in {@link #OPTIONAL} (common) or registered by a
 * loader module via {@link #registerOptional(String, String)} (loader-specific mods), by class name.
 * Their proxy classes are only loaded when the mod is present, so they may freely reference the
 * other mod's classes. Each such class must have a {@code public static final INSTANCE} field.
 */
public final class Mods {
    private Mods() {
    }

    private static boolean preInited = false;
    private static boolean inited = false;

    private static final List<ModBase> knownMods = new ArrayList<>();

    // ----------------------------------------------------------------------- //

    public static List<ModBase> All() {
        synchronized (knownMods) {
            return new ArrayList<>(knownMods);
        }
    }

    public static final SimpleMod Minecraft = new SimpleMod(IDs.Minecraft);
    public static final SimpleMod OpenComputers = new SimpleMod(IDs.OpenComputers);
    public static final SimpleMod AppliedEnergistics2 = new SimpleMod(IDs.AppliedEnergistics2);
    public static final SimpleMod ComputerCraft = new SimpleMod(IDs.ComputerCraft);
    public static final SimpleMod JustEnoughItems = new SimpleMod(IDs.JustEnoughItems);
    public static final SimpleMod Jade = new SimpleMod(IDs.Jade);
    public static final SimpleMod TIS3D = new SimpleMod(IDs.TIS3D);
    public static final SimpleMod Mekanism = new SimpleMod(IDs.Mekanism);
    public static final SimpleMod EnderStorage = new SimpleMod(IDs.EnderStorage);
    public static final SimpleMod ProjectRedTransmission = new SimpleMod(IDs.ProjectRedTransmission);

    // ----------------------------------------------------------------------- //

    /** Optional integrations available on both loaders: mod id -> proxy class name. */
    private static final String[][] OPTIONAL = {
            {IDs.AppliedEnergistics2, "li.cil.oc.integration.appeng.ModAppEng"},
            {IDs.ComputerCraft, "li.cil.oc.integration.computercraft.ModComputerCraft"},
            {IDs.JustEnoughItems, "li.cil.oc.integration.jei.ModJEI"},
            {IDs.Jade, "li.cil.oc.integration.jade.ModJade"},
            {IDs.TIS3D, "li.cil.oc.integration.tis3d.ModTIS3D"},
    };

    private static final List<String[]> registeredOptional = new ArrayList<>();

    /**
     * Registers a loader-specific optional integration (called by the loader entrypoint before
     * {@code OpenComputers.init()}). The proxy class is only loaded if {@code modId} is present.
     */
    public static synchronized void registerOptional(String modId, String proxyClassName) {
        registeredOptional.add(new String[]{modId, proxyClassName});
    }

    private static List<ModProxy> proxies;

    private static synchronized List<ModProxy> proxies() {
        if (proxies == null) {
            final List<ModProxy> result = new ArrayList<>();
            // Loader-generic item/fluid/energy drivers (formerly integration.minecraftforge).
            result.add(ModPlatform.INSTANCE);
            result.add(ModMinecraft.INSTANCE);

            final List<String[]> optional = new ArrayList<>(List.of(OPTIONAL));
            optional.addAll(registeredOptional);
            for (String[] entry : optional) {
                if (!Platform.isModLoaded(entry[0])) continue;
                try {
                    result.add((ModProxy) Class.forName(entry[1]).getField("INSTANCE").get(null));
                } catch (Throwable e) {
                    li.cil.oc.OpenComputers.log.warn("Failed loading integration for '" + entry[0] + "'.", e);
                }
            }

            // We go late to ensure all other mod integration is done, e.g. to
            // allow properly checking if wireless redstone is present.
            result.add(ModOpenComputers.INSTANCE);
            proxies = result;
        }
        return proxies;
    }

    public static void preInit() {
        if (!preInited) {
            preInited = true;
            for (ModProxy proxy : proxies()) {
                tryPreInit(proxy);
            }
        }
    }

    private static boolean isEnabled(ModProxy proxy) {
        final Mod mod = proxy.getMod();
        final boolean isBlacklisted = mod != null && Settings.get().modBlacklist.contains(mod.id());
        final boolean alwaysEnabled = mod == null || mod == Mods.Minecraft;
        return !isBlacklisted && (alwaysEnabled || mod.isModAvailable());
    }

    private static String nameOf(ModProxy proxy) {
        final Mod mod = proxy.getMod();
        return mod == null ? proxy.getClass().getSimpleName() : mod.id();
    }

    private static void tryPreInit(ModProxy proxy) {
        if (isEnabled(proxy)) {
            li.cil.oc.OpenComputers.log.debug("Pre-initializing mod integration for '" + nameOf(proxy) + "'.");
            try {
                proxy.preInitialize();
            } catch (Throwable e) {
                li.cil.oc.OpenComputers.log.warn("Error pre-initializing integration for '" + nameOf(proxy) + "'", e);
            }
        }
    }

    public static void init() {
        if (!inited) {
            inited = true;
            for (ModProxy proxy : proxies()) {
                tryInit(proxy);
            }
        }
    }

    private static void tryInit(ModProxy proxy) {
        if (isEnabled(proxy)) {
            li.cil.oc.OpenComputers.log.debug("Initializing mod integration for '" + nameOf(proxy) + "'.");
            try {
                proxy.initialize();
            } catch (Throwable e) {
                li.cil.oc.OpenComputers.log.warn("Error initializing integration for '" + nameOf(proxy) + "'", e);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    public static final class IDs {
        private IDs() {
        }

        public static final String AppliedEnergistics2 = "ae2";
        public static final String ComputerCraft = "computercraft";
        public static final String Forge = "forge";
        public static final String JustEnoughItems = "jei";
        public static final String Mekanism = "mekanism";
        public static final String Minecraft = "minecraft";
        public static final String OpenComputers = "opencomputers";
        public static final String TIS3D = "tis3d";
        public static final String Jade = "jade";
        public static final String ProjectRedCore = "projectred_core";
        public static final String ProjectRedTransmission = "projectred_transmission";
        public static final String DraconicEvolution = "draconicevolution";
        public static final String EnderStorage = "enderstorage";
    }

    // ----------------------------------------------------------------------- //

    public abstract static class ModBase implements Mod {
        protected ModBase() {
            synchronized (knownMods) {
                knownMods.add(this);
            }
        }

        public Optional<dev.architectury.platform.Mod> container() {
            return Platform.getOptionalMod(id());
        }

        public Optional<String> version() {
            return container().map(dev.architectury.platform.Mod::getVersion);
        }
    }

    public static class SimpleMod extends ModBase {
        private final String id;
        private final String version;
        private Boolean isModAvailable_ = null;

        public SimpleMod(String id) {
            this(id, "");
        }

        /**
         * @param version currently ignored: maven version ranges are not available in
         *                a loader-independent way. TODO(port): check version ranges if ever needed again.
         */
        public SimpleMod(String id, String version) {
            this.id = id;
            this.version = version;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isModAvailable() {
            if (isModAvailable_ == null) {
                isModAvailable_ = Platform.isModLoaded(id);
            }
            return isModAvailable_;
        }
    }

    public static class ClassBasedMod extends ModBase {
        private final String id;
        private final String[] classNames;
        private Boolean isModAvailable_ = null;

        public ClassBasedMod(String id, String... classNames) {
            this.id = id;
            this.classNames = classNames;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isModAvailable() {
            if (isModAvailable_ == null) {
                boolean available = Platform.isModLoaded(id);
                for (String className : classNames) {
                    if (!available) break;
                    try {
                        available = Class.forName(className) != null;
                    } catch (Throwable t) {
                        available = false;
                    }
                }
                isModAvailable_ = available;
            }
            return isModAvailable_;
        }
    }
}
