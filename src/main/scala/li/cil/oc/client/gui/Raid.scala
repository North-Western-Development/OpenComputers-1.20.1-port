package li.cil.oc.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Raid(state: container.Raid, playerInventory:Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  override def renderBg(guiTypes: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1, 1, 1, 1) // Required under Linux.
    guiTypes.blit(Textures.GUI.Raid, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
