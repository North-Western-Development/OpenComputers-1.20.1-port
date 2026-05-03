package li.cil.oc.common.block.traits

import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.util.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.BlockGetter

import java.util
import scala.collection.convert.ImplicitConversionsToScala._

trait PowerAcceptor extends SimpleBlock {
  def energyThroughput: Double

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(stack: ItemStack, world: BlockGetter, tooltip: util.List[Component], advanced: TooltipFlag) {
    super.tooltipTail(stack, world, tooltip, advanced)
    for (curr <- Tooltip.extended("poweracceptor", energyThroughput.toInt)) {
      tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle))
    }
  }
}
