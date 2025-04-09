package li.cil.oc.client.gui.traits

import li.cil.oc.util.RenderState
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen

trait DisplayBuffer extends Screen {
  protected def bufferX: Int

  protected def bufferY: Int

  protected def bufferColumns: Int

  protected def bufferRows: Int

  protected var scale = 0.0

  protected def drawBufferLayer(guiGraphics: GuiGraphics) {
    scale = changeSize(bufferColumns, bufferRows)

    RenderState.checkError(getClass.getName + ".drawBufferLayer: entering (aka: wasntme)")
    val stack = guiGraphics.pose()
    stack.pushPose()
    drawBuffer(guiGraphics)
    stack.popPose()

    RenderState.checkError(getClass.getName + ".drawBufferLayer: buffer layer")
  }

  protected def drawBuffer(stack: GuiGraphics): Unit

  protected def changeSize(w: Double, h: Double): Double
}
