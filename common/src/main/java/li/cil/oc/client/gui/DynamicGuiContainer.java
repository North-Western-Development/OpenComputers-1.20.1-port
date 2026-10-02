package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.client.Textures;
import li.cil.oc.common.container.ComponentSlot;
import li.cil.oc.common.container.Player;
import li.cil.oc.util.RenderState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class DynamicGuiContainer<C extends AbstractContainerMenu> extends CustomGuiContainer<C> {
    // TODO(port): integration (ItemSearch / JEI hovered stack); always empty for now.
    protected ItemStack hoveredStackNEI = ItemStack.EMPTY;

    protected DynamicGuiContainer(C container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        // imageHeight is set in the body of the extending class, so it's not available in ours.
        inventoryLabelY = imageHeight - 96 + 2;
    }

    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        RenderState.pushAttrib();

        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);

        for (int slot = 0; slot < menu.slots.size(); slot++) {
            drawSlotHighlight(graphics, menu.getSlot(slot));
        }

        RenderState.popAttrib();
    }

    protected void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(Textures.GUI.Background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawSecondaryBackgroundLayer(graphics);

        RenderState.makeItBlend();

        drawInventorySlots(graphics);
    }

    protected void drawInventorySlots(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0);
        RenderSystem.disableDepthTest();
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            drawSlotInventory(graphics, menu.getSlot(slot));
        }
        RenderSystem.enableDepthTest();
        graphics.pose().popPose();
        RenderState.makeItBlend();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        hoveredStackNEI = ItemStack.EMPTY;

        super.render(graphics, mouseX, mouseY, dt);
    }

    protected void drawSlotInventory(GuiGraphics graphics, Slot slot) {
        RenderSystem.enableBlend();
        if (slot instanceof ComponentSlot component && (component.slot().equals(li.cil.oc.common.Slot.None) || component.tier() == li.cil.oc.common.Tier.None)) {
            if (!slot.hasItem() && slot.x >= 0 && slot.y >= 0 && component.tierIcon() != null) {
                drawDisabledSlot(graphics, component);
            }
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 1);
            if (!isInPlayerInventory(slot)) {
                drawSlotBackground(graphics, slot.x - 1, slot.y - 1);
            }
            if (!slot.hasItem()) {
                if (slot instanceof ComponentSlot component) {
                    if (component.tierIcon() != null) {
                        graphics.blit(component.tierIcon(), slot.x, slot.y, 0, 0, 16, 16, 16, 16);
                    }
                    if (component.hasBackground()) {
                        graphics.blit(component.getBackgroundLocation(), slot.x, slot.y, 0, 0, 16, 16, 16, 16);
                    }
                }
            }
            graphics.pose().popPose();
        }
        RenderSystem.disableBlend();
    }

    protected void drawSlotHighlight(GuiGraphics graphics, Slot slot) {
        if (!menu.getCarried().isEmpty()) return;
        if (slot instanceof ComponentSlot component && (component.slot().equals(li.cil.oc.common.Slot.None) || component.tier() == li.cil.oc.common.Tier.None)) {
            return; // Ignore.
        }
        final boolean currentIsInPlayerInventory = isInPlayerInventory(slot);
        final boolean drawHighlight;
        if (hoveredSlot != null) {
            final Slot hovered = hoveredSlot;
            final boolean hoveredIsInPlayerInventory = isInPlayerInventory(hovered);
            drawHighlight = (currentIsInPlayerInventory != hoveredIsInPlayerInventory) &&
                    ((currentIsInPlayerInventory && slot.hasItem() && isSelectiveSlot(hovered) && hovered.mayPlace(slot.getItem())) ||
                            (hoveredIsInPlayerInventory && hovered.hasItem() && isSelectiveSlot(slot) && slot.mayPlace(hovered.getItem())));
        } else if (!hoveredStackNEI.isEmpty()) {
            drawHighlight = !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.mayPlace(hoveredStackNEI);
        } else {
            drawHighlight = false;
        }
        if (drawHighlight) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 100);
            graphics.fillGradient(
                    slot.x, slot.y,
                    slot.x + 16, slot.y + 16,
                    0x80FFFFFF, 0x80FFFFFF);
            graphics.pose().popPose();
        }
    }

    private boolean isSelectiveSlot(Slot slot) {
        if (slot instanceof ComponentSlot component) {
            return !component.slot().equals(li.cil.oc.common.Slot.Any) && !component.slot().equals(li.cil.oc.common.Slot.Tool);
        }
        return false;
    }

    protected void drawDisabledSlot(GuiGraphics graphics, ComponentSlot slot) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(slot.tierIcon(), slot.x, slot.y, 0, 0, 16, 16, 16, 16);
    }

    protected void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(Textures.GUI.Slot, x, y, 0, 0, 18, 18, 18, 18);
    }

    private boolean isInPlayerInventory(Slot slot) {
        if (inventoryContainer instanceof Player player) {
            return slot.container == player.playerInventory;
        }
        return false;
    }
}
