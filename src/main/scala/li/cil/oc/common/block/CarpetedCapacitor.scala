package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.world.level.BlockGetter

class CarpetedCapacitor(props: Properties) extends Capacitor(props) {
  override def newBlockEntity(world: BlockGetter) = new tileentity.CarpetedCapacitor(tileentity.BlockEntityTypes.CARPETED_CAPACITOR)
}
