package li.cil.oc.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import li.cil.oc.Constants;
import li.cil.oc.CreativeTab;
import li.cil.oc.client.gui.GuiTypes;
import li.cil.oc.client.renderer.ClientRenderers;
import li.cil.oc.util.Audio;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client side proxy. Instantiated reflectively by {@link li.cil.oc.OpenComputers#proxy()}.
 */
public class Proxy extends li.cil.oc.common.Proxy {
    @Override
    public void preInit() {
        super.preInit();

        li.cil.oc.api.API.manual = Manual.INSTANCE;
    }

    @Override
    public void init() {
        super.init();

        li.cil.oc.common.PacketHandler.clientHandler = PacketHandler.INSTANCE;
    }

    @Override
    public void postInit() {
        super.postInit();
    }

    /**
     * Called during mod construction on the physical client (after {@link #preInit()}).
     * Everything that Architectury forwards to Forge's registration events (key
     * mappings, color handlers, renderers) has to be registered from here.
     */
    @Override
    public void initClient() {
        super.initClient();

        CreativeTab.TAB.listen(tab -> li.cil.oc.api.CreativeTab.instance = tab);

        Audio.init();
        Sound.init();

        PacketHandler.registerClientReceiver();

        KeyMappingRegistry.register(KeyBindings.extendedTooltip);
        KeyMappingRegistry.register(KeyBindings.analyzeCopyAddr);
        KeyMappingRegistry.register(KeyBindings.clipboardPaste);

        ColorHandler.init();

        // Block entity / entity renderers, models and client event handlers of the renderer package.
        ClientRenderers.register();

        li.cil.oc.client.event.NanomachinesHandlerClient.register();
        li.cil.oc.client.event.RackMountableRenderHandler.register();
        // TODO(port): registerModel(Item/Block, id) overrides (ModelInitialization) are gone.

        onClientSetup(Proxy::clientSetup);
    }

    /**
     * Runs {@code task} once on Architectury's {@code CLIENT_SETUP}. On Fabric, Architectury fires
     * that event from its own client entrypoint, so whether a listener registered from our client
     * entrypoint sees it depends on the entrypoint order (newer Fabric Loaders, e.g. 0.19 as required
     * by JEI 15.62, run Architectury's first). {@code CLIENT_STARTED} is the fallback for that case.
     */
    public static void onClientSetup(Runnable task) {
        final AtomicBoolean done = new AtomicBoolean(false);
        final Runnable once = () -> {
            if (done.compareAndSet(false, true)) task.run();
        };
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> once.run());
        ClientLifecycleEvent.CLIENT_STARTED.register(minecraft -> once.run());
    }

    private static void clientSetup() {
        li.cil.oc.api.CreativeTab.instance = CreativeTab.TAB.get();

        // TODO(port): Forge's FMLClientSetupEvent required enqueueWork for screen registration
        //  (MenuScreens.register is not thread-safe); Architectury runs this on the setup thread.
        GuiTypes.register();

        RenderTypeRegistry.register(RenderType.cutout(),
                block(Constants.BlockName.Keyboard),
                block(Constants.BlockName.Print),
                block(Constants.BlockName.Cable),
                block(Constants.BlockName.NetSplitter));
    }

    private static Block block(String name) {
        return li.cil.oc.api.Items.get(name).block();
    }
}
