package li.cil.oc.client.gui.widget

import com.mojang.blaze3d.vertex.PoseStack

@Deprecated
abstract class Widget {
  var owner: WidgetContainer = _

  def x: Int

  def y: Int

  def width: Int

  def height: Int

  def draw(stack: PoseStack): Unit
}
