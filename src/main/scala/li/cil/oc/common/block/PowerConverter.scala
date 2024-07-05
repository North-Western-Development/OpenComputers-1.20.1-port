package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.world.level.BlockGetter

class PowerConverter(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor {
  override def energyThroughput: Double = Settings.get.powerConverterRate

  override def newBlockEntity(world: BlockGetter) = new tileentity.PowerConverter(tileentity.BlockEntityTypes.POWER_CONVERTER)
}
