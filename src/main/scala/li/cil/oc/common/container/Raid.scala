package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType

class Raid(selfType: MenuType[_ <: Raid], id: Int, playerInventory: Inventory, raid: Container)
  extends Player(selfType, id, playerInventory, raid) {

  override protected def getHostClass = classOf[tileentity.Raid]

  addSlotToContainer(60, 23, Slot.HDD, Tier.Three)
  addSlotToContainer(80, 23, Slot.HDD, Tier.Three)
  addSlotToContainer(100, 23, Slot.HDD, Tier.Three)
  addPlayerInventorySlots(8, 84)
}
