package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.common.container.ComponentSlot;
import li.cil.oc.util.RenderState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class Printer extends DynamicGuiContainer<li.cil.oc.common.container.Printer> {
    private final ProgressBar materialBar = addCustomWidget(new ProgressBar(40, 21) {
        @Override
        public int width() {
            return 62;
        }

        @Override
        public int height() {
            return 12;
        }

        @Override
        public ResourceLocation barTexture() {
            return Textures.GUI.PrinterMaterial;
        }
    });
    private final ProgressBar inkBar = addCustomWidget(new ProgressBar(40, 53) {
        @Override
        public int width() {
            return 62;
        }

        @Override
        public int height() {
            return 12;
        }

        @Override
        public ResourceLocation barTexture() {
            return Textures.GUI.PrinterInk;
        }
    });
    private final ProgressBar progressBar = addCustomWidget(new ProgressBar(105, 20) {
        @Override
        public int width() {
            return 46;
        }

        @Override
        public int height() {
            return 46;
        }

        @Override
        public ResourceLocation barTexture() {
            return Textures.GUI.PrinterProgress;
        }
    });

    public Printer(li.cil.oc.common.container.Printer state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        RenderState.pushAttrib();
        if (isPointInRegion(materialBar.x, materialBar.y, materialBar.width(), materialBar.height(), mouseX - leftPos, mouseY - topPos)) {
            final List<String> tooltip = new ArrayList<>();
            tooltip.add(inventoryContainer.amountMaterial() + "/" + inventoryContainer.maxAmountMaterial());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
        if (isPointInRegion(inkBar.x, inkBar.y, inkBar.width(), inkBar.height(), mouseX - leftPos, mouseY - topPos)) {
            final List<String> tooltip = new ArrayList<>();
            tooltip.add(inventoryContainer.amountInk() + "/" + inventoryContainer.maxAmountInk());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
        RenderState.popAttrib();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(Textures.GUI.Printer, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        materialBar.level = inventoryContainer.amountMaterial() / (double) inventoryContainer.maxAmountMaterial();
        inkBar.level = inventoryContainer.amountInk() / (double) inventoryContainer.maxAmountInk();
        progressBar.level = inventoryContainer.progress();
        drawWidgets(graphics);
        drawInventorySlots(graphics);
    }

    @Override
    protected void drawDisabledSlot(GuiGraphics graphics, ComponentSlot slot) {
    }
}
