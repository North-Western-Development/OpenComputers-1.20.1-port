package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

class Geolyzer(props: Properties) extends SimpleBlock(props) {
  override def newBlockEntity(pos:BlockPos, state: BlockState) = new tileentity.Geolyzer(tileentity.BlockEntityTypes.GEOLYZER, pos, state)

  override def getTicker[T <: BlockEntity](level: Level, blockState: BlockState, blockEntityType: BlockEntityType[T]): BlockEntityTicker[T] = {
    (_: Level, pos: BlockPos, state: BlockState, entity: T) =>
      entity match {
        case tileEntity: tileentity.Geolyzer => tileEntity.updateEntity()
        case _ =>
      }
  }
}
