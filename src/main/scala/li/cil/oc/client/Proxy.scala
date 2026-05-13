package li.cil.oc.client

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.client
import li.cil.oc.client.gui.GuiTypes
import li.cil.oc.client.renderer.entity.ModelQuadcopter
import li.cil.oc.common.entity.DroneObject
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
//import li.cil.oc.client.renderer.HighlightRenderer
import li.cil.oc.client.renderer.MFUTargetRenderer
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.ModelInitialization
import li.cil.oc.client.renderer.block.NetSplitterModel
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.entity.EntityTypes
import li.cil.oc.common.event.NanomachinesHandler
import li.cil.oc.common.event.RackMountableRenderHandler
import li.cil.oc.common.tileentity
import li.cil.oc.util.Audio
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context
import net.minecraft.world.level.block.Block
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererProvider, EntityRenderers}
import net.minecraft.world.item.Item
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent

private[oc] class Proxy extends CommonProxy {
  modEventBus.register(classOf[GuiTypes])
  modEventBus.register(ModelInitialization)
  modEventBus.register(NetSplitterModel)
  modEventBus.register(Textures)

  modEventBus.register(this)

  override def preInit() {
    super.preInit()

    api.API.manual = client.Manual
  }

  @SubscribeEvent
  def registerBindings(e:RegisterKeyMappingsEvent): Unit = {
    e.register(KeyBindings.extendedTooltip);
    e.register(KeyBindings.analyzeCopyAddr);
    e.register(KeyBindings.clipboardPaste);
  }

  override def init(e: FMLCommonSetupEvent) {
    super.init(e)

    CommonPacketHandler.clientHandler = PacketHandler

    e.enqueueWork((() => {
      ModelInitialization.preInit()

      ColorHandler.init()

      //MinecraftForge.EVENT_BUS.register(HighlightRenderer) SEE: HighlightRenderer.scala for reason
      MinecraftForge.EVENT_BUS.register(NanomachinesHandler.Client)
      MinecraftForge.EVENT_BUS.register(PetRenderer)
      MinecraftForge.EVENT_BUS.register(RackMountableRenderHandler)
      MinecraftForge.EVENT_BUS.register(Sound)
      MinecraftForge.EVENT_BUS.register(TextBuffer)
      MinecraftForge.EVENT_BUS.register(MFUTargetRenderer)
      MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
      MinecraftForge.EVENT_BUS.register(Audio)
      MinecraftForge.EVENT_BUS.register(HologramRenderer)
    }): Runnable)

    RenderSystem.recordRenderCall(() => MinecraftForge.EVENT_BUS.register(TextBufferRenderCache))
  }

  @SubscribeEvent
  def onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers): Unit = {
    // Entity renderer
    event.registerEntityRenderer[Drone](
      EntityTypes.DRONE.get(),
      ctx => new DroneRenderer(ctx)
    )

    // Block entity renderers
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.ADAPTER.get(), AdapterRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.ASSEMBLER.get(), AssemblerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.CASE.get(), CaseRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.CHARGER.get(), ChargerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.DISASSEMBLER.get(), DisassemblerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.DISK_DRIVE.get(), DiskDriveRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.GEOLYZER.get(), GeolyzerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.HOLOGRAM.get(), HologramRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.MICROCONTROLLER.get(), MicrocontrollerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.NET_SPLITTER.get(), NetSplitterRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.POWER_DISTRIBUTOR.get(), PowerDistributorRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.PRINTER.get(), PrinterRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.RAID.get(), RaidRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.RACK.get(), RackRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.RELAY.get(), RelayRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.ROBOT.get(), RobotRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.SCREEN.get(), ScreenRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.TRANSPOSER.get(), TransposerRenderer.apply)
  }

  @SubscribeEvent
  def onRegisterLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions): Unit = {
    event.registerLayerDefinition(DroneObject.DRONE_MODEL_LAYER, () => ModelQuadcopter.createBodyLayer)
  }

  override def registerModel(instance: Item, id: String): Unit = ModelInitialization.registerModel(instance, id)
}
