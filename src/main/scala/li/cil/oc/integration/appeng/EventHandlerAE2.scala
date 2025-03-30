package li.cil.oc.integration.appeng

import appeng.api.implementations.items.IAEWrench
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos

object EventHandlerAE2 {
  def useWrench(player: Player, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getItemInHand(Hand.MAIN_HAND).getItem match {
      case wrench: IAEWrench => wrench.canWrench(player.getItemInHand(Hand.MAIN_HAND), player, pos)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IAEWrench]
}
