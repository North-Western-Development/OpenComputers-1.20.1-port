package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

class PowerConverter(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor {
  override def energyThroughput: Double = Settings.get.powerConverterRate

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new tileentity.PowerConverter(tileentity.TileEntityTypes.POWER_CONVERTER.get(), pos, state)
}
