package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.{AbstractContainerMenu, MenuType}

class Adapter(selfType: MenuType[_ <: Adapter], id: Int, playerInventory: Inventory, adapter: Container)
  extends Player(selfType, id, playerInventory, adapter) {

  override protected def getHostClass = classOf[tileentity.Adapter]

  addSlotToContainer(80, 35, Slot.Upgrade)
  addPlayerInventorySlots(8, 84)
}
