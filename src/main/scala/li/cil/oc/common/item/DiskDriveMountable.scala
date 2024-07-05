package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.inventory.DiskDriveMountableInventory
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.util.{ActionResult, ActionResultType, Hand}
import net.minecraft.world.level.Level
import net.minecraftforge.common.extensions.IForgeItem

class DiskDriveMountable(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def use(stack: ItemStack, world: Level, player: Player) = {
    if (!world.isClientSide) player match {
      case srvPlr: ServerPlayer => ContainerTypes.openDiskDriveGui(srvPlr, new DiskDriveMountableInventory {
        override def container: ItemStack = stack

        override def stillValid(player: Player) = player == srvPlr
      })
      case _ =>
    }
    player.swing(Hand.MAIN_HAND)
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }
}
