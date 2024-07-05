package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.api.internal
import li.cil.oc.server.PacketSender
import li.cil.oc.util.Color
import net.minecraft.world.item.DyeColor
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

trait Colored extends BlockEntity with internal.Colored {
  private var _color = 0

  def consumesDye = false

  override def getColor: Int = _color

  override def setColor(value: Int) = if (value != _color) {
    _color = value
    onColorChanged()
  }

  override def controlsConnectivity = false

  protected def onColorChanged() {
    if (getLevel != null && isServer) {
      PacketSender.sendColorChange(this)
    }
  }

  // ----------------------------------------------------------------------- //

  private final val RenderColorTag = Settings.namespace + "renderColorRGB"
  private final val RenderColorTagCompat = Settings.namespace + "renderColor"

  override def loadForServer(nbt: CompoundTag) {
    super.loadForServer(nbt)
    if (nbt.contains(RenderColorTagCompat)) {
      _color = Color.rgbValues(DyeColor.byId(nbt.getInt(RenderColorTagCompat)))
    }
    if (nbt.contains(RenderColorTag)) {
      _color = nbt.getInt(RenderColorTag)
    }
  }

  override def saveForServer(nbt: CompoundTag) {
    super.saveForServer(nbt)
    nbt.putInt(RenderColorTag, _color)
  }

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundTag) {
    super.loadForClient(nbt)
    _color = nbt.getInt(RenderColorTag)
  }

  override def saveForClient(nbt: CompoundTag) {
    super.saveForClient(nbt)
    nbt.putInt(RenderColorTag, _color)
  }
}
