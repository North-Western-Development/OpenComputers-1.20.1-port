package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public abstract class Segment {
    /**
     * Parent segment, i.e. the segment this segment was refined from.
     * Each line starts as a TextSegment that is refined based into segments
     * based on the handled formatting rules / patterns.
     */
    public abstract Segment parent();

    /**
     * The root segment, i.e. the original parent of this segment.
     */
    public final Segment root() {
        Segment segment = this;
        while (segment.parent() != null) segment = segment.parent();
        return segment;
    }

    /**
     * Get the X coordinate at which to render the next segment.
     * <p>
     * For flowing/inline segments this will be to the right of the last line
     * this segment renders, for block segments it will be at the start of
     * the next line below this segment.
     * <p>
     * The coordinates in this context are relative to (0,0).
     */
    public abstract int nextX(int indent, int maxWidth, Font renderer);

    /**
     * Get the Y coordinate at which to render the next segment.
     * <p>
     * For flowing/inline segments this will be the same level as the last line
     * this segment renders, unless it's the last segment on its line. For block
     * segments and last-on-line segments this will be the next line after.
     * <p>
     * The coordinates in this context are relative to (0,0).
     */
    public abstract int nextY(int indent, int maxWidth, Font renderer);

    /**
     * Render the segment at the specified coordinates with the specified
     * properties.
     */
    public Optional<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth, Font renderer, int mouseX, int mouseY) {
        return Optional.empty();
    }

    public List<String> renderAsText(MarkupFormat format) {
        Segment segment = this;
        final List<String> result = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        while (segment != null) {
            builder.append(segment.toString(format));
            if (segment.isLast()) {
                result.add(builder.toString());
                builder.setLength(0);
            }
            segment = segment.next;
        }
        return result;
    }

    public abstract String toString(MarkupFormat format);

    @Override
    public String toString() {
        return toString(MarkupFormat.Markdown);
    }

    // ----------------------------------------------------------------------- //

    // Used during construction, checks a segment for inner segments.
    public List<Segment> refine(Pattern pattern, BiFunction<Segment, MatchResult, Segment> factory) {
        return Collections.singletonList(this);
    }

    // Set after construction of document, used for formatting, specifically
    // to compute the height for last segment on a line (to force a new line).
    public Segment next = null;

    // Utility method to check if the segment is the last on a line.
    public boolean isLast() {
        return next == null || root() != next.root();
    }
}
