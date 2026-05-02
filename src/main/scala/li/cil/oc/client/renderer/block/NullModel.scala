package li.cil.oc.client.renderer.block

import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides}
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.data.ModelData

import java.util

object NullModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverrides.EMPTY

  override def getQuads(state: BlockState, side: Direction, rand: RandomSource, extraData: ModelData, renderType: RenderType): util.List[BakedQuad] = {
    java.util.Collections.emptyList()
  }
}
