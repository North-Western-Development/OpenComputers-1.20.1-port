package li.cil.oc.common.container

import li.cil.oc.common.InventorySlots
import li.cil.oc.server.component
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.inventory.{AbstractContainerMenu, MenuType}

class Server(selfType: MenuType[_ <: Server], id: Int, playerInventory: Inventory, val stack: ItemStack, serverInventory: Container, tier: Int, val rackSlot: Int)
  extends li.cil.oc.common.container.Player(selfType, id, playerInventory, serverInventory) {

  override protected def getHostClass = classOf[component.Server]

  for (i <- 0 to 1) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  val verticalSlots = math.min(3, 1 + tier)
  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(100, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(124, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(148, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 2 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(26, 34, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def stillValid(player: Player) = {
    otherInventory match {
      case _: component.Server => super.stillValid(player)
      case _ => player == playerInventory.player
    }
  }

  var isRunning = false
  var isItem = true

  override def updateCustomData(nbt: CompoundTag): Unit = {
    super.updateCustomData(nbt)
    isRunning = nbt.getBoolean("isRunning")
    isItem = nbt.getBoolean("isItem")
  }

  override protected def detectCustomDataChanges(nbt: CompoundTag): Unit = {
    super.detectCustomDataChanges(nbt)
    otherInventory match {
      case s: component.Server => nbt.putBoolean("isRunning", s.machine.isRunning)
      case _ => nbt.putBoolean("isItem", true)
    }
  }
}