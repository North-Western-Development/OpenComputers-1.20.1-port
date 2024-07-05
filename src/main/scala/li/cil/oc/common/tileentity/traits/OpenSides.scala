package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.util.RotationHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
  * @author Vexatos
  */
trait OpenSides extends BlockEntity {
  protected def SideCount = RotationHelper.getNumDirections

  protected def defaultState: Boolean = false

  var openSides = Array.fill(SideCount)(defaultState)

  def compressSides = (Direction.values(), openSides).zipped.foldLeft(0)((acc, entry) => acc | (if (entry._2) 1 << entry._1.ordinal() else 0)).toByte

  def uncompressSides(byte: Byte) = Direction.values().map(d => ((1 << d.ordinal()) & byte) != 0)

  def isSideOpen(side: Direction) = side != null && openSides(side.ordinal())

  def setSideOpen(side: Direction, value: Boolean): Unit = if (side != null && openSides(side.ordinal()) != value) {
    openSides(side.ordinal()) = value
  }

  override def loadForServer(nbt: CompoundTag) {
    super.loadForServer(nbt)
    if (nbt.contains(Settings.namespace + "openSides"))
      openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def saveForServer(nbt: CompoundTag) {
    super.saveForServer(nbt)
    nbt.putByte(Settings.namespace + "openSides", compressSides)
  }

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundTag) {
    super.loadForClient(nbt)
    openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def saveForClient(nbt: CompoundTag) {
    super.saveForClient(nbt)
    nbt.putByte(Settings.namespace + "openSides", compressSides)
  }
}
