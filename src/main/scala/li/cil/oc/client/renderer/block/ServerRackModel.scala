package li.cil.oc.client.renderer.block

import li.cil.oc.api.component.RackMountable
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides}
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraftforge.client.model.data.{ModelData, ModelProperty}
import net.minecraftforge.common.MinecraftForge

import java.util
import java.util.Collections
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ServerRackModel {
  final val RACK_PROPERTY: ModelProperty[tileentity.Rack] = new ModelProperty[tileentity.Rack]()
}

class ServerRackModel(val parent: BakedModel) extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data: ModelData, renderType: RenderType): util.List[BakedQuad] = {
    val rack = data.get(ServerRackModel.RACK_PROPERTY)
    if (rack == null) {
      if (side == null) {
        val faces = mutable.ArrayBuffer.empty[BakedQuad]
        for (side <- Direction.values) {
          faces ++= bakeQuads(Case(side.get3DDataValue), serverRackTexture, None)
        }
        return faces.asJava
      } else {
        return Collections.emptyList()
      }
    }

    val facing = rack.facing
    val faces = mutable.ArrayBuffer.empty[BakedQuad]
    if (side == null)
      for (side <- Direction.values if side != facing) {
        faces ++= bakeQuads(Case(side.get3DDataValue), serverRackTexture, None)
      }

    if (side == facing) {
      val textures = serverTexture
      val defaultFront = Textures.getSprite(Textures.Block.RackFront)
      for (slot <- 0 until 4) rack.getMountable(slot) match {
        case mountable: RackMountable =>
          val event = new RackMountableRenderEvent.Block(rack, slot, rack.lastData(slot), facing)
          MinecraftForge.EVENT_BUS.post(event)
          if (!event.isCanceled) {
            if (event.getFrontTextureOverride != null) {
              (2 until 6).foreach(textures(_) = event.getFrontTextureOverride)
            } else {
              (2 until 6).foreach(textures(_) = defaultFront)
            }
            faces ++= bakeQuads(Servers(slot), textures, None)
          }
        case _ =>
      }
    }

    faces.asJava
  }

  protected def serverRackTexture = Array(
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.RackSide),
    Textures.getSprite(Textures.Block.RackSide),
    Textures.getSprite(Textures.Block.RackSide),
    Textures.getSprite(Textures.Block.RackSide)
  )

  protected def serverTexture = Array(
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.RackFront),
    Textures.getSprite(Textures.Block.RackFront),
    Textures.getSprite(Textures.Block.RackFront),
    Textures.getSprite(Textures.Block.RackFront)
  )

  protected final val Case = Array(
    makeBox(new Vec3(0 / 16f, 0 / 16f, 0 / 16f), new Vec3(16 / 16f, 2 / 16f, 16 / 16f)),
    makeBox(new Vec3(0 / 16f, 14 / 16f, 0 / 16f), new Vec3(16 / 16f, 16 / 16f, 16 / 16f)),
    makeBox(new Vec3(0 / 16f, 2 / 16f, 0 / 16f), new Vec3(16 / 16f, 14 / 16f, 0.99f / 16f)),
    makeBox(new Vec3(0 / 16f, 2 / 16f, 15.01f / 16f), new Vec3(16 / 16f, 14 / 16f, 16 / 16f)),
    makeBox(new Vec3(0 / 16f, 2 / 16f, 0 / 16f), new Vec3(0.99f / 16f, 14 / 16f, 16 / 16f)),
    makeBox(new Vec3(15.01f / 16f, 2 / 16f, 0 / 16f), new Vec3(16 / 16f, 14f / 16f, 16 / 16f))
  )

  protected final val Servers = Array(
    makeBox(new Vec3(0.5f / 16f, 11 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 14 / 16f, 15.5f / 16f)),
    makeBox(new Vec3(0.5f / 16f, 8 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 11 / 16f, 15.5f / 16f)),
    makeBox(new Vec3(0.5f / 16f, 5 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 8 / 16f, 15.5f / 16f)),
    makeBox(new Vec3(0.5f / 16f, 2 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 5 / 16f, 15.5f / 16f))
  )

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity, seed: Int): BakedModel = parent
  }

}
