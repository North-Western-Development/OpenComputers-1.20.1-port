package li.cil.oc.client.gui.widget

import net.minecraft.client.gui.GuiGraphics

import scala.collection.mutable

@Deprecated
trait WidgetContainer {
  protected val widgets = mutable.ArrayBuffer.empty[Widget]

  def addCustomWidget[T <: Widget](widget: T) = {
    widgets += widget
    widget.owner = this
    widget
  }

  def windowX = 0

  def windowY = 0

  def windowZ = 0f

  def drawWidgets(guiGraphics: GuiGraphics) {
    widgets.foreach(_.draw(guiGraphics))
  }
}
