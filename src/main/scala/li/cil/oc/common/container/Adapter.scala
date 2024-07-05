package li.cil.oc.common.container

import li.cil.oc.api.Driver
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.Container
import net.minecraft.inventory.container.ContainerType

class Adapter(selfType: ContainerType[_ <: Adapter], id: Int, playerInventory: PlayerInventory, adapter: Container)
  extends Player(selfType, id, playerInventory, adapter) {

  override protected def getHostClass = classOf[tileentity.Adapter]

  addSlotToContainer(80, 35, Slot.Upgrade)
  addPlayerInventorySlots(8, 84)
}
