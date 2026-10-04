package li.cil.oc.util;

/**
 * Helper functions for handling strings with characters outside of the Unicode BMP
 * (code points above U+FFFF, which take two chars - a surrogate pair - in Java strings).
 */
public final class ExtendedUnicodeHelper {
    private ExtendedUnicodeHelper() {
    }

    /** The number of code points in the string. */
    public static int length(String s) {
        return s.codePointCount(0, s.length());
    }

    /** Reverses the string by code points, keeping surrogate pairs intact. */
    public static String reverse(String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        for (int i = s.length() - 1; i >= 0; i--) {
            final char c = s.charAt(i);
            if (Character.isLowSurrogate(c) && i > 0) {
                i--;
                final char c2 = s.charAt(i);
                if (Character.isHighSurrogate(c2)) {
                    sb.append(c2).append(c);
                } else {
                    // Invalid surrogate pair?
                    sb.append(c).append(c2);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Substring by code point indices (0 based, end exclusive). */
    public static String substring(String s, int start, int end) {
        return s.substring(s.offsetByCodePoints(0, start), s.offsetByCodePoints(0, end));
    }

    // unicode.* API (native Lua and LuaJ). Indices are in code points, so characters
    // outside the BMP (two chars in Java strings) count as one character.

    /** unicode.sub: Lua string.sub semantics (1 based, negative from the end, inclusive). */
    public static String sub(String string, long i1, long i2) {
        final int sLength = ExtendedUnicodeHelper.length(string);
        final int start;
        if (i1 < 0) start = string.offsetByCodePoints(string.length(), (int) Math.max(i1, -sLength));
        else if (i1 == 0) start = 0;
        else start = string.offsetByCodePoints(0, (int) Math.min(i1 - 1, sLength));
        final int end;
        if (i2 < 0) end = string.offsetByCodePoints(string.length(), (int) Math.max(i2 + 1, -sLength));
        else end = string.offsetByCodePoints(0, (int) Math.min(i2, sLength));
        if (end <= start) return "";
        else return string.substring(start, end);
    }

    /** unicode.wlen: the width of the string in columns. */
    public static int wlen(String value) {
        return value.codePoints().map(ch -> Math.max(1, FontUtils.wcwidth(ch))).sum();
    }

    /** unicode.wtrunc: the longest prefix of the string narrower than count columns. */
    public static String wtrunc(String value, long count) {
        int width = 0;
        int end = 0;
        while (width < count) {
            // Like before: errors with "index out of range" if the string is not wider than count.
            if (end >= value.length()) throw new StringIndexOutOfBoundsException(end);
            width += Math.max(1, FontUtils.wcwidth(value.codePointAt(end)));
            end = value.offsetByCodePoints(end, 1);
        }
        // Drop the character that reached count (end > 1: at least one complete code point remains).
        final int last = end > 0 ? value.offsetByCodePoints(end, -1) : 0;
        if (last > 0) return value.substring(0, last);
        else return "";
    }
}
