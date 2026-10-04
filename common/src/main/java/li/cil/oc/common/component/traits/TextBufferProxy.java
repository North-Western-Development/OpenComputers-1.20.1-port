package li.cil.oc.common.component.traits;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.util.ExtendedUnicodeHelper;
import li.cil.oc.util.PackedColor;

/**
 * Implements most of {@link TextBuffer} on top of a {@link li.cil.oc.util.TextBuffer}.
 * Implementers provide {@link #data()} and may override the {@code onBufferXxx} hooks.
 */
public interface TextBufferProxy extends TextBuffer {
    li.cil.oc.util.TextBuffer data();

    @Override
    default int getWidth() {
        return data().width;
    }

    @Override
    default int getHeight() {
        return data().height;
    }

    @Override
    default boolean setColorDepth(TextBuffer.ColorDepth depth) {
        if (depth.ordinal() > getMaximumColorDepth().ordinal())
            throw new IllegalArgumentException("unsupported depth");
        return data().setFormat(PackedColor.Depth.format(depth));
    }

    @Override
    default TextBuffer.ColorDepth getColorDepth() {
        return data().format().depth();
    }

    default void onBufferPaletteChange(int index) {
    }

    @Override
    default void setPaletteColor(int index, int color) {
        if (data().format() instanceof PackedColor.MutablePaletteFormat palette) {
            palette.update(index, color);
            onBufferPaletteChange(index);
        } else throw new RuntimeException("palette not available");
    }

    @Override
    default int getPaletteColor(int index) {
        if (data().format() instanceof PackedColor.MutablePaletteFormat palette) {
            return palette.apply(index);
        } else throw new RuntimeException("palette not available");
    }

    default void onBufferColorChange() {
    }

    @Override
    default void setForegroundColor(int color) {
        setForegroundColor(color, false);
    }

    @Override
    default void setForegroundColor(int color, boolean isFromPalette) {
        final PackedColor.Color value = new PackedColor.Color(color, isFromPalette);
        if (!data().foreground().equals(value)) {
            data().setForeground(value);
            onBufferColorChange();
        }
    }

    @Override
    default int getForegroundColor() {
        return data().foreground().value;
    }

    @Override
    default boolean isForegroundFromPalette() {
        return data().foreground().isPalette;
    }

    @Override
    default void setBackgroundColor(int color) {
        setBackgroundColor(color, false);
    }

    @Override
    default void setBackgroundColor(int color, boolean isFromPalette) {
        final PackedColor.Color value = new PackedColor.Color(color, isFromPalette);
        if (!data().background().equals(value)) {
            data().setBackground(value);
            onBufferColorChange();
        }
    }

    @Override
    default int getBackgroundColor() {
        return data().background().value;
    }

    @Override
    default boolean isBackgroundFromPalette() {
        return data().background().isPalette;
    }

