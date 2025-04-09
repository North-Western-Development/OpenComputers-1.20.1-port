package li.cil.oc.common.container

import li.cil.oc.common.{InventorySlots, Tier, tileentity}
import net.minecraft.world.Container
import net.minecraft.world.entity.player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.{DataSlot, MenuType}

class Case(selfType: MenuType[_ <: Case], id: Int, playerInventory:Inventory, computer: Container, tier: Int)
  extends Player(selfType, id, playerInventory, computer) {

  override protected def getHostClass = classOf[tileentity.Case]

  for (i <- 0 to (if (tier >= Tier.Three) 2 else 1)) {
    val slot = InventorySlots.computer(tier)(getItems.size)
    addSlotToContainer(98, 16 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to (if (tier == Tier.One) 0 else 1)) {
    val slot = InventorySlots.computer(tier)(getItems.size)
    addSlotToContainer(120, 16 + (i + 1) * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to (if (tier == Tier.One) 0 else 1)) {
    val slot = InventorySlots.computer(tier)(getItems.size)
    addSlotToContainer(142, 16 + i * slotSize, slot.slot, slot.tier)
  }

  if (tier >= Tier.Three) {
    val slot = InventorySlots.computer(tier)(getItems.size)
    addSlotToContainer(142, 16 + 2 * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.computer(tier)(getItems.size)
    addSlotToContainer(120, 16, slot.slot, slot.tier)
  }

  if (tier == Tier.One) {
    val slot = InventorySlots.computer(tier)(getItems.size)
    addSlotToContainer(120, 16 + 2 * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.computer(tier)(getItems.size)
    addSlotToContainer(48, 34, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  private val runningData = computer match {
    case te: tileentity.Case => {
      addDataSlot(new DataSlot {
        override def get(): Int = if (te.isRunning) 1 else 0

        override def set(value: Int): Unit = te.setRunning(value != 0)
      })
    }
    case _ => addDataSlot(DataSlot.standalone)
  }
  def isRunning = runningData.get != 0

  override def stillValid(player: player.Player) =
    super.stillValid(player) && (computer match {
      case te: tileentity.Case => te.canInteract(player.getName.getString)
      case _ => true
    })
}