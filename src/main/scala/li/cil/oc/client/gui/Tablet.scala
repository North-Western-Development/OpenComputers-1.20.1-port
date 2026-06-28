package li.cil.oc.client.gui

import li.cil.oc.common.container
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Tablet(state: container.Tablet, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[container.Tablet] {

  override def lockedStack = inventoryContainer.stack
}
