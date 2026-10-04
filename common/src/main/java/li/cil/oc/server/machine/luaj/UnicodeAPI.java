package li.cil.oc.server.machine.luaj;

import li.cil.oc.util.ExtendedUnicodeHelper;
import li.cil.oc.util.FontUtils;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.LuaString;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;

import java.nio.charset.StandardCharsets;

public class UnicodeAPI extends LuaJAPI {
    public UnicodeAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    /**
     * Like {@code args.checkjstring(i)}, but decodes 4 byte UTF-8 sequences (characters
     * outside the BMP) properly, which LuaJ's own decoder does not.
     */
    private static String checkString(Varargs args, int i) {
        final LuaString s = args.checkstring(i);
        return new String(s.m_bytes, s.m_offset, s.m_length, StandardCharsets.UTF_8);
    }

    @Override
    public void initialize() {
        // Provide some better Unicode support.
        final LuaTable unicode = LuaValue.tableOf();

        unicode.set("lower", new ScalaClosure(args -> LuaValue.valueOf(checkString(args, 1).toLowerCase())));

        unicode.set("upper", new ScalaClosure(args -> LuaValue.valueOf(checkString(args, 1).toUpperCase())));

        unicode.set("char", new ScalaClosure(args -> {
            final StringBuilder builder = new StringBuilder();
            for (int i = 1; i <= args.narg(); i++) builder.appendCodePoint(args.checkint(i));
            return LuaValue.valueOf(builder.toString());
        }));

        unicode.set("len", new ScalaClosure(args -> LuaValue.valueOf(ExtendedUnicodeHelper.length(checkString(args, 1)))));

        unicode.set("reverse", new ScalaClosure(args -> LuaValue.valueOf(ExtendedUnicodeHelper.reverse(checkString(args, 1)))));

        unicode.set("sub", new ScalaClosure(args -> {
            final String string = checkString(args, 1);
            final int i1 = args.checkint(2);
            final int i2 = args.narg() > 2 ? args.checkint(3) : Integer.MAX_VALUE;
            return LuaValue.valueOf(ExtendedUnicodeHelper.sub(string, i1, i2));
        }));

        unicode.set("isWide", new ScalaClosure(args ->
                LuaValue.valueOf(FontUtils.wcwidth(checkString(args, 1).codePointAt(0)) > 1)));

        unicode.set("charWidth", new ScalaClosure(args ->
                LuaValue.valueOf(FontUtils.wcwidth(checkString(args, 1).codePointAt(0)))));

        unicode.set("wlen", new ScalaClosure(args ->
                LuaValue.valueOf(ExtendedUnicodeHelper.wlen(checkString(args, 1)))));

        unicode.set("wtrunc", new ScalaClosure(args ->
                LuaValue.valueOf(ExtendedUnicodeHelper.wtrunc(checkString(args, 1), args.checkint(2)))));

        lua().set("unicode", unicode);
    }
}
