package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import com.mojang.math.Axis
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation

import java.util.function.Function

object CaseRenderer extends Function[BlockRenderDispatcher, CaseRenderer] {
  override def apply(dispatch: BlockRenderDispatcher) = new CaseRenderer(dispatch)
}

class CaseRenderer(dispatch: BlockRenderDispatcher) extends BlockEntityRenderer[Case](dispatch) {
  override def render(computer: Case, dt: Float, stack: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    computer.yaw match {
      case Direction.WEST => stack.mulPose(Axis.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Axis.YP.rotationDegrees(180))
      case Direction.EAST => stack.mulPose(Axis.YP.rotationDegrees(90))
      case _ => // No yaw.
    }

    stack.translate(-0.5, 0.5, 0.505)
    stack.scale(1, -1, 1)

    if (computer.isRunning) {
      renderFrontOverlay(stack, Textures.Block.CaseFrontOn, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY))
      if (System.currentTimeMillis() - computer.lastFileSystemAccess < 400 && computer.world.random.nextDouble() > 0.1) {
        renderFrontOverlay(stack, Textures.Block.CaseFrontActivity, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY))
      }
    }
    else if (computer.hasErrored && RenderUtil.shouldShowErrorLight(computer.hashCode)) {
      renderFrontOverlay(stack, Textures.Block.CaseFrontError, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY))
    }

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def renderFrontOverlay(stack: PoseStack, texture: ResourceLocation, r: VertexConsumer): Unit = {
    val icon = Textures.getSprite(texture)
    r.vertex(stack.last.pose, 0, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
    r.vertex(stack.last.pose, 1, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
    r.vertex(stack.last.pose, 1, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
    r.vertex(stack.last.pose, 0, 0, 0).uv(icon.getU0, icon.getV0).endVertex()
  }
}
