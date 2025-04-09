package li.cil.oc

import li.cil.oc.common.init.Items
import net.minecraft.world.item.CreativeModeTab

object CreativeTab extends CreativeModeTab(CreativeModeTab.builder()
  .icon(() => api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1))
  .displayItems((_, list) =>
    Items.decorateCreativeTab(list)
  )){
}
