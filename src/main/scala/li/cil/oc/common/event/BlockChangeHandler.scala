package li.cil.oc.common.event

import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraftforge.event.world.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.mutable

/**
  * @author Vexatos
  */
@Deprecated
object BlockChangeHandler {

  def addListener(listener: ChangeListener, coord: BlockPosition) = {
    EventHandler.scheduleServer(() => changeListeners.put(listener, coord))
  }

  def removeListener(listener: ChangeListener) = {
    EventHandler.scheduleServer(() => changeListeners.remove(listener))
  }

  private val changeListeners = mutable.WeakHashMap.empty[ChangeListener, BlockPosition]

  trait ChangeListener {
    def onBlockChanged()
  }

}
