package li.cil.oc.common

import java.util.function.Supplier
import com.google.common.base.Strings
import li.cil.oc._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.entity.EntityTypes
import li.cil.oc.common.init.Blocks
import li.cil.oc.common.init.Items
import li.cil.oc.common.tileentity.BlockEntityTypes
import li.cil.oc.common.recipe.RecipeSerializers
import li.cil.oc.integration.Mods
import li.cil.oc.server
import li.cil.oc.server._
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.server.machine.luac.{LuaStateFactory, NativeLua52Architecture, NativeLua53Architecture, NativeLua54Architecture}
import li.cil.oc.server.machine.luaj.LuaJLuaArchitecture
import net.minecraft.world.level.block.Block
import net.minecraft.world.item.Item
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.{FMLCommonSetupEvent, FMLLoadCompleteEvent, InterModProcessEvent}
import net.minecraftforge.network.{NetworkEvent, NetworkRegistry}

import scala.jdk.CollectionConverters._

class Proxy {
  protected val modBus = MinecraftForge.EVENT_BUS
  protected val modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus
  modEventBus.register(classOf[ContainerTypes])
  modEventBus.register(classOf[EntityTypes])
  modEventBus.register(classOf[BlockEntityTypes])
  modEventBus.register(classOf[RecipeSerializers])
  LootFunctions.init(modEventBus)

  def preInit() {
    OpenComputers.log.info("Initializing OpenComputers API.")

    api.CreativeTab.instance = CreativeTab
    api.API.driver = driver.Registry
    api.API.fileSystem = fs.FileSystem
    api.API.items = Items
    api.API.machine = machine.Machine
    api.API.nanomachines = nanomachines.Nanomachines
    api.API.network = network.Network

    api.API.config = Settings.get.config

    if (LuaStateFactory.isAvailable) {
      if (LuaStateFactory.include53) {
        api.Machine.add(classOf[NativeLua53Architecture])
      }
      if (LuaStateFactory.include54) {
        api.Machine.add(classOf[NativeLua54Architecture])
      }
      if (LuaStateFactory.include52) {
        api.Machine.add(classOf[NativeLua52Architecture])
      }
    }
    if (LuaStateFactory.includeLuaJ) {
      api.Machine.add(classOf[LuaJLuaArchitecture])
    }
    
    api.Machine.LuaArchitecture =
      if (Settings.get.forceLuaJ) classOf[LuaJLuaArchitecture]
      else api.Machine.architectures.asScala.head
  }

  @SubscribeEvent
  def init(e: FMLCommonSetupEvent) {
    e.enqueueWork((() => {
      OpenComputers.channel = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "net_main"), () => "", "".equals(_), "".equals(_))
      OpenComputers.channel.registerMessage(0, classOf[Array[Byte]],
        (msg: Array[Byte], buff: FriendlyByteBuf) => buff.writeByteArray(msg), _.readByteArray(),
        (msg: Array[Byte], ctx: Supplier[NetworkEvent.Context]) => {
          val context = ctx.get
          context.enqueueWork(() => CommonPacketHandler.handlePacket(context.getDirection, msg, context.getSender))
          context.setPacketHandled(true)
        })
      CommonPacketHandler.serverHandler = server.PacketHandler

      Loot.init()
      Achievement.init()

      OpenComputers.log.debug("Initializing mod integration.")
      Mods.init()

      OpenComputers.log.info("Initializing capabilities.")

      api.API.isPowerEnabled = !Settings.get.ignorePower
    }): Runnable)
  }

  @SubscribeEvent
  def register(event: RegisterCapabilitiesEvent): Unit = {
    Capabilities.init(event)
  }

  @SubscribeEvent
  def postInit(e: FMLLoadCompleteEvent) {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true
  }

  def registerModel(instance: Item, id: String): Unit = {}

  def registerModel(instance: Block, id: String): Unit = {}
}
