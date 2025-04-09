package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraftforge.common.extensions.IForgeItem

class ArrowKeys(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override protected def tooltipName = None
}
