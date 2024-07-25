package li.cil.oc.integration.cofh.foundation

import cofh.thermal.core.item.WrenchItem
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand

object EventHandlerFoundation {
  def useWrench(player: Player, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getItemInHand(InteractionHand.MAIN_HAND).getItem match {
      case wrench: WrenchItem => true
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[WrenchItem]
}
