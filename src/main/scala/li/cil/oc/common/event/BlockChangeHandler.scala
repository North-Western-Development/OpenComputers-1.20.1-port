package li.cil.oc.common.event

import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition

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
