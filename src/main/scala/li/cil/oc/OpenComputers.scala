package li.cil.oc

import java.nio.file.Paths
import li.cil.oc.common.IMC
import li.cil.oc.common.Proxy
import li.cil.oc.common.init.Blocks
import li.cil.oc.common.init.Items
import li.cil.oc.integration.Mods
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.world.level.block.Block
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.forgespi.Environment
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.network.simple.SimpleChannel
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import scala.collection.convert.ImplicitConversionsToScala._

object OpenComputers {
  final val ID = "opencomputers"

  final val Name = "OpenComputers"

  final val McVersion = "@MCVERSION@-forge"

  final val Version = "@VERSION@"

  final val log: Logger = LogManager.getLogger(Name)

  lazy val proxy: Proxy = {
    val cls = Environment.get.getDist match {
      case Dist.CLIENT => Class.forName("li.cil.oc.client.Proxy")
      case _ => Class.forName("li.cil.oc.common.Proxy")
    }
    cls.getConstructor().newInstance().asInstanceOf[Proxy]
  }

  var channel: SimpleChannel = null

  private var instance: Option[OpenComputers] = None

  def get = instance match {
    case Some(oc) => oc
    case _ => throw new IllegalStateException("not initialized")
  }
}

class OpenComputers {
  val modContainer: ModContainer = ModLoadingContext.get.getActiveContainer

  OpenComputers.instance = Some(this)

  MinecraftForge.EVENT_BUS.register(OpenComputers.proxy)
  Settings.load(FMLPaths.CONFIGDIR.get().resolve(Paths.get("opencomputers", "settings.conf")).toFile())
  OpenComputers.proxy.preInit()
  MinecraftForge.EVENT_BUS.register(ThreadPoolFactory)
  Mods.preInit() // Must happen after loading Settings but before registry events are fired.

  @SubscribeEvent
  def registerBlocks(e: RegistryEvent.Register[Block]): Unit = {
    Blocks.init()
  }

  @SubscribeEvent
  def registerItems(e: RegistryEvent.Register[Item]): Unit = {
    Items.init()
  }

  @SubscribeEvent
  def imc(e: InterModProcessEvent): Unit = {
    // Technically requires synchronization because IMC.sendTo doesn't check the loading stage.
    e.enqueueWork((() => {
      InterModComms.getMessages(OpenComputers.ID).sequential.iterator.foreach(IMC.handleMessage)
    }): Runnable)
  }
}
