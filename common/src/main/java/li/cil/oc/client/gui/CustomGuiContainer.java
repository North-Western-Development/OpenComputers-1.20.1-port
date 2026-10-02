package li.cil.oc.client.gui;

import li.cil.oc.client.gui.widget.Widget;
import li.cil.oc.client.gui.widget.WidgetContainer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;

// Workaround because certain other mods *cough*TMI*cough* do base class
// transformations that break things! Such fun. Many annoyed. And yes, this
// is a common issue, have a look at EnderIO and Enchanting Plus. They have
// to work around this, too.
public abstract class CustomGuiContainer<C extends AbstractContainerMenu> extends AbstractContainerScreen<C> implements WidgetContainer {
    public final C inventoryContainer;

    private final List<Widget> widgets = new ArrayList<>();

    protected CustomGuiContainer(C inventoryContainer, Inventory inv, Component title) {
        super(inventoryContainer, inv, title);
        this.inventoryContainer = inventoryContainer;
    }

    @Override
    public List<Widget> widgets() {
        return widgets;
    }

    @Override
    public int windowX() {
        return leftPos;
    }

    @Override
    public int windowY() {
        return topPos;
    }

    @Override
    public float windowZ() {
        // Blit offsets no longer exist; depth is part of the pose.
        return 0f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return pointX >= rectX - 1 && pointX < rectX + rectWidth + 1 && pointY >= rectY - 1 && pointY < rectY + rectHeight + 1;
    }

    protected void copiedDrawHoveringText(GuiGraphics graphics, List<String> lines, int x, int y, Font font) {
        final List<Component> text = new ArrayList<>();
        for (String line : lines) {
            text.add(Component.literal(line));
        }
        copiedDrawHoveringText0(graphics, text, x, y, font);
    }

    // Pretty much copy-pasta from the base-class tooltip rendering, but using the current pose
    // (callers draw in the label layer, which is translated to the window's origin).
    protected void copiedDrawHoveringText0(GuiGraphics graphics, List<? extends FormattedText> text, int x, int y, Font font) {
        if (!text.isEmpty()) {
            int textWidth = 0;
            for (FormattedText line : text) {
                textWidth = Math.max(textWidth, font.width(line));
            }

            int posX = x + 12;
            int posY = y - 12;
            int textHeight = 8;
            if (text.size() > 1) {
                textHeight += 2 + (text.size() - 1) * 10;
            }
            if (posX + textWidth > width) {
                posX -= 28 + textWidth;
            }
            if (posY + textHeight + 6 > height) {
                posY = height - textHeight - 6;
            }

            graphics.pose().pushPose();
            TooltipRenderUtil.renderTooltipBackground(graphics, posX, posY, textWidth, textHeight, 300);

            graphics.pose().translate(0, 0, 400);
            for (int index = 0; index < text.size(); index++) {
                graphics.drawString(font, Language.getInstance().getVisualOrder(text.get(index)), posX, posY, -1, true);
                if (index == 0) {
                    posY += 2;
                }
                posY += 10;
            }
            graphics.pose().popPose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
