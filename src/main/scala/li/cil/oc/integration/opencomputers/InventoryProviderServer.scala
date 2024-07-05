package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

object InventoryProviderServer extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: Player): Boolean = DriverServer.worksWith(stack)

  override def getInventory(stack: ItemStack, player: Player): Container = new ServerInventory {
    override def container: ItemStack = stack

    override def rackSlot = -1
  }
}
