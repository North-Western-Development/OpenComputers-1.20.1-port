package li.cil.oc.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ImageButton extends Button {
    public final ResourceLocation image;
    public final boolean canToggle;
    public final int textColor;
    public final int textDisabledColor;
    public final int textHoverColor;
    public final int textIndent;

    public boolean toggled = false;

    public boolean hoverOverride = false;

    public ImageButton(int xPos, int yPos, int w, int h, OnPress handler) {
        this(xPos, yPos, w, h, handler, null);
    }

    public ImageButton(int xPos, int yPos, int w, int h, OnPress handler, ResourceLocation image) {
        this(xPos, yPos, w, h, handler, image, false);
    }

    public ImageButton(int xPos, int yPos, int w, int h, OnPress handler, ResourceLocation image, boolean canToggle) {
        this(xPos, yPos, w, h, handler, image, Component.empty(), canToggle, 0xE0E0E0, 0xA0A0A0, 0xFFFFA0, -1);
    }

    public ImageButton(int xPos, int yPos, int w, int h,
                       OnPress handler,
                       ResourceLocation image,
                       Component text,
                       boolean canToggle,
                       int textColor,
                       int textDisabledColor,
                       int textHoverColor,
                       int textIndent) {
        super(xPos, yPos, w, h, text, handler, DEFAULT_NARRATION);
        this.image = image;
        this.canToggle = canToggle;
        this.textColor = textColor;
        this.textDisabledColor = textDisabledColor;
        this.textHoverColor = textHoverColor;
        this.textIndent = textIndent;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Only called while visible; isHovered is updated by AbstractWidget.render.
        final int x0 = getX();
        final int x1 = getX() + width;
        final int y0 = getY();
        final int y1 = getY() + height;

        final boolean drawHover = hoverOverride || (active && isHovered);

        if (image != null) {
            final float u0 = toggled ? 0.5f : 0;
            final float u1 = u0 + (canToggle ? 0.5f : 1);
            final float v0 = drawHover ? 0.5f : 0;
            final float v1 = v0 + 0.5f;

            graphics.setColor(1, 1, 1, 1);
            GuiUtil.texturedQuad(graphics, image, x0, y0, x1, y1, 0, u0, u1, v0, v1);
        } else {
            final int alpha = (int) ((drawHover ? 0.8f : 0.4f) * 255);
            graphics.fill(x0, y0, x1, y1, (alpha << 24) | 0xFFFFFF);
        }

        if (!getMessage().getString().isEmpty()) {
            final int color;
            if (!active) color = textDisabledColor;
            else if (hoverOverride || isHovered) color = textHoverColor;
            else color = textColor;
            final Minecraft mc = Minecraft.getInstance();
            if (textIndent >= 0) graphics.drawString(mc.font, getMessage(), textIndent + getX(), getY() + (height - 8) / 2, color);
            else graphics.drawCenteredString(mc.font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
        }
    }
}
