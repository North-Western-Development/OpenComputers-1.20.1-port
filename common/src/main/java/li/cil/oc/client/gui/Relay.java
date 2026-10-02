package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;

public class Relay extends DynamicGuiContainer<li.cil.oc.common.container.Relay> {
    private final DecimalFormat format = new DecimalFormat("#.##hz");

    public final Rect2i tabPosition;

    public Relay(li.cil.oc.common.container.Relay state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        tabPosition = new Rect2i(imageWidth, 10, 23, 26);
    }

    @Override
    protected void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        super.drawSecondaryBackgroundLayer(graphics);

        // Tab background.
        RenderSystem.setShaderColor(1, 1, 1, 1);
        final int x = windowX() + tabPosition.getX();
        final int y = windowY() + tabPosition.getY();
        final int w = tabPosition.getWidth();
        final int h = tabPosition.getHeight();
        GuiUtil.texturedQuad(graphics, Textures.GUI.UpgradeTab, x, y, w, h);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
        final int originalWidth = imageWidth;
        try {
            imageWidth += tabPosition.getWidth();
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            imageWidth = originalWidth;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
        final int originalWidth = imageWidth;
        try {
            imageWidth += tabPosition.getWidth();
            return super.mouseReleased(mouseX, mouseY, button);
        } finally {
            imageWidth = originalWidth;
        }
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY);

        graphics.drawString(font,
                Localization.Switch.TransferRate(),
                14, 20, 0x404040, false);
        graphics.drawString(font,
                Localization.Switch.PacketsPerCycle(),
                14, 39, 0x404040, false);
        graphics.drawString(font,
                Localization.Switch.QueueSize(),
                14, 58, 0x404040, false);

        graphics.drawString(font,
                format.format(20f / inventoryContainer.relayDelay()),
                108, 20, 0x404040, false);
        graphics.drawString(font,
                inventoryContainer.packetsPerCycleAvg() + " / " + inventoryContainer.relayAmount(),
                108, 39, thresholdBasedColor(inventoryContainer.packetsPerCycleAvg(), (int) Math.ceil(inventoryContainer.relayAmount() / 2f), inventoryContainer.relayAmount()), false);
        graphics.drawString(font,
                inventoryContainer.queueSize() + " / " + inventoryContainer.maxQueueSize(),
                108, 58, thresholdBasedColor(inventoryContainer.queueSize(), inventoryContainer.maxQueueSize() / 2, inventoryContainer.maxQueueSize()), false);
    }

    private static int thresholdBasedColor(int value, int yellow, int red) {
        if (value < yellow) return 0x009900;
        else if (value < red) return 0x999900;
        else return 0x990000;
    }
}
