package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

object InventoryProviderDatabase extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: Player): Boolean = DriverUpgradeDatabase.worksWith(stack)

  override def getInventory(stack: ItemStack, player: Player): Container = new DatabaseInventory {
    override def container: ItemStack = stack

    override def stillValid(player: Player): Boolean = player == player
  }
}
