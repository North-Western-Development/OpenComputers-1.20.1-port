package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.container.{Database => DatabaseContainer}
import li.cil.oc.integration.opencomputers.DriverUpgradeDatabase
import net.minecraft.world.entity.player.Player
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.MenuProvider

trait DatabaseInventory extends ItemStackInventory with MenuProvider {
  def tier: Int = DriverUpgradeDatabase.tier(container)

  override def getContainerSize = Settings.get.databaseEntriesPerTier(tier)

  override protected def inventoryName = "database"

  override def getMaxStackSize = 1

  override def getInventoryStackRequired = 1

  override def canPlaceItem(slot: Int, stack: ItemStack) = stack != container

  override def getDisplayName = TextComponent.EMPTY

  override def createMenu(id: Int, playerInventory: net.minecraft.world.entity.player.Inventory, player: Player) =
    new DatabaseContainer(ContainerTypes.DATABASE, id, playerInventory, container, this, tier)
}
