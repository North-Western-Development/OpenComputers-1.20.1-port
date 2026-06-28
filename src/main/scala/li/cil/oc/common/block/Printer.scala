package li.cil.oc.common.block

import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.{BlockGetter, Level}
import net.minecraft.world.phys.shapes.{BooleanOp, CollisionContext, Shapes, VoxelShape}

class Printer(props: Properties) extends SimpleBlock(props) with traits.StateAware with traits.GUI {
  val blockShape = {
    val base = Block.box(0, 0, 0, 16, 8, 16)
    val pillars = Shapes.or(Block.box(0, 8, 0, 3, 13, 3), Block.box(13, 8, 0, 16, 13, 3),
      Block.box(13, 8, 13, 16, 13, 16), Block.box(0, 8, 13, 3, 13, 16))
    val ring = Shapes.join(Block.box(0, 13, 0, 16, 16, 16),
      Block.box(3, 13, 3, 13, 16, 13), BooleanOp.ONLY_FIRST)
    Shapes.or(base, pillars, ring)
  }

  override def getShape(state: BlockState, world: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = blockShape

  override def openGui(player: ServerPlayer, world: Level, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Printer => ContainerTypes.openPrinterGui(player, te)
    case _ =>
  }

  override def newBlockEntity(pos:BlockPos, state: BlockState) = new tileentity.Printer(tileentity.BlockEntityTypes.PRINTER.get(), pos, state)

  override def getTicker[T <: BlockEntity](level: Level, blockState: BlockState, blockEntityType: BlockEntityType[T]): BlockEntityTicker[T] = {
    (_: Level, pos: BlockPos, state: BlockState, entity: T) =>
      entity match {
        case tileEntity: tileentity.Printer => tileEntity.updateEntity()
        case _ =>
      }
  }
}
