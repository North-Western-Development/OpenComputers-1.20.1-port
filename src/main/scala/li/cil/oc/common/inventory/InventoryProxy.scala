package li.cil.oc.common.inventory

import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.util.text.ITextComponent

trait InventoryProxy extends Container {
  def inventory: Container

  def offset = 0

  override def isEmpty: Boolean = inventory.isEmpty

  override def getContainerSize: Int = inventory.getContainerSize

  override def getMaxStackSize: Int = inventory.getMaxStackSize

  override def stillValid(player: Player): Boolean = inventory.stillValid(player)

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = {
    val offsetSlot = slot + offset
    isValidSlot(offsetSlot) && inventory.canPlaceItem(offsetSlot, stack)
  }

  override def getItem(slot: Int): ItemStack = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.getItem(offsetSlot)
    else ItemStack.EMPTY
  }

  override def removeItem(slot: Int, amount: Int): ItemStack = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.removeItem(offsetSlot, amount)
    else ItemStack.EMPTY
  }

  override def removeItemNoUpdate(slot: Int): ItemStack = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.removeItemNoUpdate(offsetSlot)
    else ItemStack.EMPTY
  }

  override def setItem(slot: Int, stack: ItemStack): Unit = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.setItem(offsetSlot, stack)
  }

  override def setChanged(): Unit = inventory.setChanged()

  override def startOpen(player: Player): Unit = inventory.startOpen(player)

  override def stopOpen(player: Player): Unit = inventory.stopOpen(player)

  override def clearContent(): Unit = inventory.clearContent()

  private def isValidSlot(slot: Int) = slot >= offset && slot < getContainerSize + offset
}
