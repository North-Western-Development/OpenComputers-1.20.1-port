package li.cil.oc.client.renderer.markdown.segment;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Optional;

public class RenderSegment extends Segment implements InteractiveSegment {
    private final Segment parent;
    public final String title;
    public final ImageRenderer imageRenderer;

    public int lastX = 0;
    public int lastY = 0;

    public RenderSegment(Segment parent, String title, ImageRenderer imageRenderer) {
        this.parent = parent;
        this.title = title;
        this.imageRenderer = imageRenderer;
    }

    @Override
    public Segment parent() {
        return parent;
    }

    @Override
    public Optional<String> tooltip() {
        if (imageRenderer instanceof InteractiveImageRenderer interactive) return Optional.ofNullable(interactive.getTooltip(title));
        else return Optional.ofNullable(title);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY) {
        if (imageRenderer instanceof InteractiveImageRenderer interactive) return interactive.onMouseClick(mouseX - lastX, mouseY - lastY);
        else return false;
    }

    private float scale(int maxWidth) {
        return Math.min(1f, maxWidth / (float) imageRenderer.getWidth());
    }

    public int imageWidth(int maxWidth) {
        return Math.min(maxWidth, imageRenderer.getWidth());
    }

    public int imageHeight(int maxWidth) {
        return (int) Math.ceil(imageRenderer.getHeight() * scale(maxWidth)) + 4;
    }

    @Override
    public int nextY(int indent, int maxWidth, Font renderer) {
        return imageHeight(maxWidth) + (indent > 0 ? Document.lineHeight(renderer) : 0);
    }

    @Override
    public int nextX(int indent, int maxWidth, Font renderer) {
        return 0;
    }

    @Override
    public Optional<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth, Font renderer, int mouseX, int mouseY) {
        final int width = imageWidth(maxWidth);
        final int height = imageHeight(maxWidth);
        final int xOffset = (maxWidth - width) / 2;
        final int yOffset = 2 + (indent > 0 ? Document.lineHeight(renderer) : 0);
        final float s = scale(maxWidth);

        lastX = x + xOffset;
        lastY = y + yOffset;

        final Optional<InteractiveSegment> hovered = checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, width, height);

        final PoseStack stack = graphics.pose();
        stack.pushPose();
        stack.translate(x + xOffset, y + yOffset, 0);
        stack.scale(s, s, s);

        RenderSystem.enableBlend();
        // Disabled by text rendering above it (default state is disabled).
        RenderSystem.enableDepthTest();

        if (hovered.isPresent()) {
            // Formerly a quad drawn with color (1, 1, 1, 0.15).
            graphics.fill(0, 0, imageRenderer.getWidth(), imageRenderer.getHeight(), 0x26FFFFFF);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);

        imageRenderer.render(graphics, mouseX - x, mouseY - y);
        graphics.flush();

        RenderSystem.disableBlend();

        stack.popPose();

        return hovered;
    }

    @Override
    public String toString(MarkupFormat format) {
        switch (format) {
            case IGWMod:
                return "(Sorry, images only work in the OpenComputers manual for now.)"; // TODO
            default:
                return "![" + title + "](" + imageRenderer + ")";
        }
    }
}
