package li.cil.oc.server.machine.luaj;

import li.cil.oc.util.FontUtils;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;

public class UnicodeAPI extends LuaJAPI {
    public UnicodeAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        // Provide some better Unicode support.
        final LuaTable unicode = LuaValue.tableOf();

        unicode.set("lower", new ScalaClosure(args -> LuaValue.valueOf(args.checkjstring(1).toLowerCase())));

        unicode.set("upper", new ScalaClosure(args -> LuaValue.valueOf(args.checkjstring(1).toUpperCase())));

        unicode.set("char", new ScalaClosure(args -> {
            final char[] chars = new char[args.narg()];
            for (int i = 1; i <= args.narg(); i++) chars[i - 1] = (char) args.checkint(i);
            return LuaValue.valueOf(String.valueOf(chars));
        }));

        unicode.set("len", new ScalaClosure(args -> LuaValue.valueOf(args.checkjstring(1).length())));

        unicode.set("reverse", new ScalaClosure(args -> {
            final char[] chars = args.checkjstring(1).toCharArray();
            for (int i = 0, j = chars.length - 1; i < j; i++, j--) {
                final char tmp = chars[i];
                chars[i] = chars[j];
                chars[j] = tmp;
            }
            return LuaValue.valueOf(new String(chars));
        }));

        unicode.set("sub", new ScalaClosure(args -> {
            final String string = args.checkjstring(1);
            final int i1 = args.checkint(2);
            final int start = Math.max(0, i1 < 0 ? string.length() + i1 : i1 - 1);
            final int end;
            if (args.narg() > 2) {
                final int i2 = args.checkint(3);
                end = Math.min(string.length(), i2 < 0 ? string.length() + i2 + 1 : i2);
            } else end = string.length();
            if (end <= start) return LuaValue.valueOf("");
            return LuaValue.valueOf(string.substring(start, end));
        }));

        unicode.set("isWide", new ScalaClosure(args ->
                LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0)) > 1)));

        unicode.set("charWidth", new ScalaClosure(args ->
                LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0)))));

        unicode.set("wlen", new ScalaClosure(args -> {
            final String value = args.checkjstring(1);
            int sum = 0;
            for (char ch : value.toCharArray()) sum += Math.max(1, FontUtils.wcwidth(ch));
            return LuaValue.valueOf(sum);
        }));

        unicode.set("wtrunc", new ScalaClosure(args -> {
            final String value = args.checkjstring(1);
            final int count = args.checkint(2);
            int width = 0;
            int end = 0;
            while (width < count) {
                width += Math.max(1, FontUtils.wcwidth(value.charAt(end)));
                end += 1;
            }
            if (end > 1) return LuaValue.valueOf(value.substring(0, end - 1));
            return LuaValue.valueOf("");
        }));

        lua().set("unicode", unicode);
    }
}
