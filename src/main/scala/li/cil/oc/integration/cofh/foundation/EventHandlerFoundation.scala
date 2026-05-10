package li.cil.oc.integration.cofh.foundation
// FIXME : WrenchItem, or find alternative/investigate
import cofh.core.item.HammerItem
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand

object EventHandlerFoundation {
  def useWrench(player: Player, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getItemInHand(InteractionHand.MAIN_HAND).getItem match {
      case wrench: HammerItem => true
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[HammerItem]
}
