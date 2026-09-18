package li.cil.oc.common.item

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.Localization
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedLevel._
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.core.Direction
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.world.level.block.Block
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.common.extensions.IForgeItem

import scala.collection.mutable

class TexturePicker(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  @OnlyIn(Dist.CLIENT)
  override def onItemUse(stack: ItemStack, player: Player, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = player.level.getBlock(position) match {
    case block: Block =>
      if (player.level.isClientSide) {
        val pos = position.toBlockPos
        val state = player.level.getBlockState(pos)
        val modelData = net.minecraftforge.client.model.ModelDataManager.getModelData(net.minecraft.client.Minecraft.getInstance.level, pos)
        val printedNames = mutable.Set.empty[String]
        val model = Minecraft.getInstance.getBlockRenderer.getBlockModel(state)
        val particle = model.getParticleIcon(modelData)

        if (particle != null && particle.getName != null) {
          player.sendMessage(Localization.Chat.TextureName(particle.getName.toString), Util.NIL_UUID)
          printedNames += particle.getName.toString
        }

        Minecraft.getInstance.getBlockRenderer.renderSingleBlock(state,new PoseStack(),new RenderCallback(player, printedNames),0,OverlayTexture.NO_OVERLAY, modelData)
      }
      true
    case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
  }
}

@OnlyIn(Dist.CLIENT)
private class RenderCallback(val player: Player, val printedNames: mutable.Set[String]) extends MultiBufferSource{

  override def getBuffer(p_109903_ : RenderType): VertexConsumer = {
    class VertexCallback extends VertexConsumer {

      override def vertex(p_85945_ : Double, p_85946_ : Double, p_85947_ : Double): VertexConsumer = this
      override def color(p_85973_ : Int, p_85974_ : Int, p_85975_ : Int, p_85976_ : Int): VertexConsumer = this
      override def uv(p_85948_ : Float, p_85949_ : Float): VertexConsumer = this
      override def overlayCoords(p_85971_ : Int, p_85972_ : Int): VertexConsumer = this
      override def uv2(p_86010_ : Int, p_86011_ : Int): VertexConsumer = this
      override def normal(p_86005_ : Float, p_86006_ : Float, p_86007_ : Float): VertexConsumer = this
      override def endVertex(): Unit = {}
      override def defaultColor(p_166901_ : Int, p_166902_ : Int, p_166903_ : Int, p_166904_ : Int): Unit = {
      }
      override def unsetDefaultColor(): Unit = {
      }

      override def putBulkData(pose: PoseStack.Pose, bakedQuad: BakedQuad, baseBrightness: Array[Float], red: Float, green: Float, blue: Float, alpha: Float, lightmap: Array[Int], packedOverlay: Int, readExistingColor: Boolean): Unit = {
        val texture = bakedQuad.getSprite
        if (texture != null && texture.getName != null && !printedNames.contains(texture.getName.toString)) {
          printedNames += texture.getName.toString
          player.sendMessage(Localization.Chat.TextureName(texture.getName.toString), Util.NIL_UUID)
        }
      }

      override def putBulkData(p_85996_ : PoseStack.Pose, p_85997_ : BakedQuad, p_85998_ : Array[Float], p_85999_ : Float, p_86000_ : Float, p_86001_ : Float, p_86002_ : Array[Int], p_86003_ : Int, p_86004_ : Boolean): Unit = {
        val texture = p_85997_.getSprite
        if (texture != null && texture.getName != null && !printedNames.contains(texture.getName.toString)) {
          printedNames += texture.getName.toString
          player.sendMessage(Localization.Chat.TextureName(texture.getName.toString), Util.NIL_UUID)
        }
      }
    }
    new VertexCallback()
  }
}
