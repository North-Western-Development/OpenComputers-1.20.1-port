package li.cil.oc.common.tileentity.traits

import java.util.function.Consumer

import li.cil.oc.common.inventory
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component

trait Inventory extends TileEntity with inventory.Inventory {
  private lazy val inventory = Array.fill[ItemStack](getContainerSize)(ItemStack.EMPTY)

  def items = inventory

  // ----------------------------------------------------------------------- //

  override def getDisplayName: Component = super[Inventory].getDisplayName

  override def loadForServer(nbt: CompoundTag) {
    super.loadForServer(nbt)
    loadData(nbt)
  }

  override def saveForServer(nbt: CompoundTag) {
    super.saveForServer(nbt)
    saveData(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def stillValid(player: Player) =
    player.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) <= 64

  // ----------------------------------------------------------------------- //

  def forAllLoot(dst: Consumer[ItemStack]): Unit = InventoryUtils.forAllSlots(this, dst)

  def dropSlot(slot: Int, count: Int = getMaxStackSize, direction: Option[Direction] = None) =
    InventoryUtils.dropSlot(BlockPosition(x, y, z, getLevel), this, slot, count, direction)

  def dropAllSlots() =
    InventoryUtils.dropAllSlots(BlockPosition(x, y, z, getLevel), this)

  def spawnStackInWorld(stack: ItemStack, direction: Option[Direction] = None) =
    InventoryUtils.spawnStackInWorld(BlockPosition(x, y, z, getLevel), stack, direction)
}
