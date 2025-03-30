package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.server.component.DebugCard.AccessContext
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag

class DebugCardData extends ItemData(Constants.ItemName.DebugCard) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  var access: Option[AccessContext] = None

  private final val DataTag = Settings.namespace + "data"

  override def loadData(nbt: CompoundTag): Unit = {
    access = AccessContext.loadData(dataTag(nbt))
  }

  override def saveData(nbt: CompoundTag): Unit = {
    val tag = dataTag(nbt)
    AccessContext.remove(tag)
    access.foreach(_.saveData(tag))
  }

  private def dataTag(nbt: CompoundTag) = {
    if (!nbt.contains(DataTag)) {
      nbt.put(DataTag, new CompoundTag())
    }
    nbt.getCompound(DataTag)
  }
}
