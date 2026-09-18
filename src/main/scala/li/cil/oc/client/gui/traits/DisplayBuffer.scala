package li.cil.oc.client.gui.traits

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.MultiBufferSource

trait DisplayBuffer extends Screen {
  protected def bufferX: Int

  protected def bufferY: Int

  protected def bufferColumns: Int

  protected def bufferRows: Int

  protected var scale = 0.0

  protected def drawBufferLayer(stack: PoseStack, buffer: MultiBufferSource): Unit = {
    scale = changeSize(bufferColumns, bufferRows)

    RenderState.checkError(getClass.getName + ".drawBufferLayer: entering (aka: wasntme)")

    stack.pushPose()
    drawBuffer(stack, buffer)
    stack.popPose()

    RenderState.checkError(getClass.getName + ".drawBufferLayer: buffer layer")
  }

  protected def drawBuffer(stack: PoseStack, buffer: MultiBufferSource): Unit

  protected def changeSize(w: Double, h: Double): Double
}
