package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

class CarpetedCapacitor(props: Properties) extends Capacitor(props) {
  override def newBlockEntity(pos:BlockPos, state: BlockState): tileentity.CarpetedCapacitor = new tileentity.CarpetedCapacitor(tileentity.BlockEntityTypes.CARPETED_CAPACITOR, pos, state)
}
