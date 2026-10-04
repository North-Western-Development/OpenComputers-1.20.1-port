package li.cil.oc.util;

import li.cil.oc.Settings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;

/**
 * This stores code points (Unicode, including characters outside the BMP) in a
 * 2D-Array and provides some manipulation functions.
 *
 * The main purpose of this is to allow moving most implementation detail to
 * the Lua side while keeping bandwidth costs low and still allowing for
 * relatively fast updates, given a smart algorithm (using copy()/fill()
 * instead of set()ing everything).
 */
public class TextBuffer {
    public int width;
    public int height;

    private PackedColor.ColorFormat _format;

    private PackedColor.Color _foreground = new PackedColor.Color(0xFFFFFF);

    private PackedColor.Color _background = new PackedColor.Color(0x000000);

    private short packed;

    public short[][] color;

    public int[][] buffer;

    public TextBuffer(int width, int height, PackedColor.ColorFormat initialFormat) {
        this.width = width;
        this.height = height;
        this._format = initialFormat;
        this.packed = PackedColor.pack(_foreground, _background, _format);
        this.color = new short[height][width];
        for (short[] row : color) Arrays.fill(row, packed);
        this.buffer = new int[height][width];
        for (int[] row : buffer) Arrays.fill(row, 0x20);
    }

    public TextBuffer(Pair<Integer, Integer> size, PackedColor.ColorFormat format) {
        this(size.getLeft(), size.getRight(), format);
    }

    public PackedColor.Color foreground() {
        return _foreground;
    }

    public TextBuffer setForeground(PackedColor.Color value) {
        format().validate(value);
        _foreground = value;
        packed = PackedColor.pack(_foreground, _background, _format);
        return this;
    }

    public PackedColor.Color background() {
        return _background;
    }

    public TextBuffer setBackground(PackedColor.Color value) {
        format().validate(value);
        _background = value;
        packed = PackedColor.pack(_foreground, _background, _format);
        return this;
    }

    public PackedColor.ColorFormat format() {
        return _format;
    }

    public boolean setFormat(PackedColor.ColorFormat value) {
        if (format().depth() != value.depth()) {
            for (int row = 0; row < height; row++) {
                final short[] rowColor = color[row];
                for (int col = 0; col < width; col++) {
                    final short packed = rowColor[col];
                    final PackedColor.Color fg = new PackedColor.Color(PackedColor.unpackForeground(packed, _format));
                    final PackedColor.Color bg = new PackedColor.Color(PackedColor.unpackBackground(packed, _format));
                    rowColor[col] = PackedColor.pack(fg, bg, value);
                }
            }
            _format = value;
            packed = PackedColor.pack(_foreground, _background, _format);
            return true;
        } else return false;
    }

    /** The current buffer size in columns by rows. */
    public Pair<Integer, Integer> size() {
        return Pair.of(width, height);
    }

    /**
     * Set the new buffer size, returns true if the size changed.
     *
     * This will perform a proper resize as required, keeping as much of the
     * buffer valid as possible if the size decreases, i.e. only data outside the
     * new buffer size will be truncated, all data still inside will be copied.
     */
    public boolean setSize(int iw, int ih) {
        final int w = Math.max(iw, 1);
        final int h = Math.max(ih, 1);
        if (width != w || height != h) {
            final int[][] newBuffer = new int[h][w];
            for (int[] row : newBuffer) Arrays.fill(row, 0x20);
            final short[][] newColor = new short[h][w];
            for (short[] row : newColor) Arrays.fill(row, packed);
            for (int y = 0; y < Math.min(h, height); y++) {
                System.arraycopy(buffer[y], 0, newBuffer[y], 0, Math.min(w, width));
                System.arraycopy(color[y], 0, newColor[y], 0, Math.min(w, width));
            }
            buffer = newBuffer;
            color = newColor;
            width = w;
            height = h;
            return true;
        } else return false;
    }

    public boolean setSize(Pair<Integer, Integer> value) {
        return setSize(value.getLeft(), value.getRight());
    }

