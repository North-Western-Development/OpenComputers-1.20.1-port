package li.cil.oc.client.renderer.block

import li.cil.oc.client.Textures
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides}
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d

import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object DroneModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: RandomSource): util.List[BakedQuad] = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    faces ++= Boxes.flatMap(box => bakeQuads(box, Array.fill(6)(droneTexture), None).toSeq)

    faces.asJava
  }

  protected def droneTexture = Textures.getSprite(Textures.Item.DroneItem)

  protected def Boxes = Array(
    makeBox(new Vector3d(1f / 16f, 7f / 16f, 1f / 16f), new Vector3d(7f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vector3d(1f / 16f, 7f / 16f, 9f / 16f), new Vector3d(7f / 16f, 8f / 16f, 15f / 16f)),
    makeBox(new Vector3d(9f / 16f, 7f / 16f, 1f / 16f), new Vector3d(15f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vector3d(9f / 16f, 7f / 16f, 9f / 16f), new Vector3d(15f / 16f, 8f / 16f, 15f / 16f)),
    rotateBox(makeBox(new Vector3d(6f / 16f, 6f / 16f, 6f / 16f), new Vector3d(10f / 16f, 9f / 16f, 10f / 16f)), 45)
  )

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity, entity_id: Int): BakedModel = DroneModel
  }

}
