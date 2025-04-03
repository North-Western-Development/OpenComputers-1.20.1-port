package li.cil.oc.util

import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

import scala.collection.mutable
import scala.language.implicitConversions

object ExtendedInventory {

  implicit def extendedInventory(inventory: Container): ExtendedInventory = new ExtendedInventory(inventory)

  class ExtendedInventory(val inventory: Container) extends mutable.IndexedSeq[ItemStack] {
    override def length = inventory.getContainerSize

    override def update(idx: Int, elem: ItemStack) = inventory.setItem(idx, elem)

    override def apply(idx: Int) = inventory.getItem(idx)
  }

}
