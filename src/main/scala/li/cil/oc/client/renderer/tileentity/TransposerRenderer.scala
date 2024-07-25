package li.cil.oc.client.renderer.tileentity

import java.util.function.Function
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}

object TransposerRenderer extends Function[BlockEntityRendererProvider.Context, TransposerRenderer] {
  override def apply(ctx: BlockEntityRendererProvider.Context) = new TransposerRenderer(ctx)
}

class TransposerRenderer(ctx: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[tileentity.Transposer] {
  override def render(transposer: tileentity.Transposer, dt: Float, stack: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.setShaderColor(1, 1, 1, 1)

    val activity = math.max(0, 1 - (System.currentTimeMillis() - transposer.lastOperation) / 1000.0f)
    if (activity > 0) {
      stack.pushPose()

      stack.translate(0.5, 0.5, 0.5)
      stack.scale(1.0025f, -1.0025f, 1.0025f)
      stack.translate(-0.5f, -0.5f, -0.5f)

      val r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY_COLOR)

      val icon = Textures.getSprite(Textures.Block.TransposerOn)
      r.vertex(stack.last.pose, 0, 1, 0).color(1, 1, 1, activity).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 1, 0).color(1, 1, 1, activity).uv(icon.getU0, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 1, 1).color(1, 1, 1, activity).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 1, 1).color(1, 1, 1, activity).uv(icon.getU1, icon.getV1).endVertex()

      r.vertex(stack.last.pose, 0, 0, 0).color(1, 1, 1, activity).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 0, 1).color(1, 1, 1, activity).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 1).color(1, 1, 1, activity).uv(icon.getU0, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 0).color(1, 1, 1, activity).uv(icon.getU0, icon.getV1).endVertex()

      r.vertex(stack.last.pose, 1, 1, 0).color(1, 1, 1, activity).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 1, 0).color(1, 1, 1, activity).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 0, 0).color(1, 1, 1, activity).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 0).color(1, 1, 1, activity).uv(icon.getU0, icon.getV0).endVertex()

      r.vertex(stack.last.pose, 0, 1, 1).color(1, 1, 1, activity).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 1, 1).color(1, 1, 1, activity).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 0, 1).color(1, 1, 1, activity).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 0, 0, 1).color(1, 1, 1, activity).uv(icon.getU0, icon.getV0).endVertex()

      r.vertex(stack.last.pose, 0, 1, 0).color(1, 1, 1, activity).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 1, 1).color(1, 1, 1, activity).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 0, 1).color(1, 1, 1, activity).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 0, 0, 0).color(1, 1, 1, activity).uv(icon.getU0, icon.getV0).endVertex()

      r.vertex(stack.last.pose, 1, 1, 1).color(1, 1, 1, activity).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 1, 0).color(1, 1, 1, activity).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 0, 0).color(1, 1, 1, activity).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 1).color(1, 1, 1, activity).uv(icon.getU0, icon.getV0).endVertex()

      stack.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
