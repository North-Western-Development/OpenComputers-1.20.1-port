package li.cil.oc.client.renderer.block

import net.minecraft.client.renderer.block.model.ItemOverrides

object NullModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverrides.EMPTY
}
