package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.BlockGetter

class CarpetedCapacitor(props: Properties) extends Capacitor(props) {
  override def newBlockEntity(world: BlockGetter) = new tileentity.CarpetedCapacitor(tileentity.BlockEntityTypes.CARPETED_CAPACITOR)
}
