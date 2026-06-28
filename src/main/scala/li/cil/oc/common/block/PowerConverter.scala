package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

class PowerConverter(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor {
  override def energyThroughput: Double = Settings.get.powerConverterRate

  override def newBlockEntity(pos:BlockPos, state: BlockState) = new tileentity.PowerConverter(tileentity.BlockEntityTypes.POWER_CONVERTER.get(), pos, state)


  override def getTicker[T <: BlockEntity](level: Level, blockState: BlockState, blockEntityType: BlockEntityType[T]): BlockEntityTicker[T] = {
    (_: Level, pos: BlockPos, state: BlockState, entity: T) =>
      entity match {
        case tileEntity: tileentity.PowerConverter => tileEntity.updateEntity()
        case _ =>
      }
  }
}
