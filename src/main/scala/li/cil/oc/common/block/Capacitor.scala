package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

import java.util.Random

class Capacitor(props: Properties) extends SimpleBlock(props) {
  @Deprecated
  override def isRandomlyTicking(state: BlockState) = true

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos:BlockPos, state: BlockState) = new tileentity.Capacitor(tileentity.BlockEntityTypes.CAPACITOR, pos, state)

  // ----------------------------------------------------------------------- //

  override def hasAnalogOutputSignal(state: BlockState): Boolean = true

  override def getAnalogOutputSignal(state: BlockState, world: Level, pos: BlockPos): Int =
    world.getBlockEntity(pos) match {
      case capacitor: tileentity.Capacitor if !world.isClientSide =>
        math.round(15 * capacitor.node.localBuffer / capacitor.node.localBufferSize).toInt
      case _ => 0
    }

  override def tick(state: BlockState, world: ServerLevel, pos: BlockPos, rand: Random): Unit = {
    world.updateNeighborsAt(pos, this)
  }

  @Deprecated
  override def neighborChanged(state: BlockState, world: Level, pos: BlockPos, block: Block, fromPos: BlockPos, b: Boolean): Unit =
    world.getBlockEntity(pos) match {
      case capacitor: tileentity.Capacitor => capacitor.recomputeCapacity()
      case _ =>
    }

  override def getTicker[T <: BlockEntity](level: Level, blockState: BlockState, blockEntityType: BlockEntityType[T]): BlockEntityTicker[T] = {
    (_: Level, pos: BlockPos, state: BlockState, entity: T) =>
      entity match {
        case tileEntity: tileentity.Capacitor => tileEntity.updateEntity()
        case _ =>
      }
  }
}
