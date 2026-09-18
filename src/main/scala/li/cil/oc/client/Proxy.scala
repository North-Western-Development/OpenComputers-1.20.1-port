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
import net.minecraftforge.client.ClientRegistry
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

  override def init(e: FMLCommonSetupEvent) {
    super.init(e)

    CommonPacketHandler.clientHandler = PacketHandler

    e.enqueueWork((() => {
      ModelInitialization.preInit()

      ColorHandler.init()

      ClientRegistry.registerKeyBinding(KeyBindings.extendedTooltip)
      ClientRegistry.registerKeyBinding(KeyBindings.analyzeCopyAddr)
      ClientRegistry.registerKeyBinding(KeyBindings.clipboardPaste)

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
      EntityTypes.DRONE,
      ctx => new DroneRenderer(ctx)
    )

    // Block entity renderers
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.ADAPTER,           AdapterRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.ASSEMBLER,         AssemblerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.CASE,              CaseRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.CHARGER,           ChargerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.DISASSEMBLER,      DisassemblerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.DISK_DRIVE,        DiskDriveRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.GEOLYZER,          GeolyzerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.HOLOGRAM,          HologramRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.MICROCONTROLLER,   MicrocontrollerRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.NET_SPLITTER,      NetSplitterRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.POWER_DISTRIBUTOR, PowerDistributorRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.PRINTER,           PrinterRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.RAID,              RaidRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.RACK,              RackRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.RELAY,             RelayRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.ROBOT,             RobotRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.SCREEN,            ScreenRenderer.apply)
    event.registerBlockEntityRenderer(tileentity.BlockEntityTypes.TRANSPOSER,        TransposerRenderer.apply)
  }

  @SubscribeEvent
  def onRegisterLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions): Unit = {
    event.registerLayerDefinition(DroneObject.DRONE_MODEL_LAYER, () => ModelQuadcopter.createBodyLayer)
  }

  override def registerModel(instance: Item, id: String): Unit = ModelInitialization.registerModel(instance, id)
}
