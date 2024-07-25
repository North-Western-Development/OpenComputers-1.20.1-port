package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.server.level.ServerPlayer
import net.minecraft.core.BlockPos
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

class Assembler(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override def energyThroughput = Settings.get.assemblerRate

  val blockShape = {
    val bottom = Block.box(0, 0, 0, 16, 7, 16)
    val mid = Block.box(2, 7, 2, 14, 9, 14)
    val top = Block.box(0, 9, 0, 16, 16, 16)
    VoxelShapes.or(top, bottom, mid)
  }

  override def getShape(state: BlockState, world: BlockGetter, pos: BlockPos, ctx: ISelectionContext): VoxelShape = blockShape

  override def openGui(player: ServerPlayer, world: Level, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Assembler => ContainerTypes.openAssemblerGui(player, te)
    case _ =>
  }

  override def newBlockEntity(world: BlockGetter) = new tileentity.Assembler(tileentity.BlockEntityTypes.ASSEMBLER)
}
