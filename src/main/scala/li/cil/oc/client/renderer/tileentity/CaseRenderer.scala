package li.cil.oc.client.renderer.tileentity

import java.util.function.Function
import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import com.mojang.math.Vector3f
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}

object CaseRenderer extends Function[BlockEntityRendererProvider.Context, CaseRenderer] {
  override def apply(ctx: BlockEntityRendererProvider.Context) = new CaseRenderer(ctx)
}

class CaseRenderer(ctx: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Case](ctx) {
  override def render(computer: Case, dt: Float, stack: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    computer.yaw match {
      case Direction.WEST => stack.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => stack.mulPose(Vector3f.YP.rotationDegrees(90))
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
