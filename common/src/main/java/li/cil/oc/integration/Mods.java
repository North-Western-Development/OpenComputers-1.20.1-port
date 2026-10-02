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
 * Third-party integrations (AE2, ComputerCraft, Mekanism, ProjectRed, TIS-3D, EnderStorage, JEI, WAILA)
 * are parked in {@code legacy/}; only their ids are kept here.
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
    // TODO(port): integration - AppliedEnergistics2, ComputerCraft, Forge, JustEnoughItems, Mekanism,
    //  TIS3D, ProjectRedTransmission, DraconicEvolution, EnderStorage mod handles were dropped with
    //  their integrations (see legacy/).

    // ----------------------------------------------------------------------- //

    private static ModProxy[] proxies() {
        return new ModProxy[]{
                // Loader-generic item/fluid/energy drivers (formerly integration.minecraftforge).
                ModPlatform.INSTANCE,
                ModMinecraft.INSTANCE,

                // We go late to ensure all other mod integration is done, e.g. to
                // allow properly checking if wireless redstone is present.
                ModOpenComputers.INSTANCE
        };
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
            OpenComputers.log.debug("Pre-initializing mod integration for '" + nameOf(proxy) + "'.");
            try {
                proxy.preInitialize();
            } catch (Throwable e) {
                OpenComputers.log.warn("Error pre-initializing integration for '" + nameOf(proxy) + "'", e);
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
            OpenComputers.log.debug("Initializing mod integration for '" + nameOf(proxy) + "'.");
            try {
                proxy.initialize();
            } catch (Throwable e) {
                OpenComputers.log.warn("Error initializing integration for '" + nameOf(proxy) + "'", e);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    public static final class IDs {
        private IDs() {
        }

        public static final String AppliedEnergistics2 = "appliedenergistics2";
        public static final String ComputerCraft = "computercraft";
        public static final String Forge = "forge";
        public static final String JustEnoughItems = "jei";
        public static final String Mekanism = "mekanism";
        public static final String Minecraft = "minecraft";
        public static final String OpenComputers = "opencomputers";
        public static final String TIS3D = "tis3d";
        public static final String Waila = "waila";
        public static final String ProjectRedTransmission = "projectred-transmission";
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
