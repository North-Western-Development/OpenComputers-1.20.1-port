package li.cil.oc.client.gui.traits

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.screens.Screen

trait DisplayBuffer extends Screen {
  protected def bufferX: Int

  protected def bufferY: Int

  protected def bufferColumns: Int

  protected def bufferRows: Int

  protected var scale = 0.0

  protected def drawBufferLayer(stack: PoseStack): Unit = {
    scale = changeSize(bufferColumns, bufferRows)

    RenderState.checkError(getClass.getName + ".drawBufferLayer: entering (aka: wasntme)")

    stack.pushPose()
    drawBuffer(stack)
    stack.popPose()

    RenderState.checkError(getClass.getName + ".drawBufferLayer: buffer layer")
  }

  protected def drawBuffer(stack: PoseStack): Unit

  protected def changeSize(w: Double, h: Double): Double
}
