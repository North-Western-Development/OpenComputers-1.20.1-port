package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Optional;

public class CodeSegment extends BasicTextSegment {
    private final Segment parent;
    public final String text;

    public CodeSegment(Segment parent, String text) {
        this.parent = parent;
        this.text = text;
    }

    @Override
    public Segment parent() {
        return parent;
    }

    @Override
    protected String text() {
        return text;
    }

    @Override
    public Optional<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth, Font renderer, int mouseX, int mouseY) {
        TextBufferRenderCache.renderer.generateChars(text.codePoints().toArray());

        int currentX = x + indent;
        int currentY = y;
        String chars = text;
        final int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (chars.length() > 0) {
            final String part = take(chars, numChars);
            // Formerly RenderSystem.color4f(0.75f, 0.8f, 1, 1).
            TextBufferRenderCache.renderer.drawString(graphics.pose(), graphics.bufferSource(), part, currentX, currentY, 0xBFCCFF);
            currentX = x + wrapIndent;
            currentY += lineHeight(renderer);
            chars = dropLeadingWhitespace(drop(chars, numChars));
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }
        graphics.flush();

        return Optional.empty();
    }

    @Override
    protected boolean ignoreLeadingWhitespace() {
        return false;
    }

    @Override
    protected int stringWidth(String s, Font renderer) {
        return s.length() * TextBufferRenderCache.renderer.charRenderWidth();
    }

    @Override
    public String toString(MarkupFormat format) {
        switch (format) {
            case IGWMod:
                return "[prefix{1}]" + text + " [prefix{}]";
            default:
                return "`" + text + "`";
        }
    }
}
