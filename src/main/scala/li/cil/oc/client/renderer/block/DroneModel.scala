package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.model.BakedQuad
import net.minecraft.client.renderer.model.IBakedModel
import net.minecraft.client.renderer.model.ItemOverrideList
import net.minecraft.client.world.ClientLevel
import net.minecraft.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object DroneModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: util.Random): util.List[BakedQuad] = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    faces ++= Boxes.flatMap(box => bakeQuads(box, Array.fill(6)(droneTexture), None).toSeq)

    faces.asJava
  }

  protected def droneTexture = Textures.getSprite(Textures.Item.DroneItem)

  protected def Boxes = Array(
    makeBox(new Vec3(1f / 16f, 7f / 16f, 1f / 16f), new Vec3(7f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vec3(1f / 16f, 7f / 16f, 9f / 16f), new Vec3(7f / 16f, 8f / 16f, 15f / 16f)),
    makeBox(new Vec3(9f / 16f, 7f / 16f, 1f / 16f), new Vec3(15f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vec3(9f / 16f, 7f / 16f, 9f / 16f), new Vec3(15f / 16f, 8f / 16f, 15f / 16f)),
    rotateBox(makeBox(new Vec3(6f / 16f, 6f / 16f, 6f / 16f), new Vec3(10f / 16f, 9f / 16f, 10f / 16f)), 45)
  )

  object ItemOverride extends ItemOverrideList {
    override def resolve(originalModel: IBakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity): IBakedModel = DroneModel
  }

}
