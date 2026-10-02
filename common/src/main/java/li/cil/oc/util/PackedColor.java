package li.cil.oc.util;

import li.cil.oc.Settings;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.internal.TextBuffer;
import net.minecraft.nbt.CompoundTag;

public final class PackedColor {
    private PackedColor() {
    }

    public static final class Depth {
        private Depth() {
        }

        public static int bits(TextBuffer.ColorDepth depth) {
            switch (depth) {
                case OneBit:
                    return 1;
                case FourBit:
                    return 4;
                case EightBit:
                    return 8;
                default:
                    throw new IllegalArgumentException();
            }
        }

        public static ColorFormat format(TextBuffer.ColorDepth depth) {
            switch (depth) {
                case OneBit:
                    return SingleBitFormat.INSTANCE;
                case FourBit:
                    return new MutablePaletteFormat();
                case EightBit:
                    return new HybridFormat();
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private static final int rShift32 = 16;
    private static final int gShift32 = 8;
    private static final int bShift32 = 0;

    private static int[] extract(int value) {
        final int r = (value >>> rShift32) & 0xFF;
        final int g = (value >>> gShift32) & 0xFF;
        final int b = (value >>> bShift32) & 0xFF;
        return new int[]{r, g, b};
    }

    public interface ColorFormat extends Persistable {
        TextBuffer.ColorDepth depth();

        int inflate(int value);

        default void validate(Color value) {
            if (value.isPalette) {
                throw new IllegalArgumentException("color palette not supported");
            }
        }

        byte deflate(Color value);

        default boolean isFromPalette(int value) {
            return false;
        }

        @Override
        default void loadData(CompoundTag nbt) {
        }

        @Override
        default void saveData(CompoundTag nbt) {
        }
    }

    public static class SingleBitFormat implements ColorFormat {
        /** Scala `object SingleBitFormat extends SingleBitFormat(Settings.get.monochromeColor)`. */
        public static final SingleBitFormat INSTANCE = new SingleBitFormat(Settings.get().monochromeColor);

        public final int color;

        public SingleBitFormat(int color) {
            this.color = color;
        }

        @Override
        public TextBuffer.ColorDepth depth() {
            return TextBuffer.ColorDepth.OneBit;
        }

        @Override
        public int inflate(int value) {
            return value == 0 ? 0x000000 : color;
        }

        @Override
        public byte deflate(Color value) {
            return (byte) (value.value == 0 ? 0 : 1);
        }
    }

    public abstract static class PaletteFormat implements ColorFormat {
        @Override
        public int inflate(int value) {
            final int[] palette = palette();
            return palette[Math.max(0, Math.min(palette.length - 1, value))];
        }

        @Override
        public void validate(Color value) {
            if (value.isPalette && (value.value < 0 || value.value >= palette().length)) {
                throw new IllegalArgumentException("invalid palette index");
            }
        }

        @Override
        public byte deflate(Color value) {
            final int[] palette = palette();
            if (value.isPalette) return (byte) (Math.max(0, value.value) % palette.length);
            else {
                int bestIndex = 0;
                double best = Double.POSITIVE_INFINITY;
                for (int i = 0; i < palette.length; i++) {
                    final double d = delta(value.value, palette[i]);
                    if (d < best) {
                        best = d;
                        bestIndex = i;
                    }
                }
                return (byte) bestIndex;
            }
        }

        @Override
        public boolean isFromPalette(int value) {
            return true;
        }

        protected abstract int[] palette();

        protected double delta(int colorA, int colorB) {
            final int[] a = extract(colorA);
            final int[] b = extract(colorB);
            final int dr = a[0] - b[0];
            final int dg = a[1] - b[1];
            final int db = a[2] - b[2];
            return 0.2126 * dr * dr + 0.7152 * dg * dg + 0.0722 * db * db;
        }
    }

    public static class MutablePaletteFormat extends PaletteFormat {
        @Override
        public TextBuffer.ColorDepth depth() {
            return TextBuffer.ColorDepth.FourBit;
        }

        public int apply(int index) {
            return palette[index];
        }

        public void update(int index, int value) {
            palette[index] = value;
        }

        protected final int[] palette = new int[]{
                0xFFFFFF, 0xFFCC33, 0xCC66CC, 0x6699FF,
                0xFFFF33, 0x33CC33, 0xFF6699, 0x333333,
                0xCCCCCC, 0x336699, 0x9933CC, 0x333399,
                0x663300, 0x336600, 0xFF3333, 0x000000};

        @Override
        protected int[] palette() {
            return palette;
        }

        @Override
        public void loadData(CompoundTag nbt) {
            final int[] loaded = nbt.getIntArray("palette");
            System.arraycopy(loaded, 0, palette, 0, Math.min(loaded.length, palette.length));
        }

        @Override
        public void saveData(CompoundTag nbt) {
            nbt.putIntArray("palette", palette);
        }
    }

    public static class HybridFormat extends MutablePaletteFormat {
        private static final int reds = 6;
        private static final int greens = 8;
        private static final int blues = 5;

        public HybridFormat() {
            // Initialize palette to grayscale, excluding black and white, because
            // those are already contained in the normal color cube.
            for (int i = 0; i < palette.length; i++) {
                final int shade = 0xFF * (i + 1) / (palette.length + 1);
                this.update(i, (shade << rShift32) | (shade << gShift32) | (shade << bShift32));
            }
        }

        @Override
        public TextBuffer.ColorDepth depth() {
            return TextBuffer.ColorDepth.EightBit;
        }

        @Override
        public int inflate(int value) {
            if (isFromPalette(value)) return super.inflate(value);
            else {
                final int index = value - palette.length;
                final int idxB = index % blues;
                final int idxG = (index / blues) % greens;
                final int idxR = (index / blues / greens) % reds;
                final int r = (int) (idxR * 0xFF / (reds - 1.0) + 0.5);
                final int g = (int) (idxG * 0xFF / (greens - 1.0) + 0.5);
                final int b = (int) (idxB * 0xFF / (blues - 1.0) + 0.5);
                return (r << rShift32) | (g << gShift32) | (b << bShift32);
            }
        }

        @Override
        public byte deflate(Color value) {
            final byte paletteIndex = super.deflate(value);
            if (value.isPalette) return paletteIndex;
            else {
                final int[] rgb = extract(value.value);
                final int idxR = (int) (rgb[0] * (reds - 1.0) / 0xFF + 0.5);
                final int idxG = (int) (rgb[1] * (greens - 1.0) / 0xFF + 0.5);
                final int idxB = (int) (rgb[2] * (blues - 1.0) / 0xFF + 0.5);
                final byte deflated = (byte) (palette.length + idxR * greens * blues + idxG * blues + idxB);
                if (delta(inflate(deflated & 0xFF), value.value) < delta(inflate(paletteIndex & 0xFF), value.value)) {
                    return deflated;
                } else {
                    return paletteIndex;
                }
            }
        }

        @Override
        public boolean isFromPalette(int value) {
            return value >= 0 && value < palette.length;
        }
    }

    /** Scala `case class Color(value: Int, isPalette: Boolean = false)`. */
    public static final class Color {
        public final int value;
        public final boolean isPalette;

        public Color(int value, boolean isPalette) {
            this.value = value;
            this.isPalette = isPalette;
        }

        public Color(int value) {
            this(value, false);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Color that) {
                return value == that.value && isPalette == that.isPalette;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * value + (isPalette ? 1 : 0);
        }

        @Override
        public String toString() {
            return "Color(" + value + "," + isPalette + ")";
        }
    }

    // Colors are packed: 0xFFBB (F = foreground, B = background)
    public static final int ForegroundShift = 8;
    public static final int BackgroundMask = 0x000000FF;

    public static short pack(Color foreground, Color background, ColorFormat format) {
        return (short) (((format.deflate(foreground) & 0xFF) << ForegroundShift) | (format.deflate(background) & 0xFF));
    }

    public static int extractForeground(short color) {
        return (color & 0xFFFF) >>> ForegroundShift;
    }

    public static int extractBackground(short color) {
        return color & BackgroundMask;
    }

    public static int unpackForeground(short color, ColorFormat format) {
        return format.inflate(extractForeground(color));
    }

    public static int unpackBackground(short color, ColorFormat format) {
        return format.inflate(extractBackground(color));
    }
}
