package li.cil.oc.common.inventory

import li.cil.oc.api.Driver
import li.cil.oc.common.container.{ContainerTypes, DiskDrive => DiskDriveContainer}
import li.cil.oc.common.{Slot, tileentity}
import net.minecraft.network.chat.Component
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.item.ItemStack

trait DiskDriveMountableInventory extends ItemStackInventory with MenuProvider {
  def tier: Int = 1

  override def getContainerSize = 1

  override protected def inventoryName = "diskdrive"

  override def getMaxStackSize = 1

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, classOf[tileentity.DiskDrive]))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Floppy
    case _ => false
  }

  override def getDisplayName = Component.empty()

  override def createMenu(id: Int, playerInventory:Inventory, player: Player) =
    new DiskDriveContainer(ContainerTypes.DISK_DRIVE, id, playerInventory, this)
}
