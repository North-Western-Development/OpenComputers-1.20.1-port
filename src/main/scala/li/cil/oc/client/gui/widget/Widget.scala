package li.cil.oc.client.gui.widget

import net.minecraft.client.gui.GuiGraphics

@Deprecated
abstract class Widget {
  var owner: WidgetContainer = _

  def x: Int

  def y: Int

  def width: Int

  def height: Int

  def draw(guiGraphics: GuiGraphics): Unit
}
