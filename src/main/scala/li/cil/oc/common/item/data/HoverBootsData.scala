package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag

class HoverBootsData extends ItemData(Constants.ItemName.HoverBoots) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  var charge = 0.0

  private final val ChargeTag = Settings.namespace + "charge"

  override def loadData(nbt: CompoundTag) {
    charge = nbt.getDouble(ChargeTag)
  }

  override def saveData(nbt: CompoundTag) {
    nbt.putDouble(ChargeTag, charge)
  }
}
