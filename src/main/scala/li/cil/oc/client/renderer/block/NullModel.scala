package li.cil.oc.client.renderer.block

import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides}
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.data.IModelData

import java.util
import java.util.Random

object NullModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverrides.EMPTY

  override def getQuads(state: BlockState, side: Direction, rand: Random, extraData: IModelData): util.List[BakedQuad] = {
    java.util.Collections.emptyList()
  }
}
