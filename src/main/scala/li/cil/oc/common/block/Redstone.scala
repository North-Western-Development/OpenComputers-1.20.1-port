package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.{BlockGetter, Level}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

import java.util
import scala.collection.convert.ImplicitConversionsToScala._

class Redstone(props: Properties) extends RedstoneAware(props) {
  override protected def tooltipTail(stack: ItemStack, world: BlockGetter, tooltip: util.List[Component], advanced: TooltipFlag) {
    super.tooltipTail(stack, world, tooltip, advanced)
    // todo more generic way for redstone mods to provide lines
    if (Mods.ProjectRedTransmission.isModAvailable) {
      for (curr <- Tooltip.get("redstonecard.ProjectRed")) tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle))
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos:BlockPos, state: BlockState) = new tileentity.Redstone(tileentity.BlockEntityTypes.REDSTONE_IO.get(), pos, state)

  override def getTicker[T <: BlockEntity](level: Level, blockState: BlockState, blockEntityType: BlockEntityType[T]): BlockEntityTicker[T] = {
    (_: Level, pos: BlockPos, state: BlockState, entity: T) =>
      entity match {
        case tileEntity: tileentity.Redstone => tileEntity.updateEntity()
        case _ =>
      }
  }
}
