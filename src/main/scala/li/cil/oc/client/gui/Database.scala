package li.cil.oc.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.{Tier, container}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Database(state: container.Database, playerInventory:Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[container.Database] {

  imageHeight = 256

  override def lockedStack = inventoryContainer.container

  override protected def renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) =
    drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY)

  override def drawSecondaryForegroundLayer(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {}

  override protected def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1, 1, 1, 1)
    guiGraphics.blit(Textures.GUI.Database, leftPos, topPos, 0, 0, imageWidth, imageHeight)

    if (inventoryContainer.tier > Tier.One) {
      guiGraphics.blit(Textures.GUI.Database1, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }

    if (inventoryContainer.tier > Tier.Two) {
      guiGraphics.blit(Textures.GUI.Database2, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }
  }
}
