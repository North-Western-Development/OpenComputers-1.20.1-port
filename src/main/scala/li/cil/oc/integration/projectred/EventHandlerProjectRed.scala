package li.cil.oc.integration.projectred

import mrtjp.projectred.api.IScrewdriver
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand

object EventHandlerProjectRed {
  def useWrench(player: Player, pos: BlockPos, changeDurability: Boolean): Boolean = {
    val stack = player.getItemInHand(InteractionHand.MAIN_HAND)
    stack.getItem match {
      case wrench: IScrewdriver =>
        if (changeDurability) {
          wrench.damageScrewdriver(player, stack)
          true
        }
        else true
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IScrewdriver]
}
