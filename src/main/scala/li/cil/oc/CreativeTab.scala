package li.cil.oc

import li.cil.oc.common.init.Items
import net.minecraft.core.NonNullList
import net.minecraft.world.item.{CreativeModeTab, ItemStack}

object CreativeTab extends CreativeModeTab(OpenComputers.Name) {
  private lazy val stack = api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1)

  override def makeIcon: ItemStack = stack

  override def fillItemList(list: NonNullList[ItemStack]) : Unit = {
    super.fillItemList(list)
    Items.decorateCreativeTab(list)
  }
}
