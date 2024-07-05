package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Printer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.model.ItemCameraTransforms
import net.minecraft.client.renderer.tileentity.BlockEntityRenderer
import net.minecraft.client.renderer.tileentity.BlockEntityRendererDispatcher
import com.mojang.math.Vector3f
import org.lwjgl.opengl.GL13

object PrinterRenderer extends Function[BlockEntityRendererDispatcher, PrinterRenderer] {
  override def apply(dispatch: BlockEntityRendererDispatcher) = new PrinterRenderer(dispatch)
}

class PrinterRenderer(dispatch: BlockEntityRendererDispatcher) extends BlockEntityRenderer[Printer](dispatch) {
  override def render(printer: Printer, dt: Float, matrix: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (printer.data.stateOff.nonEmpty) {
      val stack = printer.data.createItemStack()

      matrix.pushPose()
      matrix.translate(0.5, 0.5 + 0.3, 0.5)

      matrix.mulPose(Vector3f.YP.rotationDegrees((System.currentTimeMillis() % 20000) / 20000f * 360))
      matrix.scale(0.75f, 0.75f, 0.75f)

      Textures.Block.bind()
      Minecraft.getInstance.getItemRenderer.renderStatic(stack, ItemCameraTransforms.TransformType.FIXED, light, overlay, matrix, buffer)

      matrix.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
