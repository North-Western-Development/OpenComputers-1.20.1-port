package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockBehaviour.Properties

class CarpetedCapacitor(props: Properties) extends Capacitor(props) {
  override def newBlockEntity(pos: BlockPos, state: BlockState) = new tileentity.CarpetedCapacitor(tileentity.TileEntityTypes.CARPETED_CAPACITOR)
}
