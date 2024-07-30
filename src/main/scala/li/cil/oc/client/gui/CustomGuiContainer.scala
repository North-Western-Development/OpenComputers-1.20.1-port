package li.cil.oc.client.gui

import java.util
import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.client.renderer.TooltipUtils
import li.cil.oc.client.gui.widget.WidgetContainer
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.Tesselator
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.locale.Language
import net.minecraft.network.chat.{Component, FormattedText, TextComponent}
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

import scala.collection.convert.ImplicitConversionsToScala._
import scala.jdk.CollectionConverters._

// Workaround because certain other mods *cough*TMI*cough* do base class
// transformations that break things! Such fun. Many annoyed. And yes, this
// is a common issue, have a look at EnderIO and Enchanting Plus. They have
// to work around this, too.
abstract class  CustomGuiContainer[C <: AbstractContainerMenu](val inventoryContainer: C, inv: Inventory, title: Component)
  extends AbstractContainerScreen[C](inventoryContainer, inv, title) with WidgetContainer {

  override def windowX = leftPos

  override def windowY = topPos

  override def windowZ: Float = getBlitOffset

  override def isPauseScreen = false

  protected def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  //TODO: FIX TOOLTIPS
  // Pretty much Scalaified copy-pasta from base-class.
  protected def renderTooltip(stack: PoseStack, tooltipS: java.util.ArrayList[String], x: Int, y: Int): Unit = {
    super.renderTooltip(stack, x, y)
    val tooltips: List[FormattedText] = tooltipS.asScala.toList.map(new TextComponent(_))

    TooltipUtils.drawTooltip(stack, tooltips, x, y)
  }

  protected def isPointInRegion(rectX: Int, rectY: Int, rectWidth: Int, rectHeight: Int, pointX: Int, pointY: Int): Boolean =
    pointX >= rectX - 1 && pointX < rectX + rectWidth + 1 && pointY >= rectY - 1 && pointY < rectY + rectHeight + 1

  override def fillGradient(stack: PoseStack, left: Int, top: Int, right: Int, bottom: Int, startColor: Int, endColor: Int): Unit = {
    super.fillGradient(stack, left, top, right, bottom, startColor, endColor)
    RenderState.makeItBlend()
  }

  override def render(stack: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.renderBackground(stack)
    super.render(stack, mouseX, mouseY, partialTicks)
    this.renderTooltip(stack, mouseX, mouseY)
  }
}
