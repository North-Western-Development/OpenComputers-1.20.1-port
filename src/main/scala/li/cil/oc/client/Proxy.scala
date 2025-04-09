package li.cil.oc.client

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.{api, client}
import li.cil.oc.client.gui.GuiTypes
import li.cil.oc.client.renderer._
import li.cil.oc.client.renderer.block.{ModelInitialization, NetSplitterModel}
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.EntityTypes
import li.cil.oc.common.event.{NanomachinesHandler, RackMountableRenderHandler}
import li.cil.oc.common.{tileentity, PacketHandler => CommonPacketHandler, Proxy => CommonProxy}
import li.cil.oc.util.Audio
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraftforge.client.event.{EntityRenderersEvent, RegisterKeyMappingsEvent}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, FMLCommonSetupEvent}

private[oc] class Proxy extends CommonProxy {
  modBus.register(classOf[GuiTypes])
  modBus.register(ModelInitialization)
  modBus.register(NetSplitterModel)
  modBus.register(Textures)

  override def preInit() {
    super.preInit()

    api.API.manual = client.Manual
  }

  @SubscribeEvent
  override def init(e: FMLCommonSetupEvent) {
    super.init(e)
  }

  @SubscribeEvent
  def init(e: FMLClientSetupEvent) {

    CommonPacketHandler.clientHandler = PacketHandler

    e.enqueueWork((() => {
      ModelInitialization.preInit()

      ColorHandler.init()

      MinecraftForge.EVENT_BUS.register(HighlightRenderer)
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
  def registerKeyMappingsEvent (e : RegisterKeyMappingsEvent): Unit = {
    e.register(KeyMappings.extendedTooltip)
    e.register(KeyMappings.analyzeCopyAddr)
    e.register(KeyMappings.clipboardPaste)
  }

  @SubscribeEvent
  def entityRegisterRenderers (e : EntityRenderersEvent.RegisterRenderers): Unit = {
    e.registerEntityRenderer(EntityTypes.DRONE, new DroneRenderer(_))

    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.ADAPTER.get(), AdapterRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.ASSEMBLER.get(), AssemblerRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.CASE.get(), CaseRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.CHARGER.get(), ChargerRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.DISASSEMBLER.get(), DisassemblerRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.DISK_DRIVE.get(), DiskDriveRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.GEOLYZER.get(), GeolyzerRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.HOLOGRAM.get(), HologramRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.MICROCONTROLLER.get(), MicrocontrollerRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.NET_SPLITTER.get(), NetSplitterRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.POWER_DISTRIBUTOR.get(), PowerDistributorRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.PRINTER.get(), PrinterRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.RAID.get(), RaidRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.RACK.get(), RackRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.RELAY.get(), RelayRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.ROBOT.get(), RobotRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.SCREEN.get(), ScreenRenderer)
    e.registerBlockEntityRenderer(tileentity.TileEntityTypes.TRANSPOSER.get(), TransposerRenderer)
  }

  override def registerModel(instance: Item, id: String): Unit = ModelInitialization.registerModel(instance, id)

  override def registerModel(instance: Block, id: String): Unit = ModelInitialization.registerModel(instance, id)
}
