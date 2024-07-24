package li.cil.oc.common.inventory

import li.cil.oc.Localization
import net.minecraft.network.chat.{Component, TextComponent}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.{Container, Nameable}
import net.minecraft.world.item.ItemStack

trait SimpleInventory extends Container with Nameable {
  override def hasCustomName = false

  override def getDisplayName: Component = getName

  override def getMaxStackSize = 64

  // Items required in a slot before it's set to null (for ghost stacks).
  def getInventoryStackRequired = 1

  override def startOpen(player: Player): Unit = {}

  override def stopOpen(player: Player): Unit = {}

  override def removeItem(slot: Int, amount: Int): ItemStack = {
    if (slot >= 0 && slot < getContainerSize) {
      (getItem(slot) match {
        case stack: ItemStack if stack.getCount - amount < getInventoryStackRequired =>
          setItem(slot, ItemStack.EMPTY)
          stack
        case stack: ItemStack =>
          val result = stack.split(amount)
          setChanged()
          result
        case _ => ItemStack.EMPTY
      }) match {
        case stack: ItemStack if stack.getCount > 0 => stack
        case _ => ItemStack.EMPTY
      }
    }
    else ItemStack.EMPTY
  }

  override def removeItemNoUpdate(slot: Int) = {
    if (slot >= 0 && slot < getContainerSize) {
      val stack = getItem(slot)
      setItem(slot, ItemStack.EMPTY)
      stack
    }
    else ItemStack.EMPTY
  }

  override def clearContent(): Unit = {
    for (slot <- 0 until getContainerSize) {
      setItem(slot, ItemStack.EMPTY)
    }
  }
}
