package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Printer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.world.item.ItemDisplayContext

import java.util.function.Function

object PrinterRenderer extends BlockEntityRendererProvider[Printer] {
  override def create(dispatch: BlockEntityRendererProvider.Context): BlockEntityRenderer[Printer] = new PrinterRenderer(dispatch.getBlockRenderDispatcher)
}

class PrinterRenderer(dispatch: BlockRenderDispatcher) extends BlockEntityRenderer[Printer](dispatch) {
  override def render(printer: Printer, dt: Float, matrix: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (printer.data.stateOff.nonEmpty) {
      val stack = printer.data.createItemStack()

      matrix.pushPose()
      matrix.translate(0.5, 0.5 + 0.3, 0.5)

      matrix.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % 20000) / 20000f * 360))
      matrix.scale(0.75f, 0.75f, 0.75f)

      Textures.Block.bind()
      Minecraft.getInstance.getItemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, matrix, buffer)

      matrix.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
