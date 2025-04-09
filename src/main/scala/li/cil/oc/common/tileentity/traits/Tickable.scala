package li.cil.oc.common.tileentity.traits

import net.minecraft.world.level.block.entity.TickingBlockEntity

trait Tickable extends TileEntity with TickingBlockEntity {
  override def tick(): Unit = updateEntity()
}
