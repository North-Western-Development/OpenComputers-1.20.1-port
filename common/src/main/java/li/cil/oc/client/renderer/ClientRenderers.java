package li.cil.oc.client.renderer;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.client.platform.RenderPlatform;
import li.cil.oc.client.renderer.block.ModelInitialization;
import li.cil.oc.client.renderer.entity.DroneRenderer;
import li.cil.oc.client.renderer.item.DroneItemRenderer;
import li.cil.oc.client.renderer.item.HoverBootRenderer;
import li.cil.oc.client.renderer.tileentity.AdapterRenderer;
import li.cil.oc.client.renderer.tileentity.AssemblerRenderer;
import li.cil.oc.client.renderer.tileentity.CaseRenderer;
import li.cil.oc.client.renderer.tileentity.ChargerRenderer;
import li.cil.oc.client.renderer.tileentity.DisassemblerRenderer;
import li.cil.oc.client.renderer.tileentity.DiskDriveRenderer;
import li.cil.oc.client.renderer.tileentity.GeolyzerRenderer;
import li.cil.oc.client.renderer.tileentity.HologramRenderer;
import li.cil.oc.client.renderer.tileentity.MicrocontrollerRenderer;
import li.cil.oc.client.renderer.tileentity.NetSplitterRenderer;
import li.cil.oc.client.renderer.tileentity.PowerDistributorRenderer;
import li.cil.oc.client.renderer.tileentity.PrinterRenderer;
import li.cil.oc.client.renderer.tileentity.RackRenderer;
import li.cil.oc.client.renderer.tileentity.RaidRenderer;
import li.cil.oc.client.renderer.tileentity.RelayRenderer;
import li.cil.oc.client.renderer.tileentity.RobotRenderer;
import li.cil.oc.client.renderer.tileentity.ScreenRenderer;
import li.cil.oc.client.renderer.tileentity.TransposerRenderer;
import li.cil.oc.common.entity.EntityTypes;
import li.cil.oc.common.item.Drone;
import li.cil.oc.common.item.HoverBoots;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Registration of everything rendering related (replaces the renderer parts
 * of the old client proxy).
 * <p>
 * {@link #register()} must be called once during client initialization
 * ({@code OpenComputers.initClient()} / client proxy), i.e. during mod
 * construction, before resources are loaded. Registrations that need
 * registry contents (block entity renderers, item renderers) are deferred to
 * Architectury's {@link ClientLifecycleEvent#CLIENT_SETUP}.
 */
public final class ClientRenderers {
    private ClientRenderers() {
    }

    private static boolean registered = false;

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // Code generated block/item models.
        ModelInitialization.init();

        // World overlays.
        RenderPlatform.registerLevelRenderer(PetRenderer::onRenderLevel);
        RenderPlatform.registerLevelRenderer(HologramRenderer::onRenderLevelLast);
        RenderPlatform.registerLevelRenderer(MFUTargetRenderer::onRenderWorldLastEvent);
        RenderPlatform.registerLevelRenderer(WirelessNetworkDebugRenderer::onRenderWorldLastEvent);
        RenderPlatform.registerBlockHighlightRenderer(HighlightRenderer::onDrawBlockHighlight);

        EventBus.INSTANCE.register(RobotRenderEvent.class, PetRenderer::onRobotRender);

        ClientTickEvent.CLIENT_PRE.register(minecraft -> {
            TextBufferRenderCache.onTick();
            HologramRenderer.onTick();
            PetRenderer.tickStart();
        });

        // Entities.
        EntityRendererRegistry.register(EntityTypes.DRONE, DroneRenderer::new);

        // Client hooks read by the (Forge) item classes.
        Drone.customRenderer = DroneItemRenderer::instance;
        HoverBoots.armorModel = stack -> {
            HoverBootRenderer.INSTANCE.lightColor = ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : 0x66DD55;
            return HoverBootRenderer.INSTANCE;
        };

        li.cil.oc.client.Proxy.onClientSetup(() -> {
            registerBlockEntityRenderers();
            registerItemRenderers();
        });
    }

    public static void registerBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(TileEntityTypes.ADAPTER.get(), AdapterRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.ASSEMBLER.get(), AssemblerRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.CASE.get(), CaseRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.CHARGER.get(), ChargerRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.DISASSEMBLER.get(), DisassemblerRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.DISK_DRIVE.get(), DiskDriveRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.GEOLYZER.get(), GeolyzerRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.HOLOGRAM.get(), HologramRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.MICROCONTROLLER.get(), MicrocontrollerRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.NET_SPLITTER.get(), NetSplitterRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.POWER_DISTRIBUTOR.get(), PowerDistributorRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.PRINTER.get(), PrinterRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.RAID.get(), RaidRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.RACK.get(), RackRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.RELAY.get(), RelayRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.ROBOT.get(), RobotRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.SCREEN.get(), ScreenRenderer::new);
        BlockEntityRendererRegistry.register(TileEntityTypes.TRANSPOSER.get(), TransposerRenderer::new);
    }

    public static void registerItemRenderers() {
        // Fabric only; on Forge the item classes provide these via IClientItemExtensions.
        final ItemInfo drone = Items.get(Constants.ItemName.Drone);
        if (drone != null && drone.item() != null) {
            RenderPlatform.registerItemRenderer(drone.item(), DroneItemRenderer::render);
        } else {
            OpenComputers.log.warn("Drone item not found, cannot register its renderer.");
        }

        final ItemInfo hoverBoots = Items.get(Constants.ItemName.HoverBoots);
        if (hoverBoots != null && hoverBoots.item() != null) {
            RenderPlatform.registerArmorModel(hoverBoots.item(), (entity, stack, slot) -> {
                if (slot != EquipmentSlot.FEET) return null;
                HoverBootRenderer.INSTANCE.lightColor = ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : 0x66DD55;
                return HoverBootRenderer.INSTANCE;
            }, HoverBoots.ARMOR_TEXTURE);
        }
    }
}