    /** Get the code point at the specified index. */
    public int get(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height)
            throw new IndexOutOfBoundsException();
        else return buffer[row][col];
    }

    /** String based fill starting at a specified location. */
    public boolean set(int col, int row, String s, boolean vertical) {
        final int sLength = ExtendedUnicodeHelper.length(s);
        if (vertical) {
            if (col < 0 || col >= width) return false;
            else {
                boolean changed = false;
                int cx = 0;
                for (int y = row; y < Math.min(row + sLength, height); y++) {
                    if (y >= 0) {
                        final int[] line = buffer[y];
                        final short[] lineColor = color[y];
                        final int c = s.codePointAt(cx);
                        changed = changed || (line[col] != c) || (lineColor[col] != packed);
                        setChar(line, lineColor, col, c);
                    }
                    cx = s.offsetByCodePoints(cx, 1);
                }
                return changed;
            }
        } else {
            if (row < 0 || row >= height) return false;
            else {
                boolean changed = false;
                final int[] line = buffer[row];
                final short[] lineColor = color[row];
                int bx = Math.max(col, 0);
                // Skip the code points left of the buffer (negative col).
                int cx = col < 0 ? s.offsetByCodePoints(0, Math.min(-col, sLength)) : 0;
                for (int x = bx; x < Math.min(col + sLength, width); x++) {
                    if (bx < line.length) {
                        final int c = s.codePointAt(cx);
                        changed = changed || (line[bx] != c) || (lineColor[bx] != packed);
                        setChar(line, lineColor, bx, c);
                        bx += Math.max(1, FontUtils.wcwidth(c));
                        cx = s.offsetByCodePoints(cx, 1);
                    }
                }
                return changed;
            }
        }
    }

    /** Fills an area of the buffer with the specified character. */
    public boolean fill(int col, int row, int w, int h, int c) {
        // Anything to do at all?
        if (w <= 0 || h <= 0) return false;
        if (col + w < 0 || row + h < 0 || col >= width || row >= height) return false;
        boolean changed = false;
        for (int y = Math.max(row, 0); y < Math.min(row + h, height); y++) {
            final int[] line = buffer[y];
            final short[] lineColor = color[y];
            int bx = Math.max(col, 0);
            for (int x = bx; x < Math.min(col + w, width); x++) {
                if (bx < line.length) {
                    changed = changed || (line[bx] != c) || (lineColor[bx] != packed);
                    setChar(line, lineColor, bx, c);
                    bx += Math.max(1, FontUtils.wcwidth(c));
                }
            }
        }
        return changed;
    }

    /** Copies a portion of the buffer. */
    public boolean copy(int col, int row, int w, int h, int tx, int ty) {
        // Anything to do at all?
        if (w <= 0 || h <= 0) return false;
        if (tx == 0 && ty == 0) return false;
        // Loop over the target rectangle, starting from the directions away from
        // the source rectangle and copy the data. This way we ensure we don't
        // overwrite anything we still need to copy.
        int dx0 = Math.max(0, Math.min(width - 1, col + tx + w - 1));
        int dx1 = Math.max(0, Math.min(width, col + tx));
        if (!(tx > 0)) {
            final int tmp = dx0;
            dx0 = dx1;
            dx1 = tmp;
        }
        final int left_edge = Math.min(dx0, dx1) - 1;
        if (left_edge >= width - 1) return false; // no work
        int dy0 = Math.max(0, Math.min(height - 1, row + ty + h - 1));
        int dy1 = Math.max(0, Math.min(height, row + ty));
        if (!(ty > 0)) {
            final int tmp = dy0;
            dy0 = dy1;
            dy1 = tmp;
        }
        final int sx = tx > 0 ? -1 : 1;
        final int sy = ty > 0 ? -1 : 1;
        // Copy values to destination rectangle if there source is valid.
        boolean changed = false;
        for (int ny = dy0; sy > 0 ? ny <= dy1 : ny >= dy1; ny += sy) {
            final int[] nl = buffer[ny];
            final short[] nc = color[ny];
            final int oy = ny - ty;
            if (oy >= 0 && oy < height) {
                final int[] ol = buffer[oy];
                final short[] oc = color[oy];
                for (int nx = dx0; sx > 0 ? nx <= dx1 : nx >= dx1; nx += sx) {
                    final int ox = nx - tx;
                    if (ox >= 0 && ox < width) {
                        changed = changed || (nl[nx] != ol[ox]) || (nc[nx] != oc[ox]);
                        nl[nx] = ol[ox];
                        nc[nx] = oc[ox];
                        for (int offset = 1; offset < FontUtils.wcwidth(nl[nx]); offset++) {
                            nl[nx + offset] = ' ';
                            nc[nx + offset] = oc[nx];
                        }
                    } /* else: Got no source column. */
                }
                // any wide chars along the left edge of the target rectangle need to be cleared
                // don't change their colors
                if (left_edge >= 0 && FontUtils.wcwidth(nl[left_edge]) > 1) {
                    nl[left_edge] = ' ';
                    changed = true;
                }
            } /* else: Got no source row. */
        }
        return changed;
    }

    // copy a portion of another buffer into this buffer
    public boolean rawcopy(int col, int row, int w, int h, TextBuffer src, int fromCol, int fromRow) {
        boolean changed = false;
        final int col_index = col - 1;
        final int row_index = row - 1;
        for (int yOffset = 0; yOffset < h; yOffset++) {
            final int[] dstCharLine = buffer[row_index + yOffset];
            final short[] dstColorLine = color[row_index + yOffset];
            for (int xOffset = 0; xOffset < w; xOffset++) {
                final int srcChar = src.buffer[fromRow + yOffset - 1][fromCol + xOffset - 1];
                short srcColor = src.color[fromRow + yOffset - 1][fromCol + xOffset - 1];

                if (this.format().depth() != src.format().depth()) {
                    final PackedColor.Color fg = new PackedColor.Color(PackedColor.unpackForeground(srcColor, src.format()));
                    final PackedColor.Color bg = new PackedColor.Color(PackedColor.unpackBackground(srcColor, src.format()));
                    srcColor = PackedColor.pack(fg, bg, format());
                }

                if (srcChar != dstCharLine[col_index + xOffset] || srcColor != dstColorLine[col_index + xOffset]) {
                    changed = true;
                    dstCharLine[col_index + xOffset] = srcChar;
                    dstColorLine[col_index + xOffset] = srcColor;
                }
            }
        }

        return changed;
    }

    private void setChar(int[] line, short[] lineColor, int x, int c) {
        if (FontUtils.wcwidth(c) > 1 && x >= line.length - 1) {
            // Don't allow setting wide chars in right-most col.
            return;
        }
        line[x] = c;
        lineColor[x] = packed;
        for (int x1 = x + 1; x1 < x + FontUtils.wcwidth(c); x1++) {
            line[x1] = ' ';
            lineColor[x1] = packed;
        }
        if (x > 0 && FontUtils.wcwidth(line[x - 1]) > 1) {
            // remove previous wide char (but don't change its color)
            line[x - 1] = ' ';
        }
    }

    public void loadData(CompoundTag nbt) {
        final Pair<Integer, Integer> last = Settings.screenResolutionsByTier[Settings.screenResolutionsByTier.length - 1];
        final int maxResolution = Math.max(last.getLeft(), last.getRight());
        final int w = Math.max(Math.min(nbt.getInt("width"), maxResolution), 1);
        final int h = Math.max(Math.min(nbt.getInt("height"), maxResolution), 1);
        setSize(w, h);

        final ListTag b = nbt.getList("buffer", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(h, b.size()); i++) {
            final String value = b.getString(i);
            final java.util.PrimitiveIterator.OfInt it = value.codePoints().iterator();
            for (int j = 0; j < buffer[i].length && it.hasNext(); j++) {
                buffer[i][j] = it.nextInt();
            }
        }

        final li.cil.oc.api.internal.TextBuffer.ColorDepth[] depths = li.cil.oc.api.internal.TextBuffer.ColorDepth.values();
        final li.cil.oc.api.internal.TextBuffer.ColorDepth depth = depths[Math.max(Math.min(nbt.getInt("depth"), depths.length - 1), 0)];
        _format = PackedColor.Depth.format(depth);
        _format.loadData(nbt);
        setForeground(new PackedColor.Color(nbt.getInt("foreground"), nbt.getBoolean("foregroundIsPalette")));
        setBackground(new PackedColor.Color(nbt.getInt("background"), nbt.getBoolean("backgroundIsPalette")));

        if (!NbtDataStream.getShortArray(nbt, "colors", color, w, h)) {
            NbtDataStream.getIntArrayLegacy(nbt, "color", color, w, h);
        }
    }

    public void saveData(CompoundTag nbt) {
        nbt.putInt("width", width);
        nbt.putInt("height", height);

        final ListTag b = new ListTag();
        for (int i = 0; i < height; i++) {
            b.add(StringTag.valueOf(lineToString(i)));
        }
        nbt.put("buffer", b);

        nbt.putInt("depth", _format.depth().ordinal());
        _format.saveData(nbt);
        nbt.putInt("foreground", _foreground.value);
        nbt.putBoolean("foregroundIsPalette", _foreground.isPalette);
        nbt.putInt("background", _background.value);
        nbt.putBoolean("backgroundIsPalette", _background.isPalette);

        final short[] flat = new short[width * height];
        int i = 0;
        for (short[] row : color) {
            for (short value : row) {
                flat[i++] = value;
            }
        }
        NbtDataStream.setShortArray(nbt, "colors", flat);
    }

    /** The specified row as a string (all columns, including trailing blanks). */
    public String lineToString(int y) {
        final StringBuilder b = new StringBuilder(width);
        for (int x = 0; x < width; x++) {
            b.appendCodePoint(buffer[y][x]);
        }
        return b.toString();
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        for (int y = 0; y < height; y++) {
            if (y > 0) b.append('\n');
            b.append(lineToString(y));
        }
        return b.toString();
    }
}
