package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType

class DiskDrive(selfType: MenuType[_ <: DiskDrive], id: Int, playerInventory: Inventory, drive: Container)
  extends Player(selfType, id, playerInventory, drive) {

  override protected def getHostClass = classOf[tileentity.DiskDrive]

  addSlotToContainer(80, 35, Slot.Floppy)
  addPlayerInventorySlots(8, 84)
}
