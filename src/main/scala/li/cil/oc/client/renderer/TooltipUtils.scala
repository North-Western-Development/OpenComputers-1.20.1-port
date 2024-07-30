package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.{FormattedText, Style}
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.ForgeHooksClient

import scala.collection.convert.ImplicitConversions.`collection asJava`
import scala.jdk.CollectionConverters._

object TooltipUtils {
  def drawTooltip(poseStack: PoseStack, tooltip: List[FormattedText], x: Int, y: Int): Unit = {
    drawTooltip(poseStack, tooltip, x, y, 200, ItemStack.EMPTY)
  }

  def drawTooltip(poseStack: PoseStack, tooltip: List[FormattedText], x: Int, y: Int, widthHint: Int): Unit = {
    drawTooltip(poseStack, tooltip, x, y, widthHint, ItemStack.EMPTY)
  }

  def drawTooltip(poseStack: PoseStack, tooltip: List[FormattedText], x: Int, y: Int, widthHint: Int, itemStack: ItemStack): Unit = {
    val minecraft = Minecraft.getInstance
    val screen = minecraft.screen
    if (screen == null) return
    val availableWidth = Math.max(x, screen.width - x)
    val targetWidth = Math.min(availableWidth, widthHint)
    val font = ForgeHooksClient.getTooltipFont(null, itemStack, minecraft.font)
    val needsWrapping = tooltip.stream.anyMatch((line) => font.width(line) > targetWidth)
    if (!needsWrapping) screen.renderComponentTooltip(poseStack, tooltip.asJava, x, y, font, itemStack)
    else {
      val splitter = font.getSplitter
      val wrappedTooltip = tooltip.stream.flatMap((line) => splitter.splitLines(line, targetWidth, Style.EMPTY).stream).toList
      screen.renderComponentTooltip(poseStack, wrappedTooltip, x, y, font, itemStack)
    }
  }
}