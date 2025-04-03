package li.cil.oc.common.container

import li.cil.oc.common.{Tier, tileentity}
import li.cil.oc.integration.util.ItemCharge
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

class Charger(selfType: MenuType[_ <: Charger], id: Int, playerInventory:Inventory, charger: Container)
  extends Player(selfType, id, playerInventory, charger) {

  override protected def getHostClass = classOf[tileentity.Charger]

  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 80, 35, getHostClass, "tablet", Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (!container.canPlaceItem(getSlotIndex, stack)) return false
      ItemCharge.canCharge(stack)
    }
  })
  addPlayerInventorySlots(8, 84)
}
