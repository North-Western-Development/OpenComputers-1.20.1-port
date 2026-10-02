package li.cil.oc.client.renderer.markdown;

import li.cil.oc.api.Manual;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.client.renderer.markdown.segment.BoldSegment;
import li.cil.oc.client.renderer.markdown.segment.CodeSegment;
import li.cil.oc.client.renderer.markdown.segment.HeaderSegment;
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment;
import li.cil.oc.client.renderer.markdown.segment.ItalicSegment;
import li.cil.oc.client.renderer.markdown.segment.LinkSegment;
import li.cil.oc.client.renderer.markdown.segment.RenderSegment;
import li.cil.oc.client.renderer.markdown.segment.Segment;
import li.cil.oc.client.renderer.markdown.segment.StrikethroughSegment;
import li.cil.oc.client.renderer.markdown.segment.TextSegment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Primitive Markdown parser, only supports a very small subset. Used for
 * parsing documentation into segments, to be displayed in a GUI somewhere.
 * <p>
 * General usage is: parse a string using parse(), render it using render().
 * <p>
 * The parser generates a list of segments, each segment representing a part
 * of the document, with a specific formatting / render type. For example,
 * links are their own segments, a bold section in a link would be its own
 * section and so on.
 * The data structure is essentially a very flat multi-tree, where the segments
 * returned are the leaves, and the roots are the individual lines, represented
 * as text segments.
 * Formatting is done by accumulating formatting information over the parent
 * nodes, up to the root.
 */
public final class Document {
    private Document() {
    }

    /**
     * Parses a plain text document into a list of segments.
     */
    public static Segment parse(Iterable<String> document) {
        List<Segment> segments = new ArrayList<>();
        for (String line : document) {
            segments.add(new TextSegment(null, line == null ? "" : stripTrailing(line)));
        }
        for (Pair<Pattern, BiFunction<Segment, MatchResult, Segment>> entry : segmentTypes) {
            final List<Segment> refined = new ArrayList<>();
            for (Segment segment : segments) {
                refined.addAll(segment.refine(entry.getLeft(), entry.getRight()));
            }
            segments = refined;
        }
        for (int i = 0; i + 1 < segments.size(); i++) {
            segments.get(i).next = segments.get(i + 1);
        }
        return segments.isEmpty() ? new TextSegment(null, "") : segments.get(0);
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }

    /**
     * Compute the overall height of a document, e.g. for computation of scroll offsets.
     */
    public static int height(Segment document, int maxWidth, Font renderer) {
        int currentX = 0;
        int currentY = 0;
        Segment segment = document;
        while (segment != null) {
            currentY += segment.nextY(currentX, maxWidth, renderer);
            currentX = segment.nextX(currentX, maxWidth, renderer);
            segment = segment.next;
        }
        return currentY;
    }

    /**
     * Line height for a normal line of text.
     */
    public static int lineHeight(Font renderer) {
        return renderer.lineHeight + 1;
    }

    /**
     * Renders a list of segments and tooltips if a segment with a tooltip is hovered.
     * Returns the hovered interactive segment, if any.
     */
    public static Optional<InteractiveSegment> render(GuiGraphics graphics, Segment document, int x, int y, int maxWidth, int maxHeight, int yOffset, Font renderer, int mouseX, int mouseY) {
        // Clip using the scissor test to not interfere with depth testing.
        final Matrix4f pose = graphics.pose().last().pose();
        final Vector4f bottomLeft = pose.transform(new Vector4f(x, y + maxHeight, 0, 1));
        final Vector4f topRight = pose.transform(new Vector4f(x + maxWidth, y, 0, 1));
        graphics.enableScissor(
            (int) Math.floor(Math.min(bottomLeft.x(), topRight.x())),
            (int) Math.floor(Math.min(bottomLeft.y(), topRight.y())),
            (int) Math.ceil(Math.max(bottomLeft.x(), topRight.x())),
            (int) Math.ceil(Math.max(bottomLeft.y(), topRight.y())));

        // Actual rendering.
        Optional<InteractiveSegment> hovered = Optional.empty();
        int indent = 0;
        int currentY = y - yOffset;
        final int minY = y - lineHeight(renderer);
        final int maxY = y + maxHeight + lineHeight(renderer);
        Segment segment = document;
        while (segment != null) {
            final int segmentHeight = segment.nextY(indent, maxWidth, renderer);
            if (currentY + segmentHeight >= minY && currentY <= maxY) {
                final Optional<InteractiveSegment> result = segment.render(graphics, x, currentY, indent, maxWidth, renderer, mouseX, mouseY);
                if (hovered.isEmpty()) hovered = result;
            }
            currentY += segmentHeight;
            indent = segment.nextX(indent, maxWidth, renderer);
            segment = segment.next;
        }
        if (mouseX < x || mouseX > x + maxWidth || mouseY < y || mouseY > y + maxHeight) hovered = Optional.empty();
        hovered.ifPresent(InteractiveSegment::notifyHover);

        graphics.disableScissor();

        return hovered;
    }

    // ----------------------------------------------------------------------- //

    private static Segment makeHeaderSegment(Segment s, MatchResult m) {
        return new HeaderSegment(s, m.group(2), m.group(1).length());
    }

    private static Segment makeCodeSegment(Segment s, MatchResult m) {
        return new CodeSegment(s, m.group(2));
    }

    private static Segment makeLinkSegment(Segment s, MatchResult m) {
        return new LinkSegment(s, m.group(1), m.group(2));
    }

    private static Segment makeBoldSegment(Segment s, MatchResult m) {
        return new BoldSegment(s, m.group(2));
    }

    private static Segment makeItalicSegment(Segment s, MatchResult m) {
        return new ItalicSegment(s, m.group(2));
    }

    private static Segment makeStrikethroughSegment(Segment s, MatchResult m) {
        return new StrikethroughSegment(s, m.group(1));
    }

    private static Segment makeImageSegment(Segment s, MatchResult m) {
        try {
            final ImageRenderer renderer = Manual.imageFor(m.group(2));
            if (renderer != null) return new RenderSegment(s, m.group(1), renderer);
            else return new TextSegment(s, "No renderer found for: " + m.group(2));
        } catch (Throwable t) {
            return new TextSegment(s, t.toString());
        }
    }

    // ----------------------------------------------------------------------- //

    private static Pair<Pattern, BiFunction<Segment, MatchResult, Segment>> entry(String regex, BiFunction<Segment, MatchResult, Segment> factory) {
        return Pair.of(Pattern.compile(regex), factory);
    }

    private static final List<Pair<Pattern, BiFunction<Segment, MatchResult, Segment>>> segmentTypes = List.of(
        entry("^(#+)\\s(.*)", Document::makeHeaderSegment), // headers: # ...
        entry("(`)(.*?)\\1", Document::makeCodeSegment), // code: `...`
        entry("!\\[([^\\[]*)\\]\\(([^\\)]+)\\)", Document::makeImageSegment), // images: ![...](...)
        entry("\\[([^\\[]+)\\]\\(([^\\)]+)\\)", Document::makeLinkSegment), // links: [...](...)
        entry("(\\*\\*|__)(\\S.*?\\S|$)\\1", Document::makeBoldSegment), // bold: **...** | __...__
        entry("(\\*|_)(\\S.*?\\S|$)\\1", Document::makeItalicSegment), // italic: *...* | _..._
        entry("~~(\\S.*?\\S|$)~~", Document::makeStrikethroughSegment) // strikethrough: ~~...~~
    );
}
