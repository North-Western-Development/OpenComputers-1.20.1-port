package li.cil.oc.common.block

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.core.NonNullList
import net.minecraft.world.level.block.state.BlockBehaviour.Properties

object ChameliumBlock {
  final val Color = EnumProperty.create("color", classOf[DyeColor])
}

class ChameliumBlock(props: Properties) extends SimpleBlock(props) {
  protected override def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]): Unit = {
    builder.add(ChameliumBlock.Color)
  }
  registerDefaultState(stateDefinition.any.setValue(ChameliumBlock.Color, DyeColor.BLACK))

  override def getStateForPlacement(ctx: BlockPlaceContext): BlockState =
    defaultBlockState.setValue(ChameliumBlock.Color, DyeColor.byId(ctx.getItemInHand.getOrCreateTag().getInt("color")))

  override def fillItemCategory(tab: CreativeModeTab, list: NonNullList[ItemStack]): Unit = {
    val stack = new ItemStack(this)
    stack.getOrCreateTag().putInt("color", defaultBlockState.getValue(ChameliumBlock.Color).getId)
    list.add(stack)
  }
}
