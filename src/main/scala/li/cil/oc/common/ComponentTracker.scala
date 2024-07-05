package li.cil.oc.common

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraftforge.event.world.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.JavaConverters.asJavaIterable
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

/**
 * Keeps track of loaded components by ID. Used to send messages between
 * component representation on server and client without knowledge of their
 * containers. For now this is only used for screens / text buffer components.
 */
abstract class ComponentTracker {
  private val worlds = mutable.Map.empty[ResourceKey[Level], Cache[String, ManagedEnvironment]]

  private def components(world: Level) = {
    worlds.getOrElseUpdate(world.dimension,
      com.google.common.cache.CacheBuilder.newBuilder().
        weakValues().
        asInstanceOf[CacheBuilder[String, ManagedEnvironment]].
        build[String, ManagedEnvironment]())
  }

  def add(world: Level, address: String, component: ManagedEnvironment) {
    this.synchronized {
      components(world).put(address, component)
    }
  }

  def remove(world: Level, component: ManagedEnvironment) {
    this.synchronized {
      components(world).invalidateAll(asJavaIterable(components(world).asMap().filter(_._2 == component).keys))
      components(world).cleanUp()
    }
  }

  def get(world: Level, address: String): Option[ManagedEnvironment] = this.synchronized {
    components(world).cleanUp()
    Option(components(world).getIfPresent(address))
  }

  @SubscribeEvent
  def onLevelUnload(e: LevelEvent.Unload): Unit = e.getLevel match {
    case world: Level => clear(world)
    case _ =>
  }

  protected def clear(world: Level): Unit = this.synchronized {
    components(world).invalidateAll()
    components(world).cleanUp()
  }
}
