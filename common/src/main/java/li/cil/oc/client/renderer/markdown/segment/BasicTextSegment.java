package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;

import java.util.Set;

public abstract class BasicTextSegment extends Segment {
    protected static final Set<Character> breaks = Set.of(' ', '.', ',', ':', ';', '!', '?', '_', '=', '-', '+', '*', '/', '\\');
    protected static final Set<String> lists = Set.of("- ", "* ");
    private String rootPrefix = null;

    protected String rootPrefix() {
        if (rootPrefix == null) {
            final Segment root = root();
            final String rootText = root instanceof TextSegment text ? text.text : "";
            rootPrefix = take(rootText, 2);
        }
        return rootPrefix;
    }

    @Override
    public int nextX(int indent, int maxWidth, Font renderer) {
        if (isLast()) return 0;
        int currentX = indent;
        String chars = text();
        if (ignoreLeadingWhitespace() && indent == 0) chars = dropLeadingWhitespace(chars);
        final int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (chars.length() > numChars) {
            chars = dropLeadingWhitespace(drop(chars, numChars));
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
            currentX = wrapIndent;
        }
        return currentX + stringWidth(chars, renderer);
    }

    @Override
    public int nextY(int indent, int maxWidth, Font renderer) {
        int lines = 0;
        String chars = text();
        if (ignoreLeadingWhitespace() && indent == 0) chars = dropLeadingWhitespace(chars);
        final int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (chars.length() > numChars) {
            lines += 1;
            chars = dropLeadingWhitespace(drop(chars, numChars));
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }
        if (isLast()) lines += 1;
        return lines * lineHeight(renderer);
    }

    @Override
    public String toString(MarkupFormat format) {
        return text();
    }

    // ----------------------------------------------------------------------- //

    protected abstract String text();

    protected boolean ignoreLeadingWhitespace() {
        return true;
    }

    protected int lineHeight(Font renderer) {
        return Document.lineHeight(renderer);
    }

    protected abstract int stringWidth(String s, Font renderer);

    protected int maxChars(String s, int maxWidth, int maxLineWidth, Font renderer) {
        int pos = -1;
        int lastBreak = -1;
        final int fullWidth = stringWidth(s, renderer);
        while (pos < s.length()) {
            pos += 1;
            final int width = stringWidth(take(s, pos), renderer);
            final boolean exceedsLineLength = width >= maxWidth;
            if (exceedsLineLength) {
                final boolean mayUseFullLine = maxWidth == maxLineWidth;
                final boolean canFitInLine = fullWidth <= maxLineWidth;
                final boolean matchesFullLine = fullWidth == maxLineWidth;
                if (lastBreak >= 0) {
                    return lastBreak + 1; // Can do a soft split.
                }
                if (mayUseFullLine && matchesFullLine) {
                    return s.length(); // Special case for exact match.
                }
                if (canFitInLine && !mayUseFullLine) {
                    return 0; // Wrap line, use next line.
                }
                return pos - 1; // Gotta split hard.
            }
            if (pos < s.length() && breaks.contains(s.charAt(pos))) lastBreak = pos;
        }
        return pos;
    }

    protected int computeWrapIndent(Font renderer) {
        final String prefix = rootPrefix();
        return lists.contains(prefix) ? renderer.width(prefix) : 0;
    }

    // ----------------------------------------------------------------------- //
    // Scala string helpers.

    protected static String take(String s, int n) {
        return s.substring(0, Math.max(0, Math.min(n, s.length())));
    }

    protected static String drop(String s, int n) {
        return s.substring(Math.max(0, Math.min(n, s.length())));
    }

    protected static String dropLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }
}