    default void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
    }

    @Override
    default void copy(int col, int row, int w, int h, int tx, int ty) {
        if (data().copy(col, row, w, h, tx, ty))
            onBufferCopy(col, row, w, h, tx, ty);
    }

    default void onBufferFill(int col, int row, int w, int h, int c) {
    }

    @Override
    @SuppressWarnings("deprecation")
    default void fill(int col, int row, int w, int h, char c) {
        fill(col, row, w, h, (int) c);
    }

    @Override
    default void fill(int col, int row, int w, int h, int c) {
        if (data().fill(col, row, w, h, c))
            onBufferFill(col, row, w, h, c);
    }

    default void onBufferSet(int col, int row, String s, boolean vertical) {
    }

    /** The code points [leftOffset, leftOffset + min(sLength - leftOffset, maxWidth)) of s. */
    private static String truncate(String s, int sLength, int leftOffset, int maxWidth) {
        final int width = Math.min(sLength - leftOffset, maxWidth);
        if (width <= 0) return "";
        if (leftOffset == 0 && sLength <= width) return s;
        final int subFrom = s.offsetByCodePoints(0, leftOffset);
        return s.substring(subFrom, s.offsetByCodePoints(subFrom, width));
    }

    @Override
    default void set(int col, int row, String s, boolean vertical) {
        final li.cil.oc.util.TextBuffer data = data();
        // Lengths and offsets are in code points (characters outside the BMP are
        // two chars in Java strings).
        final int sLength = ExtendedUnicodeHelper.length(s);
        if (col < data.width && (col >= 0 || -col < sLength)) {
            // Make sure the string isn't longer than it needs to be, in particular to
            // avoid sending too much data to our clients.
            final int x;
            final int y;
            final String truncated;
            if (vertical) {
                if (row < 0) {
                    x = col;
                    y = 0;
                    truncated = truncate(s, sLength, Math.min(-row, sLength), data.height);
                } else {
                    x = col;
                    y = row;
                    truncated = truncate(s, sLength, 0, data.height - row);
                }
            } else {
                if (col < 0) {
                    x = 0;
                    y = row;
                    truncated = truncate(s, sLength, -col, data.width);
                } else {
                    x = col;
                    y = row;
                    truncated = truncate(s, sLength, 0, data.width - col);
                }
            }
            if (data.set(x, y, truncated, vertical))
                onBufferSet(x, row, truncated, vertical);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    default char get(int col, int row) {
        return (char) data().get(col, row);
    }

    @Override
    default int getCodePoint(int col, int row) {
        return data().get(col, row);
    }

    @Override
    default int getForegroundColor(int column, int row) {
        if (isForegroundFromPalette(column, row)) {
            return PackedColor.extractForeground(packedColorAt(column, row));
        } else {
            return PackedColor.unpackForeground(packedColorAt(column, row), data().format());
        }
    }

    @Override
    default boolean isForegroundFromPalette(int column, int row) {
        return data().format().isFromPalette(PackedColor.extractForeground(packedColorAt(column, row)));
    }

    @Override
    default int getBackgroundColor(int column, int row) {
        if (isBackgroundFromPalette(column, row)) {
            return PackedColor.extractBackground(packedColorAt(column, row));
        } else {
            return PackedColor.unpackBackground(packedColorAt(column, row), data().format());
        }
    }

    @Override
    default boolean isBackgroundFromPalette(int column, int row) {
        return data().format().isFromPalette(PackedColor.extractBackground(packedColorAt(column, row)));
    }

    @Override
    @SuppressWarnings("deprecation")
    default void rawSetText(int col, int row, char[][] text) {
        final int[][] codePoints = new int[text.length][];
        for (int y = 0; y < text.length; y++) {
            codePoints[y] = new int[text[y].length];
            for (int x = 0; x < text[y].length; x++) codePoints[y][x] = text[y][x];
        }
        rawSetText(col, row, codePoints);
    }

    @Override
    default void rawSetText(int col, int row, int[][] text) {
        final li.cil.oc.util.TextBuffer data = data();
        for (int y = row; y < Math.min(row + text.length, data.height); y++) {
            final int[] line = text[y - row];
            System.arraycopy(line, 0, data.buffer[y], col, Math.max(0, Math.min(line.length, data.width - col)));
        }
    }

    @Override
    default void rawSetForeground(int col, int row, int[][] color) {
        final li.cil.oc.util.TextBuffer data = data();
        for (int y = row; y < Math.min(row + color.length, data.height); y++) {
            final int[] line = color[y - row];
            for (int x = col; x < Math.min(col + line.length, data.width); x++) {
                final int packedBackground = data.color[y][x] & 0x00FF;
                final int packedForeground = (data.format().deflate(new PackedColor.Color(line[x - col])) << PackedColor.ForegroundShift) & 0xFF00;
                data.color[y][x] = (short) (packedForeground | packedBackground);
            }
        }
    }

    @Override
    default void rawSetBackground(int col, int row, int[][] color) {
        final li.cil.oc.util.TextBuffer data = data();
        for (int y = row; y < Math.min(row + color.length, data.height); y++) {
            final int[] line = color[y - row];
            for (int x = col; x < Math.min(col + line.length, data.width); x++) {
                final int packedBackground = data.format().deflate(new PackedColor.Color(line[x - col])) & 0x00FF;
                final int packedForeground = data.color[y][x] & 0xFF00;
                data.color[y][x] = (short) (packedForeground | packedBackground);
            }
        }
    }

    // Scala: private def color(column, row).
    private short packedColorAt(int column, int row) {
        if (column < 0 || column >= getWidth() || row < 0 || row >= getHeight())
            throw new IndexOutOfBoundsException();
        else return data().color[row][column];
    }
}
