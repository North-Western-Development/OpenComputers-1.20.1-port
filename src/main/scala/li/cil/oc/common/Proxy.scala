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
import net.minecraftforge.event.RegistryEvent.MissingMappings
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent
import net.minecraftforge.network.{NetworkEvent, NetworkRegistry}
import net.minecraftforge.registries.ForgeRegistries

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

class Proxy {
  protected val modBus = MinecraftForge.EVENT_BUS
  modBus.register(classOf[ContainerTypes])
  modBus.register(classOf[EntityTypes])
  modBus.register(classOf[BlockEntityTypes])
  modBus.register(classOf[RecipeSerializers])
  LootFunctions.init()

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
      OpenComputers.channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(OpenComputers.ID, "net_main"), () => "", "".equals(_), "".equals(_))
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
      Capabilities.init()

      api.API.isPowerEnabled = !Settings.get.ignorePower
    }): Runnable)
  }

  @SubscribeEvent
  def postInit(e: FMLLoadCompleteEvent) {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true
  }

  def registerModel(instance: Item, id: String): Unit = {}

  def registerModel(instance: Block, id: String): Unit = {}

  // Yes, this could be boiled down even further, but I like to keep it
  // explicit like this, because it makes it a) clearer, b) easier to
  // extend, in case that should ever be needed.

  // Example usage: OpenComputers.ID + ":rack" -> "serverRack"
  private val blockRenames = Map[String, String](
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack // Yay, full circle >_>
  )

  // Example usage: OpenComputers.ID + ":tabletCase" -> "tabletCase1"
  private val itemRenames = Map[String, String](
    OpenComputers.ID + ":dataCard" -> Constants.ItemName.DataCardTier1,
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack,
    OpenComputers.ID + ":wlanCard" -> Constants.ItemName.WirelessNetworkCardTier2
  )

  @SubscribeEvent
  def missingBlockMappings(e: MissingMappings[Block]) {
    for (missing <- e.getMappings(OpenComputers.ID).asScala) {
        blockRenames.get(missing.key.getPath) match {
          case Some(name) =>
            if (Strings.isNullOrEmpty(name)) missing.ignore()
            else missing.remap(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(OpenComputers.ID, name)))
          case _ => missing.warn()
        }
    }
  }

  @SubscribeEvent
  def missingItemMappings(e: MissingMappings[Item]) {
    for (missing <- e.getMappings(OpenComputers.ID).asScala) {
        itemRenames.get(missing.key.getPath) match {
          case Some(name) =>
            if (Strings.isNullOrEmpty(name)) missing.ignore()
            else missing.remap(ForgeRegistries.ITEMS.getValue(new ResourceLocation(OpenComputers.ID, name)))
          case _ => missing.warn()
        }
      }
  }
}
