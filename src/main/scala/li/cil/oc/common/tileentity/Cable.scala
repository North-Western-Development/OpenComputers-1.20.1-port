package li.cil.oc.common.tileentity

import li.cil.oc.{Constants, api}
import li.cil.oc.api.network.Visibility
import li.cil.oc.util.{Color, ItemColorizer}
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}

class Cable(selfType: BlockEntityType[_ <: Cable]) extends BlockEntity(selfType) with traits.Environment with traits.NotAnalyzable with traits.ImmibisMicroblock with traits.Colored {
  val node = api.Network.newNode(this, Visibility.None).create()

  setColor(Color.rgbValues(DyeColor.LIGHT_GRAY))

  def createItemStack() = {
    val stack = api.Items.get(Constants.BlockName.Cable).createItemStack(1)
    if (getColor != Color.rgbValues(DyeColor.LIGHT_GRAY)) {
      ItemColorizer.setColor(stack, getColor)
    }
    stack
  }

  def fromItemStack(stack: ItemStack): Unit = {
    if (ItemColorizer.hasColor(stack)) {
      setColor(ItemColorizer.getColor(stack))
    }
  }

  override def controlsConnectivity = true

  override def consumesDye = true

  override protected def onColorChanged() {
    super.onColorChanged()
    if (getLevel != null && isServer) {
      api.Network.joinOrCreateNetwork(this)
    }
  }
}
