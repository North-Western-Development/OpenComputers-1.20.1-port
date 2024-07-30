package li.cil.oc.client.gui

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.entity.player.Inventory

import scala.collection.JavaConverters.asJavaCollection
import scala.collection.convert.ImplicitConversionsToJava._

class Case(state: container.Case, playerInventory: Inventory, name: TextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {

  protected var powerButton: ImageButton = _

  override def render(stack: PoseStack, mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = inventoryContainer.isRunning
    super.render(stack, mouseX, mouseY, dt)
  }

  override protected def init() {
    super.init()
    powerButton = new ImageButton(leftPos + 70, topPos + 33, 18, 18, (b: Button) => ClientPacketSender.sendComputerPower(inventoryContainer, !inventoryContainer.isRunning), Textures.GUI.ButtonPower, canToggle = true)
    addRenderableWidget(powerButton)
  }

  override protected def drawSecondaryForegroundLayer(stack: PoseStack, mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(stack, mouseX, mouseY)
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(if (inventoryContainer.isRunning) Localization.Computer.TurnOff.linesIterator.toIterable else Localization.Computer.TurnOn.linesIterator.toIterable))
      this.renderTooltip(stack, tooltip, mouseX - leftPos, mouseY - topPos)
    }
  }

  override def drawSecondaryBackgroundLayer(stack: PoseStack) {
    RenderSystem.setShaderColor(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Computer)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
