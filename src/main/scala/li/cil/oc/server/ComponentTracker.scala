package li.cil.oc.server

import li.cil.oc.common
import net.minecraft.world.level.Level

object ComponentTracker extends common.ComponentTracker {
  override protected def clear(world: Level) = if (!world.isClientSide) super.clear(world)
}
