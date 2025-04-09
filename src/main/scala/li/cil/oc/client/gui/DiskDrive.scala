package li.cil.oc.client.gui

import li.cil.oc.common.container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component

class DiskDrive(state: container.DiskDrive, playerInventory:Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {
}
