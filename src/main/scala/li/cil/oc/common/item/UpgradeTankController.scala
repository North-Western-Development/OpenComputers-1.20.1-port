package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraftforge.common.extensions.IForgeItem

class UpgradeTankController(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier