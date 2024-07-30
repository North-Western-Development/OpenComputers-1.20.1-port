package li.cil.oc.client.gui

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Button.OnPress
import net.minecraft.network.chat.{Component, TextComponent}
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

class Assembler(state: container.Assembler, playerInventory: Inventory, name: TextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {

  imageWidth = 176
  imageHeight = 192

  for (slot <- menu.slots) slot match {
    case component: ComponentSlot => component.changeListener = Option(onSlotChanged)
    case _ =>
  }

  private def onSlotChanged(slot: Slot) {
    runButton.active = canBuild
    runButton.toggled = !runButton.active
    info = validate
  }

  var info: Option[(Boolean, Component, Array[Component])] = None

  protected var runButton: ImageButton = _

  private val progress = addCustomWidget(new ProgressBar(28, 92))

  private def validate = AssemblerTemplates.select(inventoryContainer.getSlot(0).getItem).map(_.validate(inventoryContainer.otherInventory))

  private def canBuild = !inventoryContainer.isAssembling && validate.exists(_._1)

  override protected def init() {
    super.init()
    runButton = new ImageButton(leftPos + 7, topPos + 89, 18, 18, (b: Button) => if (canBuild) ClientPacketSender.sendRobotAssemblerStart(inventoryContainer), Textures.GUI.ButtonRun, canToggle = true)
    addRenderableWidget(runButton)
  }

  override protected def renderLabels(stack: PoseStack, mouseX: Int, mouseY: Int) {
    drawSecondaryForegroundLayer(stack, mouseX, mouseY)

    for (slot <- 0 until menu.slots.size()) {
      drawSlotHighlight(stack, menu.getSlot(slot))
    }
  }

  override def drawSecondaryForegroundLayer(stack: PoseStack, mouseX: Int, mouseY: Int): Unit = {
    RenderState.pushAttrib()
    if (!inventoryContainer.isAssembling) {
      val message =
        if (!inventoryContainer.getSlot(0).hasItem) {
          Localization.Assembler.InsertTemplate
        }
        else info match {
          case Some((_, value, _)) if value != null => value.getString
          case _ if inventoryContainer.getSlot(0).hasItem => Localization.Assembler.CollectResult
          case _ => ""
        }
      font.draw(stack, message, 30, 94, 0x404040)
      if (runButton.isMouseOver(mouseX, mouseY)) {
        val tooltip = new java.util.ArrayList[String]
        tooltip.add(Localization.Assembler.Run)
        info.foreach {
          case (valid, _, warnings) => if (valid && warnings.length > 0) {
            tooltip.addAll(warnings.map(_.getString).toList)
          }
        }
        this.renderTooltip(stack, tooltip, mouseX - leftPos, mouseY - topPos)
      }
    }
    else if (isPointInRegion(progress.x, progress.y, progress.width, progress.height, mouseX - leftPos, mouseY - topPos)) {
      val tooltip = new java.util.ArrayList[String]
      val timeRemaining = formatTime(inventoryContainer.assemblyRemainingTime)
      tooltip.add(Localization.Assembler.Progress(inventoryContainer.assemblyProgress, timeRemaining))
      this.renderTooltip(stack, tooltip, mouseX - leftPos, mouseY - topPos)
    }
    RenderState.popAttrib()
  }

  private def formatTime(seconds: Int) = {
    // Assembly times should not / rarely exceed one hour, so this is good enough.
    if (seconds < 60) f"0:$seconds%02d"
    else f"${seconds / 60}:${seconds % 60}%02d"
  }

  override protected def renderBg(stack: PoseStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1, 1, 1, 1) // Required under Linux.
    Textures.bind(Textures.GUI.RobotAssembler)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    if (inventoryContainer.isAssembling) progress.level = inventoryContainer.assemblyProgress / 100.0
    else progress.level = 0
    drawWidgets(stack)
    drawInventorySlots(stack)
  }

  override protected def drawDisabledSlot(stack: PoseStack, slot: ComponentSlot) {}
}
