package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.{Component, TextComponent}
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.{CollisionContext, Shapes, VoxelShape}

import java.util
import scala.collection.convert.ImplicitConversionsToScala._

class Hologram(props: Properties, val tier: Int) extends SimpleBlock(props) {
  val shape = Shapes.box(0, 0, 0, 1, 0.5, 1)

  // ----------------------------------------------------------------------- //

  override def getShape(state: BlockState, world: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape

  // ----------------------------------------------------------------------- //

  override protected def tooltipBody(stack: ItemStack, world: BlockGetter, tooltip: util.List[Component], advanced: TooltipFlag) {
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase() + tier)) {
      tooltip.add(new TextComponent(curr).setStyle(Tooltip.DefaultStyle))
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos:BlockPos, state: BlockState) = new tileentity.Hologram(tileentity.BlockEntityTypes.HOLOGRAM, pos, state, tier)
}
