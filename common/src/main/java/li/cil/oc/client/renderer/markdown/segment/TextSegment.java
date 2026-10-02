package li.cil.oc.client.renderer.markdown.segment;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.client.renderer.markdown.Document;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSegment extends BasicTextSegment {
    private final Segment parent;
    public final String text;

    public TextSegment(Segment parent, String text) {
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
        int currentX = x + indent;
        int currentY = y;
        String chars = text;
        if (indent == 0) chars = dropLeadingWhitespace(chars);
        final int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        Optional<InteractiveSegment> hovered = Optional.empty();
        final float scale = resolvedScale();
        final PoseStack stack = graphics.pose();
        while (chars.length() > 0) {
            final String part = take(chars, numChars);
            if (hovered.isEmpty()) {
                final Optional<InteractiveSegment> interactive = resolvedInteractive();
                if (interactive.isPresent()) {
                    hovered = interactive.get().checkHovered(mouseX, mouseY, currentX, currentY, stringWidth(part, renderer), (int) (Document.lineHeight(renderer) * scale));
                }
            }
            stack.pushPose();
            stack.translate(currentX, currentY, 0);
            stack.scale(scale, scale, scale);
            stack.translate(-currentX, -currentY, 0);
            graphics.drawString(renderer, resolvedFormat() + part, currentX, currentY, resolvedColor(), false);
            stack.popPose();
            currentX = x + wrapIndent;
            currentY += lineHeight(renderer);
            chars = dropLeadingWhitespace(drop(chars, numChars));
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }

        return hovered;
    }

    @Override
    public List<Segment> refine(Pattern pattern, BiFunction<Segment, MatchResult, Segment> factory) {
        final List<Segment> result = new ArrayList<>();

        // Keep track of last matches end, to generate plain text segments.
        int textStart = 0;
        final Matcher m = pattern.matcher(text);
        while (m.find()) {
            // Create segment for leading plain text.
            if (m.start() > textStart) {
                result.add(new TextSegment(this, text.substring(textStart, m.start())));
            }
            textStart = m.end();

            // Create segment for formatted text.
            result.add(factory.apply(this, m.toMatchResult()));
        }

        // Create segment for remaining plain text.
        if (textStart == 0) {
            result.add(this);
        } else if (textStart < text.length()) {
            result.add(new TextSegment(this, text.substring(textStart)));
        }
        return result;
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected int lineHeight(Font renderer) {
        return (int) (super.lineHeight(renderer) * resolvedScale());
    }

    @Override
    protected int stringWidth(String s, Font renderer) {
        return (int) (renderer.width(resolvedFormat() + s) * resolvedScale());
    }

    // ----------------------------------------------------------------------- //

    protected Optional<Integer> color() {
        return Optional.empty();
    }

    protected Optional<Float> scale() {
        return Optional.empty();
    }

    protected String format() {
        return "";
    }

    private int resolvedColor() {
        return color().orElseGet(() -> parent instanceof TextSegment segment ? segment.resolvedColor() : 0xDDDDDD);
    }

    private float resolvedScale() {
        if (parent instanceof TextSegment segment) return scale().orElse(1f) * segment.resolvedScale();
        else return 1f;
    }

    private String resolvedFormat() {
        if (parent instanceof TextSegment segment) return segment.resolvedFormat() + format();
        else return format();
    }

    private Optional<InteractiveSegment> resolvedInteractive = null;

    private Optional<InteractiveSegment> resolvedInteractive() {
        if (resolvedInteractive == null) {
            if (this instanceof InteractiveSegment segment) resolvedInteractive = Optional.of(segment);
            else if (parent instanceof TextSegment segment) resolvedInteractive = segment.resolvedInteractive();
            else resolvedInteractive = Optional.empty();
        }
        return resolvedInteractive;
    }
}
