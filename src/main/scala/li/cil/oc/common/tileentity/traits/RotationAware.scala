package li.cil.oc.common.tileentity.traits

import net.minecraft.core.Direction

trait RotationAware extends TileEntity {
  def toLocal(value: Direction) = value

  def toGlobal(value: Direction) = value
}
