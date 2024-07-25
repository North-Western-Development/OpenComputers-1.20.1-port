package li.cil.oc.common.block

import java.util

import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

import scala.collection.convert.ImplicitConversionsToScala._

class Redstone(props: Properties) extends RedstoneAware(props) {
  override protected def tooltipTail(stack: ItemStack, world: BlockGetter, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    super.tooltipTail(stack, world, tooltip, advanced)
    // todo more generic way for redstone mods to provide lines
    if (Mods.ProjectRedTransmission.isModAvailable) {
      for (curr <- Tooltip.get("redstonecard.ProjectRed")) tooltip.add(new StringTextComponent(curr).setStyle(Tooltip.DefaultStyle))
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: BlockGetter) = new tileentity.Redstone(tileentity.BlockEntityTypes.REDSTONE_IO)
}
