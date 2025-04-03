package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity.Charger
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction

import java.util.function.Function

object ChargerRenderer extends Function[BlockRenderDispatcher, ChargerRenderer] {
  override def apply(dispatch: BlockRenderDispatcher) = new ChargerRenderer(dispatch)
}

class ChargerRenderer(dispatch: BlockRenderDispatcher) extends BlockEntityRenderer[Charger](dispatch) {
  override def render(charger: Charger, dt: Float, stack: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.setShaderColor(1, 1, 1, 1)

    if (charger.chargeSpeed > 0) {
      stack.pushPose()

      stack.translate(0.5, 0.5, 0.5)

      charger.yaw match {
        case Direction.WEST => stack.mulPose(Axis.YP.rotationDegrees(-90))
        case Direction.NORTH => stack.mulPose(Axis.YP.rotationDegrees(180))
        case Direction.EAST => stack.mulPose(Axis.YP.rotationDegrees(90))
        case _ => // No yaw.
      }

      stack.translate(-0.5f, 0.5f, 0.5f)
      stack.scale(1, -1, 1)

      val r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY)

      {
        val inverse = 1 - charger.chargeSpeed.toFloat
        val icon = Textures.getSprite(Textures.Block.ChargerFrontOn)
        r.vertex(stack.last.pose, 0, 1, 0.005f).uv(icon.getU0, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 1, 1, 0.005f).uv(icon.getU1, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 1, inverse, 0.005f).uv(icon.getU1, icon.getV(inverse * 16)).endVertex()
        r.vertex(stack.last.pose, 0, inverse, 0.005f).uv(icon.getU0, icon.getV(inverse * 16)).endVertex()
      }

      if (charger.hasPower) {
        val icon = Textures.getSprite(Textures.Block.ChargerSideOn)

        r.vertex(stack.last.pose, -0.005f, 1, -1).uv(icon.getU0, icon.getV1).endVertex()
        r.vertex(stack.last.pose, -0.005f, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
        r.vertex(stack.last.pose, -0.005f, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
        r.vertex(stack.last.pose, -0.005f, 0, -1).uv(icon.getU0, icon.getV0).endVertex()

        r.vertex(stack.last.pose, 1, 1, -1.005f).uv(icon.getU0, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 1, -1.005f).uv(icon.getU1, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 0, -1.005f).uv(icon.getU1, icon.getV0).endVertex()
        r.vertex(stack.last.pose, 1, 0, -1.005f).uv(icon.getU0, icon.getV0).endVertex()

        r.vertex(stack.last.pose, 1.005f, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 1.005f, 1, -1).uv(icon.getU1, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 1.005f, 0, -1).uv(icon.getU1, icon.getV0).endVertex()
        r.vertex(stack.last.pose, 1.005f, 0, 0).uv(icon.getU0, icon.getV0).endVertex()
      }

      stack.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
