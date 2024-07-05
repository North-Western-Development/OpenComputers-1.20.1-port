package li.cil.oc.client

import com.mojang.blaze3d.systems.IRenderCall
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
import net.minecraft.world.level.block.Block
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererManager}
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.{ClientRegistry, IRenderFactory, RenderingRegistry}
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.network.NetworkRegistry

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

      RenderingRegistry.registerEntityRenderingHandler(EntityTypes.DRONE, new IRenderFactory[Drone] {
        override def createRenderFor(manager: EntityRendererManager): EntityRenderer[_ >: Drone] = new DroneRenderer(manager)
      })

      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.ADAPTER, AdapterRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.ASSEMBLER, AssemblerRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.CASE, CaseRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.CHARGER, ChargerRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.DISASSEMBLER, DisassemblerRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.DISK_DRIVE, DiskDriveRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.GEOLYZER, GeolyzerRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.HOLOGRAM, HologramRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.MICROCONTROLLER, MicrocontrollerRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.NET_SPLITTER, NetSplitterRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.POWER_DISTRIBUTOR, PowerDistributorRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.PRINTER, PrinterRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.RAID, RaidRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.RACK, RackRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.RELAY, RelayRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.ROBOT, RobotRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.SCREEN, ScreenRenderer)
      ClientRegistry.bindBlockEntityRenderer(tileentity.BlockEntityTypes.TRANSPOSER, TransposerRenderer)

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

  override def registerModel(instance: Block, id: String): Unit = ModelInitialization.registerModel(instance, id)
}
