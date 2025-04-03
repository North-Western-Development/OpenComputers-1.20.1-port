package li.cil.oc.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Disassembler(state: container.Disassembler, playerInventory:Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  val progress = addCustomWidget(new ProgressBar(18, 65))

  override protected def renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
    guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040)
    drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY)

    for (slot <- 0 until menu.slots.size()) {
      drawSlotHighlight(guiGraphics, menu.getSlot(slot))
    }
  }

  override def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1, 1, 1, 1)
    guiGraphics.blit(Textures.GUI.Disassembler, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    progress.level = inventoryContainer.disassemblyProgress / 100.0
    drawWidgets(guiGraphics)
  }
}
