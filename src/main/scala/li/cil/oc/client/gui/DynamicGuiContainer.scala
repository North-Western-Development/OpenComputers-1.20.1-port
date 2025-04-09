package li.cil.oc.client.gui

import com.mojang.blaze3d.vertex.{DefaultVertexFormat, PoseStack, Tesselator, VertexFormat}
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.container.Player
import li.cil.oc.integration.Mods
import li.cil.oc.integration.jei.ModJEI
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.RenderState
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.gui.{AbstractGui, GuiGraphics}
import net.minecraft.world.entity.player.Inventory
import net.minecraft.inventory.container.Container
import net.minecraft.inventory.container.Slot
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot
import org.lwjgl.opengl.GL11

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

abstract class DynamicGuiContainer[C <: Container](container: C, inv:Inventory, title: Component)
  extends CustomGuiContainer(container, inv, title) {

  protected var hoveredStackNEI: StackOption = EmptyStack

  override protected def init() {
    super.init()
    // imageHeight is set in the body of the extending class, so it's not available in ours.
    inventoryLabelY = imageHeight - 96 + 2
  }

  protected def drawSecondaryForegroundLayer(stack: GuiGraphics, mouseX: Int, mouseY: Int) {}

  override protected def renderLabels(stack: GuiGraphics, mouseX: Int, mouseY: Int) {
    super.renderLabels(stack, mouseX, mouseY)
    RenderState.pushAttrib()

    drawSecondaryForegroundLayer(stack, mouseX, mouseY)

    for (slot <- 0 until menu.slots.size()) {
      drawSlotHighlight(stack, menu.getSlot(slot))
    }

    RenderState.popAttrib()
  }

  protected def drawSecondaryBackgroundLayer(stack: GuiGraphics) {}

  override protected def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Background)
    guiGraphics.blit(Textures.GUI.Background, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    drawSecondaryBackgroundLayer(guiGraphics)

    RenderState.makeItBlend()
    RenderSystem.disableLighting()

    drawInventorySlots(guiGraphics)
  }

  protected def drawInventorySlots(guiGraphics: GuiGraphics): Unit = {
    val stack = guiGraphics.pose()
    stack.pushPose()
    stack.translate(leftPos, topPos, 0)
    RenderSystem.disableDepthTest()
    for (slot <- 0 until menu.slots.size()) {
      drawSlotInventory(stack, menu.getSlot(slot))
    }
    RenderSystem.enableDepthTest()
    stack.popPose()
    RenderState.makeItBlend()
  }

  override def render(stack: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float) {
    hoveredStackNEI = ItemSearch.hoveredStack(this, mouseX, mouseY)

    super.render(stack, mouseX, mouseY, dt)
  }

  protected def drawSlotInventory(guiGraphics: GuiGraphics, slot: Slot) {
    RenderSystem.enableBlend()
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None =>
        if (!slot.hasItem && slot.x >= 0 && slot.y >= 0 && component.tierIcon != null) {
          drawDisabledSlot(guiGraphics, component)
        }
      case _ =>
        setBlitOffset(getBlitOffset + 1)
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(guiGraphics, slot.x - 1, slot.y - 1)
        }
        if (!slot.hasItem) {
          slot match {
            case component: ComponentSlot =>
              if (component.tierIcon != null) {
                guiGraphics.blit(component.tierIcon, slot.x, slot.y, getBlitOffset, 0, 0, 16, 16, 16, 16)
              }
              if (component.hasBackground) {
                guiGraphics.blit(component.getBackgroundLocation, slot.x, slot.y, getBlitOffset, 0, 0, 16, 16, 16, 16)
              }
            case _ =>
          }
          setBlitOffset(getBlitOffset - 1)
        }
    }
    RenderSystem.disableBlend()
  }

  protected def drawSlotHighlight(matrix: GuiGraphics, slot: Slot) {
    if (minecraft.player.getInventory.getCarried.isEmpty) slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None => // Ignore.
      case _ =>
        val currentIsInPlayerInventory = isInPlayerInventory(slot)
        val drawHighlight = hoveredSlot match {
          case hovered: Slot =>
            val hoveredIsInPlayerInventory = isInPlayerInventory(hovered)
            (currentIsInPlayerInventory != hoveredIsInPlayerInventory) &&
              ((currentIsInPlayerInventory && slot.hasItem && isSelectiveSlot(hovered) && hovered.mayPlace(slot.getItem)) ||
                (hoveredIsInPlayerInventory && hovered.hasItem && isSelectiveSlot(slot) && slot.mayPlace(hovered.getItem)))
          case _ => hoveredStackNEI match {
            case SomeStack(stack) => !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.mayPlace(stack)
            case _ => false
          }
        }
        if (drawHighlight) {
          setBlitOffset(getBlitOffset + 100)
          fillGradient(matrix,
            slot.x, slot.y,
            slot.x + 16, slot.y + 16,
            0x80FFFFFF, 0x80FFFFFF)
          setBlitOffset(getBlitOffset - 100)
        }
    }
  }

  private def isSelectiveSlot(slot: Slot) = slot match {
    case component: ComponentSlot => component.slot != common.Slot.Any && component.slot != common.Slot.Tool
    case _ => false
  }

  protected def drawDisabledSlot(guiGraphics: GuiGraphics, slot: ComponentSlot) {
    RenderSystem.setShaderColor(1, 1, 1, 1)
    guiGraphics.blit(slot.tierIcon, slot.x, slot.y, getBlitOffset, 0, 0, 16, 16, 16, 16)
  }

  protected def drawSlotBackground(stack: GuiGraphics, x: Int, y: Int) {
    RenderSystem.setShaderColor(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Slot)
    val t = Tesselator.getInstance
    val r = t.getBuilder
    r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
    r.vertex(stack.last.pose, x, y + 18, getBlitOffset + 1).uv(0, 1).endVertex()
    r.vertex(stack.last.pose, x + 18, y + 18, getBlitOffset + 1).uv(1, 1).endVertex()
    r.vertex(stack.last.pose, x + 18, y, getBlitOffset + 1).uv(1, 0).endVertex()
    r.vertex(stack.last.pose, x, y, getBlitOffset + 1).uv(0, 0).endVertex()
    t.end()
  }

  private def isInPlayerInventory(slot: Slot) = container match {
    case player: Player => slot.container == player.playerInventory
    case _ => false
  }
}
