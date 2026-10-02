package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.widget.ProgressBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Disassembler extends DynamicGuiContainer<li.cil.oc.common.container.Disassembler> {
    public final ProgressBar progress = addCustomWidget(new ProgressBar(18, 65));

    public Disassembler(li.cil.oc.common.container.Disassembler state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);

        for (int slot = 0; slot < menu.slots.size(); slot++) {
            drawSlotHighlight(graphics, menu.getSlot(slot));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(Textures.GUI.Disassembler, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        progress.level = inventoryContainer.disassemblyProgress() / 100.0;
        drawWidgets(graphics);
    }
}
