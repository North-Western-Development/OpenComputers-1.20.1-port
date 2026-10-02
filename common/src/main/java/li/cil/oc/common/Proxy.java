package li.cil.oc.common;

import dev.architectury.event.events.common.LifecycleEvent;
import li.cil.oc.Constants;
import li.cil.oc.CreativeTab;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.entity.EntityTypes;
import li.cil.oc.common.event.AngelUpgradeHandler;
import li.cil.oc.common.event.ChunkloaderUpgradeHandler;
import li.cil.oc.common.event.ExperienceUpgradeHandler;
import li.cil.oc.common.event.FileSystemAccessHandler;
import li.cil.oc.common.event.HoverBootsHandler;
import li.cil.oc.common.event.NanomachinesHandler;
import li.cil.oc.common.event.NetworkActivityHandler;
import li.cil.oc.common.event.RobotCommonHandler;
import li.cil.oc.common.event.WirelessNetworkCardHandler;
import li.cil.oc.common.init.Items;
import li.cil.oc.common.recipe.RecipeSerializers;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.integration.Mods;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import li.cil.oc.server.machine.luac.NativeLua52Architecture;
import li.cil.oc.server.machine.luac.NativeLua53Architecture;
import li.cil.oc.server.machine.luaj.LuaJLuaArchitecture;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * Common (dedicated server) proxy; {@code li.cil.oc.client.Proxy} extends it.
 * Lifecycle (see {@link OpenComputers#init()}):
 * <ul>
 * <li>{@link #preInit()}: mod construction, before blocks/items are registered.</li>
 * <li>{@link #init()}: {@code LifecycleEvent.SETUP}.</li>
 * <li>{@link #postInit()}: {@code LifecycleEvent.SERVER_BEFORE_START}.</li>
 * <li>{@link #initClient()}: physical client only, after {@link OpenComputers#init()}.</li>
 * </ul>
 */
public class Proxy {
    private boolean postInitDone;

    public void preInit() {
        // Registries owned by the common proxy (blocks / items are registered by OpenComputers.init()).
        TileEntityTypes.init();
        ContainerTypes.init();
        EntityTypes.init();
        RecipeSerializers.init();
        Sound.init();
        LootFunctions.init();

        // Client -> server packets.
        li.cil.oc.server.PacketHandler.registerServerReceiver();

        // Event handlers.
        EventHandler.register();
        SaveHandler.register();
        Loot.register();
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(li.cil.oc.server.ComponentTracker.INSTANCE::onWorldUnload);
        AngelUpgradeHandler.register();
        ChunkloaderUpgradeHandler.register();
        ExperienceUpgradeHandler.register();
        FileSystemAccessHandler.register();
        HoverBootsHandler.register();
        NanomachinesHandler.Common.register();
        NetworkActivityHandler.register();
        RobotCommonHandler.register();
        WirelessNetworkCardHandler.register();

        OpenComputers.log.info("Initializing OpenComputers API.");

        li.cil.oc.api.API.driver = Registry.INSTANCE;
        li.cil.oc.api.API.fileSystem = li.cil.oc.server.fs.FileSystem.INSTANCE;
        li.cil.oc.api.API.items = Items.INSTANCE;
        li.cil.oc.api.API.machine = Machine.INSTANCE;
        li.cil.oc.api.API.nanomachines = li.cil.oc.common.nanomachines.Nanomachines.INSTANCE;
        li.cil.oc.api.API.network = li.cil.oc.server.network.Network.INSTANCE;

        li.cil.oc.api.API.config = Settings.get().config;

        // Weird JNLua bug identified
        // When loading JNLua (for either 5.2 or 5.3 lua state) there is a static section that the library loads
        // being static, it loads once regardless of which lua state is loaded first
        // static { REGISTRYINDEX = lua_registryindex(); }
        // The problem is that lua_registryindex was removed in 5.3
        // Thus, if we load JNLua from a lua5.3 state first, this static section fails
        // We must load 5.2 first, AND we know 5.3 will likely fail to load if 5.2 failed
        final boolean include52 = LuaStateFactory.include52();
        // now that JNLua has been initialized from a lua52 state, we are safe to check 5.3
        if (LuaStateFactory.include53()) {
            li.cil.oc.api.Machine.add(NativeLua53Architecture.class);
        }
        if (include52) {
            li.cil.oc.api.Machine.add(NativeLua52Architecture.class);
        }
        if (LuaStateFactory.includeLuaJ()) {
            li.cil.oc.api.Machine.add(LuaJLuaArchitecture.class);
        }

        if (Settings.get().forceLuaJ) {
            li.cil.oc.api.Machine.LuaArchitecture = LuaJLuaArchitecture.class;
        } else {
            final java.util.Iterator<Class<? extends Architecture>> it = li.cil.oc.api.Machine.architectures().iterator();
            li.cil.oc.api.Machine.LuaArchitecture = it.hasNext() ? it.next() : null;
        }

        // IMC messages (formerly Forge InterModComms) are delivered directly from now on.
        li.cil.oc.api.API.imc = IMC.INSTANCE;
        li.cil.oc.api.IMC.processPending();
    }

    public void init() {
        // Registries are populated by now.
        li.cil.oc.api.CreativeTab.instance = CreativeTab.TAB.get();

        Loot.init();
        Achievement.init();

        OpenComputers.log.debug("Initializing mod integration.");
        Mods.init();

        // TODO(port): Forge capabilities (Capabilities.init()) are gone; OC's own capabilities
        //  are plain instanceof checks now (see PORTING.md).

        li.cil.oc.api.API.isPowerEnabled = !Settings.get().ignorePower;
    }

    public void postInit() {
        // SERVER_BEFORE_START fires for every server start in a client session; only the first counts.
        if (postInitDone) return;
        postInitDone = true;
        // Don't allow driver registration after this point, to avoid issues.
        Registry.INSTANCE.locked = true;
    }

    /**
     * Physical client only. Subclasses (client.Proxy) must call {@code super.initClient()}.
     */
    public void initClient() {
        EventHandler.registerClient();
    }

    public void registerModel(Item instance, String id) {
    }

    public void registerModel(Block instance, String id) {
    }

    // Yes, this could be boiled down even further, but I like to keep it
    // explicit like this, because it makes it a) clearer, b) easier to
    // extend, in case that should ever be needed.

    // TODO(port): Forge RegistryEvent.MissingMappings has no Architectury equivalent; these
    //  1.12-era renames are not applied anymore (Fabric has no remapping hook either).

    // Example usage: OpenComputers.ID + ":rack" -> "serverRack"
    protected static final Map<String, String> blockRenames = Map.of(
            OpenComputers.ID + ":serverRack", Constants.BlockName.Rack // Yay, full circle >_>
    );

    // Example usage: OpenComputers.ID + ":tabletCase" -> "tabletCase1"
    protected static final Map<String, String> itemRenames = Map.of(
            OpenComputers.ID + ":dataCard", Constants.ItemName.DataCardTier1,
            OpenComputers.ID + ":serverRack", Constants.BlockName.Rack,
            OpenComputers.ID + ":wlanCard", Constants.ItemName.WirelessNetworkCardTier2
    );
}
