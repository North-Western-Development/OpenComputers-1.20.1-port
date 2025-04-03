package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections
import li.cil.oc.client.Textures
import li.cil.oc.common.block.property.PropertyCableConnection
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.ItemColorizer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.item.DyeColor
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.client.model.data.ModelData
import org.joml.Vector3d

import scala.collection.JavaConverters.bufferAsJavaList
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.mutable

object CableModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data: ModelData, renderType: RenderType): util.List[BakedQuad] = {
    data match {
      case cable: tileentity.Cable if side == null =>
        val color = cable.getColor
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        faces ++= bakeQuads(Middle, cableTexture, color)
        val directions = Direction.values
        val numConnected = directions.count(d => state.getValue(PropertyCableConnection.BY_DIRECTION.get(d)) != PropertyCableConnection.Shape.NONE)
        for (side <- directions) {
          val shape = state.getValue(PropertyCableConnection.BY_DIRECTION.get(side))
          val connected = shape != PropertyCableConnection.Shape.NONE
          val isCableOnSide = shape == PropertyCableConnection.Shape.CABLE
          val (plug, shortBody, longBody) = Connected(side.get3DDataValue)
          if (connected) {
            if (isCableOnSide) {
              faces ++= bakeQuads(longBody, cableTexture, color)
            }
            else {
              faces ++= bakeQuads(shortBody, cableTexture, color)
              faces ++= bakeQuads(plug, cableCapTexture, None)
            }
          }
          else {
            val otherConn = state.getValue(PropertyCableConnection.BY_DIRECTION.get(side.getOpposite)) != PropertyCableConnection.Shape.NONE
            if ((otherConn && numConnected == 1) || numConnected == 0) {
              faces ++= bakeQuads(Disconnected(side.get3DDataValue), cableCapTexture, None)
            }
          }
        }

        bufferAsJavaList(faces)
      case _ => super.getQuads(state, side, rand)
    }
  }

  protected final val Middle = makeBox(new Vector3d(6 / 16f, 6 / 16f, 6 / 16f), new Vector3d(10 / 16f, 10 / 16f, 10 / 16f))

  // Per side, always plug + short cable + long cable (no plug).
  protected final val Connected = Array(
    (makeBox(new Vector3d(5 / 16f, 0 / 16f, 5 / 16f), new Vector3d(11 / 16f, 1 / 16f, 11 / 16f)),
      makeBox(new Vector3d(6 / 16f, 1 / 16f, 6 / 16f), new Vector3d(10 / 16f, 6 / 16f, 10 / 16f)),
      makeBox(new Vector3d(6 / 16f, 0 / 16f, 6 / 16f), new Vector3d(10 / 16f, 6 / 16f, 10 / 16f))),
    (makeBox(new Vector3d(5 / 16f, 15 / 16f, 5 / 16f), new Vector3d(11 / 16f, 16 / 16f, 11 / 16f)),
      makeBox(new Vector3d(6 / 16f, 10 / 16f, 6 / 16f), new Vector3d(10 / 16f, 15 / 16f, 10 / 16f)),
      makeBox(new Vector3d(6 / 16f, 10 / 16f, 6 / 16f), new Vector3d(10 / 16f, 16 / 16f, 10 / 16f))),
    (makeBox(new Vector3d(5 / 16f, 5 / 16f, 0 / 16f), new Vector3d(11 / 16f, 11 / 16f, 1 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 1 / 16f), new Vector3d(10 / 16f, 10 / 16f, 6 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 0 / 16f), new Vector3d(10 / 16f, 10 / 16f, 6 / 16f))),
    (makeBox(new Vector3d(5 / 16f, 5 / 16f, 15 / 16f), new Vector3d(11 / 16f, 11 / 16f, 16 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 10 / 16f), new Vector3d(10 / 16f, 10 / 16f, 15 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 10 / 16f), new Vector3d(10 / 16f, 10 / 16f, 16 / 16f))),
    (makeBox(new Vector3d(0 / 16f, 5 / 16f, 5 / 16f), new Vector3d(1 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vector3d(1 / 16f, 6 / 16f, 6 / 16f), new Vector3d(6 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vector3d(0 / 16f, 6 / 16f, 6 / 16f), new Vector3d(6 / 16f, 10 / 16f, 10 / 16f))),
    (makeBox(new Vector3d(15 / 16f, 5 / 16f, 5 / 16f), new Vector3d(16 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vector3d(10 / 16f, 6 / 16f, 6 / 16f), new Vector3d(15 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vector3d(10 / 16f, 6 / 16f, 6 / 16f), new Vector3d(16 / 16f, 10 / 16f, 10 / 16f)))
  )

  // Per side, cap only.
  protected final val Disconnected = Array(
    makeBox(new Vector3d(6 / 16f, 5 / 16f, 6 / 16f), new Vector3d(10 / 16f, 6 / 16f, 10 / 16f)),
    makeBox(new Vector3d(6 / 16f, 10 / 16f, 6 / 16f), new Vector3d(10 / 16f, 11 / 16f, 10 / 16f)),
    makeBox(new Vector3d(6 / 16f, 6 / 16f, 5 / 16f), new Vector3d(10 / 16f, 10 / 16f, 6 / 16f)),
    makeBox(new Vector3d(6 / 16f, 6 / 16f, 10 / 16f), new Vector3d(10 / 16f, 10 / 16f, 11 / 16f)),
    makeBox(new Vector3d(5 / 16f, 6 / 16f, 6 / 16f), new Vector3d(6 / 16f, 10 / 16f, 10 / 16f)),
    makeBox(new Vector3d(10 / 16f, 6 / 16f, 6 / 16f), new Vector3d(11 / 16f, 10 / 16f, 10 / 16f))
  )

  protected def cableTexture = Array.fill(6)(Textures.getSprite(Textures.Block.Cable))

  protected def cableCapTexture = Array.fill(6)(Textures.getSprite(Textures.Block.CableCap))

  object ItemOverride extends ItemOverrides {
    class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
      override def getQuads(state: BlockState, side: Direction, rand: RandomSource): util.List[BakedQuad] = {
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        val color = if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else Color.rgbValues(DyeColor.LIGHT_GRAY)

        faces ++= bakeQuads(Middle, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(0)._2, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(1)._2, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(0)._1, cableCapTexture, None)
        faces ++= bakeQuads(Connected(1)._1, cableCapTexture, None)

        bufferAsJavaList(faces)
      }
    }

    override def resolve(originalModel: BakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity, entity_id: Int): BakedModel = new ItemModel(stack)
  }

}
