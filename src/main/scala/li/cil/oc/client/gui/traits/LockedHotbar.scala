package li.cil.oc.client.gui.traits

import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.inventory.container.ClickType
import net.minecraft.inventory.container.Container
import net.minecraft.inventory.container.Slot
import net.minecraft.world.Container
import net.minecraft.world.inventory.{ClickType, Slot}
import net.minecraft.world.item.ItemStack

trait LockedHotbar[C <: Container] extends ContainerScreen {
  def lockedStack: ItemStack
  override def slotClicked(slot: Slot, slotId: Int, mouseButton: Int, clickType: ClickType): Unit = {
    if (slot == null || !ItemStack.isSameItem(slot.getItem, lockedStack)) {
      super.slotClicked(slot, slotId, mouseButton, clickType)
    }
  }
}
