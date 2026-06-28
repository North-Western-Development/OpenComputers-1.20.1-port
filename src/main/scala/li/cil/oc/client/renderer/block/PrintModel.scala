package li.cil.oc.client.renderer.block

import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.client.{KeyBindings, Textures}
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.{Color, ExtendedAABB}
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides}
import net.minecraft.client.renderer.texture.{MissingTextureAtlasSprite, TextureAtlasSprite}
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.data.{ModelData, ModelProperty}

import java.util
import java.util.Collections
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object PrintModel extends SmartBlockModelBase {
  final val PRINT_PROPERTY: ModelProperty[tileentity.Print] = new ModelProperty[tileentity.Print]()

  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data: ModelData, renderType: RenderType): util.List[BakedQuad] = {
    if (side != null)
      return Collections.emptyList()

    val t = data.get(PRINT_PROPERTY)
    if (t == null || t.shapes.isEmpty) {
      val bounds = ExtendedAABB.unitBounds
      val texture = resolveTexture(Settings.resourceDomain + ":blocks/white")
      return bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), Color.rgbValues(DyeColor.LIME)).toList.asJava
    }

    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    for (shape <- t.shapes if !Strings.isNullOrEmpty(shape.texture)) {
      val bounds = shape.bounds.rotateTowards(t.facing)
      val texture = resolveTexture(shape.texture)
      faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), shape.tint.getOrElse(White))
    }

    faces.asJava
  }

  private def resolveTexture(name: String): TextureAtlasSprite = try {
    val texture = Textures.getSprite(ResourceLocation.parse(name))
    if (texture.getName == MissingTextureAtlasSprite.getLocation) Textures.getSprite(ResourceLocation.fromNamespaceAndPath("minecraft", "block/" + name))
    else texture
  }
  catch {
    case _: Throwable => Textures.getSprite(MissingTextureAtlasSprite.getLocation)
  }

  class ItemModel(val data: PrintData) extends SmartBlockModelBase {
    override def getOverrides: ItemOverrides = ItemOverrides.EMPTY

    override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data2: ModelData, renderType: RenderType): util.List[BakedQuad] = {
      if (side != null)
        return Collections.emptyList()

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

      faces.asJava
    }
  }

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity, seed: Int): BakedModel = new ItemModel(new PrintData(stack))
  }

}
