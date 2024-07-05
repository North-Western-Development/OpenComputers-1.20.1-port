package li.cil.oc.common.tileentity.traits

import net.minecraft.tileentity.ITickableBlockEntity

trait Tickable extends BlockEntity with ITickableBlockEntity {
  override def tick(): Unit = updateEntity()
}
