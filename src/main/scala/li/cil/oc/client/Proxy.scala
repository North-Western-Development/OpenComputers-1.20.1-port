package li.cil.oc.client

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.client
import li.cil.oc.client.gui.GuiTypes
import li.cil.oc.client.renderer.HighlightRenderer
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
  modBus.register(classOf[GuiTypes])
  modBus.register(ModelInitialization)
  modBus.register(NetSplitterModel)
  modBus.register(Textures)

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

      EntityRenderers.register(EntityTypes.DRONE, new EntityRendererProvider[Drone] {
        override def create(manager: Context): EntityRenderer[_ >: Drone] = new DroneRenderer(manager)
      })
      
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.ADAPTER, AdapterRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.ASSEMBLER, AssemblerRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.CASE, CaseRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.CHARGER, ChargerRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.DISASSEMBLER, DisassemblerRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.DISK_DRIVE, DiskDriveRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.GEOLYZER, GeolyzerRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.HOLOGRAM, HologramRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.MICROCONTROLLER, MicrocontrollerRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.NET_SPLITTER, NetSplitterRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.POWER_DISTRIBUTOR, PowerDistributorRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.PRINTER, PrinterRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.RAID, RaidRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.RACK, RackRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.RELAY, RelayRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.ROBOT, RobotRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.SCREEN, ScreenRenderer.apply)
      BlockEntityRenderers.register(tileentity.BlockEntityTypes.TRANSPOSER, TransposerRenderer.apply)

      ClientRegistry.registerKeyBinding(KeyBindings.extendedTooltip)
      ClientRegistry.registerKeyBinding(KeyBindings.analyzeCopyAddr)
      ClientRegistry.registerKeyBinding(KeyBindings.clipboardPaste)

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

  override def registerModel(instance: Item, id: String): Unit = ModelInitialization.registerModel(instance, id)

  //override def registerModel(instance: Block, id: String): Unit = ModelInitialization.registerModel(instance, id)
}
