package li.cil.oc.client.renderer.block

import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.client.{KeyBindings, Textures}
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.util.{Color, ExtendedAABB}
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides}
import net.minecraft.client.renderer.texture.{MissingTextureAtlasSprite, TextureAtlasSprite}
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.data.IModelData

import java.util
import scala.collection.JavaConverters.bufferAsJavaList
import scala.collection.mutable

object PrintModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: util.Random, data: IModelData): util.List[BakedQuad] =
    data match {
      case t: tileentity.Print =>
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        for (shape <- t.shapes if !Strings.isNullOrEmpty(shape.texture)) {
          val bounds = shape.bounds.rotateTowards(t.facing)
          val texture = resolveTexture(shape.texture)
          faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), shape.tint.getOrElse(White))
        }

        bufferAsJavaList(faces)
      case _ => super.getQuads(state, side, rand)
    }

  private def resolveTexture(name: String): TextureAtlasSprite = try {
    val texture = Textures.getSprite(new ResourceLocation(name))
    if (texture.getName == MissingTextureAtlasSprite.getLocation) Textures.getSprite(new ResourceLocation("minecraft:blocks/" + name))
    else texture
  }
  catch {
    case _: Throwable => Textures.getSprite(MissingTextureAtlasSprite.getLocation)
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    override def getOverrides: ItemOverrides = ItemOverrides.EMPTY

    val data = new PrintData(stack)

    override def getQuads(state: BlockState, side: Direction, rand: util.Random): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      val shapes =
        if (data.hasActiveState && KeyBindings.showExtendedTooltips)
          data.stateOn
        else
          data.stateOff
      for (shape <- shapes) {
        val bounds = shape.bounds
        val texture = resolveTexture(shape.texture)
        faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), shape.tint.getOrElse(White))
      }
      if (shapes.isEmpty) {
        val bounds = ExtendedAABB.unitBounds
        val texture = resolveTexture(Settings.resourceDomain + ":blocks/white")
        faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), Color.rgbValues(DyeColor.LIME))
      }

      bufferAsJavaList(faces)
    }
  }

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity, seed: Int): BakedModel = new ItemModel(stack)
  }

}
