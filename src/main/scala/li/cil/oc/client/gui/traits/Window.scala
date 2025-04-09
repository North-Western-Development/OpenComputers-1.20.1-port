package li.cil.oc.client.gui.traits

import java.util
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

trait Window extends Screen {
  var leftPos = 0
  var topPos = 0
  var imageWidth = 0
  var imageHeight = 0

  val windowWidth = 176
  val windowHeight = 166

  def backgroundImage: ResourceLocation

  override def isPauseScreen = false

  override protected def init(): Unit = {
    super.init()

    imageWidth = windowWidth
    imageHeight = windowHeight
    leftPos = (width - imageWidth) / 2
    topPos = (height - imageHeight) / 2
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    // Texture width and height are intentionally backwards.
    guiGraphics.blit(backgroundImage, leftPos, topPos, getBlitOffset, 0, 0, imageWidth, imageHeight, windowHeight, windowWidth)

    super.render(guiGraphics, mouseX, mouseY, dt)
  }

}
