package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.Container
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.{MenuType, Slot => BaseSlot}

class Printer(selfType: MenuType[_ <: Printer], id: Int, playerInventory: Inventory, val printer: Container)
  extends Player(selfType, id, playerInventory, printer) {

  override protected def getHostClass = classOf[tileentity.Printer]

  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 18, 19, getHostClass, Slot.Filtered, Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (!container.canPlaceItem(getSlotIndex, stack)) return false
      PrintData.materialValue(stack) > 0
    }
  })
  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 18, 51, getHostClass, Slot.Filtered, Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (!container.canPlaceItem(getSlotIndex, stack)) return false
      PrintData.inkValue(stack) > 0
    }
  })
  addSlot(new BaseSlot(otherInventory, slots.size, 152, 35) {
    override def mayPlace(stack: ItemStack): Boolean = false
  })

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  def progress = synchronizedData.getDouble("progress")

  def maxAmountMaterial = synchronizedData.getInt("maxAmountMaterial")

  def amountMaterial = synchronizedData.getInt("amountMaterial")

  def maxAmountInk = synchronizedData.getInt("maxAmountInk")

  def amountInk = synchronizedData.getInt("amountInk")

  override protected def detectCustomDataChanges(nbt: CompoundTag): Unit = {
    printer match {
      case te: tileentity.Printer => {
        synchronizedData.putDouble("progress", if (te.isPrinting) te.progress / 100.0 else 0)
        synchronizedData.putInt("maxAmountMaterial", te.maxAmountMaterial)
        synchronizedData.putInt("amountMaterial", te.amountMaterial)
        synchronizedData.putInt("maxAmountInk", te.maxAmountInk)
        synchronizedData.putInt("amountInk", te.amountInk)
      }
      case _ =>
    }
    super.detectCustomDataChanges(nbt)
  }
}
