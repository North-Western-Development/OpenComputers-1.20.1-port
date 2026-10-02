package li.cil.oc;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import li.cil.oc.common.Proxy;
import li.cil.oc.common.init.Blocks;
import li.cil.oc.common.init.Items;
import li.cil.oc.integration.Mods;
import li.cil.oc.util.ThreadPoolFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;

/**
 * Loader independent mod entry point. Both the Forge ({@code li.cil.oc.forge.OpenComputersForge})
 * and the Fabric ({@code li.cil.oc.fabric.OpenComputersFabric[Client]}) entrypoints call
 * {@link #init()} during mod construction and, on the client, {@link #initClient()}.
 */
public final class OpenComputers {
    public static final String ID = "opencomputers";

    public static final String Name = "OpenComputers";

    public static final Logger log = LogManager.getLogger(Name);

    private static volatile Proxy proxy;

    private static boolean initialized;

    private OpenComputers() {
    }

    /**
     * The side specific proxy: {@code li.cil.oc.client.Proxy} on the physical
     * client, {@code li.cil.oc.common.Proxy} on a dedicated server. The client
     * proxy is instantiated reflectively so the dedicated server never loads
     * client classes.
     */
    public static Proxy proxy() {
        Proxy result = proxy;
        if (result == null) {
            synchronized (OpenComputers.class) {
                result = proxy;
                if (result == null) {
                    final String className = Platform.getEnvironment() == Env.CLIENT
                            ? "li.cil.oc.client.Proxy"
                            : "li.cil.oc.common.Proxy";
                    try {
                        final Constructor<?> ctor = Class.forName(className).getDeclaredConstructor();
                        ctor.setAccessible(true);
                        result = (Proxy) ctor.newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Failed instantiating proxy " + className, e);
                    }
                    proxy = result;
                }
            }
        }
        return result;
    }

    /**
     * The version of the mod, as declared in the loader's mod metadata.
     */
    public static String version() {
        return Platform.getMod(ID).getVersion();
    }

    /**
     * Common initialization, called by both loaders during mod construction.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        Settings.load(Platform.getConfigFolder().resolve("opencomputers").resolve("settings.conf").toFile());

        ThreadPoolFactory.init();
        Mods.preInit(); // Must happen after loading Settings but before registries are registered.

        // Fabric registers entries immediately when a DeferredRegister is registered, so
        // blocks and items must exist before the block entity types etc. that reference them.
        CreativeTab.register();
        Blocks.init();
        Items.init();

        // Sets up the registries the proxy owns (menu / entity / block entity types,
        // recipe serializers, sounds, loot functions), packets, events and API implementations.
        final Proxy proxy = proxy();
        proxy.preInit();

        // Formerly FMLCommonSetupEvent / FMLLoadCompleteEvent subscriptions on the proxy.
        LifecycleEvent.SETUP.register(() -> proxy().init());
        // TODO(port): there is no Architectury equivalent of FMLLoadCompleteEvent; lock the
        //  driver registry once the server is about to start (after all mods had their setup).
        LifecycleEvent.SERVER_BEFORE_START.register(server -> proxy().postInit());

        // TODO(port): Forge IMC (InterModProcessEvent -> IMC.handleMessage) is gone. Other mods
        //  call the li.cil.oc.api.IMC methods directly instead of sending messages.
    }

    /**
     * Client-only initialization, called by both loaders on the physical client
     * after {@link #init()}.
     */
    public static void initClient() {
        proxy().initClient();
    }
}
