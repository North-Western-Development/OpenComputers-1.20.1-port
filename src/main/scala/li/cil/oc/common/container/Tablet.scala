package li.cil.oc.common.container

import li.cil.oc.common.item.TabletWrapper
import li.cil.oc.integration.opencomputers.DriverScreen
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.Container
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

class Tablet(selfType: MenuType[_ <: Tablet], id: Int, playerInventory: Inventory, val stack: ItemStack, tablet: Container, slot1: String, tier1: Int)
  extends li.cil.oc.common.container.Player(selfType, id, playerInventory, tablet) {

  override protected def getHostClass = classOf[TabletWrapper]

  addSlot(new StaticComponentSlot(this, otherInventory, otherInventory.getContainerSize - 1, 80, 35, getHostClass, slot1, tier1) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (DriverScreen.worksWith(stack, getHostClass)) return false
      super.mayPlace(stack)
    }
  })

  addPlayerInventorySlots(8, 84)

  override def stillValid(player: Player) = player == playerInventory.player
}
