package li.cil.oc.client.renderer.block

import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.block.Screen
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.util.{Color, RotationHelper}
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides}
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.data.{IModelData, ModelProperty}

import java.util
import java.util.Collections
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

object ScreenModel extends SmartBlockModelBase {
  final val COLOR_PROPERTY: ModelProperty[Int] = new ModelProperty[Int]()
  /**
   * merged because individually they are meaningless
   */
  final val WIDTH_HEIGHT_LOCAL_POSITION_PROPERTY: ModelProperty[(Int, Int, Int, Int)] = new ModelProperty[(Int, Int, Int, Int)]()

  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: util.Random, data: IModelData): util.List[BakedQuad] = {
    if (side == null)
      return Collections.emptyList()

    val pitch = state.getValue(PropertyRotatable.Pitch)
    val yaw = state.getValue(PropertyRotatable.Yaw)
    val color: Int = Option(data.getData(COLOR_PROPERTY)).getOrElse(Color.rgbValues(Color.byTier(0)))
    val (width: Int, height: Int, x: Int, y: Int) = Option(data.getData[(Int, Int, Int, Int)](WIDTH_HEIGHT_LOCAL_POSITION_PROPERTY)).getOrElse(() => (1, 1, 0, 0))

    val facing = toLocal(side, pitch, yaw)

    var px = xy2part(x, width - 1)
    var py = xy2part(y, height - 1)

    if ((side == Direction.DOWN || this.facing(pitch, yaw) == Direction.DOWN) && side != this.facing(pitch, yaw)) {
      px = 2 - px
      py = 2 - py
    }

    val rotation =
      if (side == Direction.UP) yaw.get2DDataValue
      else if (side == Direction.DOWN) -yaw.get2DDataValue
      else 0

    val pitch2 = if (pitch == Direction.NORTH) 0 else 1
    val texture =
      if (width == 1 && height == 1) {
        if (facing == Direction.SOUTH)
          Textures.Block.Screen.SingleFront(pitch2)
        else
          Textures.Block.Screen.Single(side.get3DDataValue)
      }
      else if (width == 1) {
        if (facing == Direction.SOUTH)
          Textures.Block.Screen.VerticalFront(pitch2)(py)
        else
          Textures.Block.Screen.Vertical(pitch2)(py)(facing.get3DDataValue)
      }
      else if (height == 1) {
        if (facing == Direction.SOUTH)
          Textures.Block.Screen.HorizontalFront(pitch2)(px)
        else
          Textures.Block.Screen.Horizontal(pitch2)(px)(facing.get3DDataValue)
      }
      else {
        if (facing == Direction.SOUTH)
          Textures.Block.Screen.MultiFront(pitch2)(py)(px)
        else
          Textures.Block.Screen.Multi(pitch2)(py)(px)(facing.get3DDataValue)
      }

    Seq(bakeQuad(side, Textures.getSprite(texture), Some(color), rotation)).asJava
  }

  private def xy2part(value: Int, high: Int) = if (value == 0) 2 else if (value == high) 0 else 1

  private def toLocal(value: Direction, pitch: Direction, yaw: Direction): Direction = if (value == null) null else {
    if (pitch != null && yaw != null) RotationHelper.toLocal(pitch, yaw, value) else null
  }

  private def facing(pitch: Direction, yaw: Direction): Direction = pitch match {
    case Direction.DOWN | Direction.UP => pitch
    case _ => yaw
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    override def getOverrides: ItemOverrides = ItemOverrides.EMPTY

    val color: DyeColor = api.Items.get(stack).block() match {
      case screen: Screen => Color.byTier(screen.tier)
      case _ => Color.byTier(Tier.One)
    }

    override def getQuads(state: BlockState, side: Direction, rand: util.Random, data: IModelData): util.List[BakedQuad] = {
      val result =
        if (side == Direction.NORTH || side == null)
          Textures.Block.Screen.SingleFront(0)
        else
          Textures.Block.Screen.Single(side.ordinal())
      Seq(bakeQuad(if (side != null) side else Direction.SOUTH, Textures.getSprite(result), Some(Color.rgbValues(color)), 0)).asJava
    }
  }

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity, seed: Int): BakedModel = new ItemModel(stack)
  }

}
