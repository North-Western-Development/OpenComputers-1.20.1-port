package li.cil.oc.client

import java.util.Timer
import java.util.TimerTask
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.resources.sounds.{AbstractTickableSoundInstance, TickableSoundInstance}
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.{SoundEvent, SoundSource}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable

object Sound {
  private val sources = mutable.Map.empty[BlockEntity, PseudoLoopingStream]

  private val commandQueue = mutable.PriorityQueue.empty[Command]

  private val updateTimer = new Timer("OpenComputers-SoundUpdater", true)
  if (Settings.get.soundVolume > 0) {
    updateTimer.scheduleAtFixedRate(new TimerTask {
      override def run() {
        sources.synchronized(Sound.updateCallable = Some(() => processQueue()))
      }
    }, 500, 50)
  }

  private var updateCallable = None: Option[() => Unit]

  private def processQueue() {
    if (commandQueue.nonEmpty) {
      commandQueue.synchronized {
        while (commandQueue.nonEmpty && commandQueue.head.when < System.currentTimeMillis()) {
          try commandQueue.dequeue()() catch {
            case t: Throwable => OpenComputers.log.warn("Error processing sound command.", t)
          }
        }
      }
    }
  }

  def startLoop(tileEntity: BlockEntity, name: String, volume: Float = 1f, delay: Long = 0) {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StartCommand(System.currentTimeMillis() + delay, tileEntity, name, volume)
      }
    }
  }

  def stopLoop(tileEntity: BlockEntity) {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StopCommand(tileEntity)
      }
    }
  }

  def updatePosition(tileEntity: BlockEntity) {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new UpdatePositionCommand(tileEntity)
      }
    }
  }

  @SubscribeEvent
  def onTick(e: ClientTickEvent) {
    sources.synchronized {
      updateCallable.foreach(_ ())
      updateCallable = None
    }
  }

  @SubscribeEvent
  def onLevelUnload(event: WorldEvent.Unload): Unit = {
    commandQueue.synchronized(commandQueue.clear())
    sources.synchronized(try sources.foreach(_._2.doStop()) catch {
      case _: Throwable => // Ignore.
    })
    sources.clear()
  }

  private abstract class Command(val when: Long, val tileEntity: BlockEntity) extends Ordered[Command] {
    def apply(): Unit

    override def compare(that: Command) = (that.when - when).toInt
  }

  private class StartCommand(when: Long, tileEntity: BlockEntity, val name: String, val volume: Float) extends Command(when, tileEntity) {
    override def apply() {
      sources.synchronized {
        val current = sources.getOrElse(tileEntity, null)
        if (current == null || !current.getLocation.getPath.equals(name)) {
          if (current != null) current.doStop()
          sources(tileEntity) = new PseudoLoopingStream(tileEntity, volume, name)
        }
      }
    }
  }

  private class StopCommand(tileEntity: BlockEntity) extends Command(System.currentTimeMillis() + 1, tileEntity) {
    override def apply() {
      sources.synchronized {
        sources.remove(tileEntity) match {
          case Some(sound) => sound.doStop()
          case _ =>
        }
      }
      commandQueue.synchronized {
        // Remove all other commands for this tile entity from the queue. This
        // is inefficient, but we generally don't expect the command queue to
        // be very long, so this should be OK.
        commandQueue ++= commandQueue.dequeueAll.filter(_.tileEntity != tileEntity)
      }
    }
  }

  private class UpdatePositionCommand(tileEntity: BlockEntity) extends Command(System.currentTimeMillis(), tileEntity) {
    override def apply() {
      sources.synchronized {
        sources.get(tileEntity) match {
          case Some(sound) => sound.updatePosition()
          case _ =>
        }
      }
    }
  }

  private class PseudoLoopingStream(val tileEntity: BlockEntity, val subVolume: Float, name: String)
    extends AbstractTickableSoundInstance(new SoundEvent(new ResourceLocation(OpenComputers.ID, name)), SoundSource.BLOCKS) {

    var stopped = false
    volume = subVolume * Settings.get.soundVolume
    relative = tileEntity != null
    looping = true
    updatePosition()

    def updatePosition() {
      if (tileEntity != null) {
        val pos = tileEntity.getBlockPos
        x = pos.getX + 0.5
        y = pos.getY + 0.5
        z = pos.getZ + 0.5
      }
    }

    override def canStartSilent() = true

    override def isStopped() = stopped

    // Required by ITickableSound, which is required to update position while playing
    override def tick() = ()

    def doStop(): Unit = {
      stopped = true
      looping = false
      stop()
    }
  }
}
