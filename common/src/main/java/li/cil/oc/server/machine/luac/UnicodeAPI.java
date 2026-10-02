package li.cil.oc.server.machine.luac;

import li.cil.oc.util.ExtendedLuaState;
import li.cil.oc.util.FontUtils;

public class UnicodeAPI extends NativeLuaAPI {
    public UnicodeAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        // Provide some better Unicode support.
        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final char[] chars = new char[lua.getTop()];
            for (int i = 1; i <= chars.length; i++) chars[i - 1] = (char) lua.checkInt32(i);
            lua.pushString(String.valueOf(chars));
            return 1;
        });
        lua().setField(-2, "char");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushInteger(lua.checkString(1).length());
            return 1;
        });
        lua().setField(-2, "len");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushString(lua.checkString(1).toLowerCase());
            return 1;
        });
        lua().setField(-2, "lower");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final char[] chars = lua.checkString(1).toCharArray();
            for (int i = 0, j = chars.length - 1; i < j; i++, j--) {
                final char tmp = chars[i];
                chars[i] = chars[j];
                chars[j] = tmp;
            }
            lua.pushString(new String(chars));
            return 1;
        });
        lua().setField(-2, "reverse");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String string = lua.checkString(1);
            final int i1 = lua.checkInt32(2);
            final int start = Math.max(0, i1 < 0 ? string.length() + i1 : i1 - 1);
            final int end;
            if (lua.getTop() > 2) {
                final int i2 = lua.checkInt32(3);
                end = Math.min(string.length(), i2 < 0 ? string.length() + i2 + 1 : i2);
            } else end = string.length();
            if (end <= start) lua.pushString("");
            else lua.pushString(string.substring(start, end));
            return 1;
        });
        lua().setField(-2, "sub");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushString(lua.checkString(1).toUpperCase());
            return 1;
        });
        lua().setField(-2, "upper");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushBoolean(FontUtils.wcwidth(lua.checkString(1).codePointAt(0)) > 1);
            return 1;
        });
        lua().setField(-2, "isWide");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushInteger(FontUtils.wcwidth(lua.checkString(1).codePointAt(0)));
            return 1;
        });
        lua().setField(-2, "charWidth");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String value = lua.checkString(1);
            int sum = 0;
            for (char ch : value.toCharArray()) sum += Math.max(1, FontUtils.wcwidth(ch));
            lua.pushInteger(sum);
            return 1;
        });
        lua().setField(-2, "wlen");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String value = lua.checkString(1);
            final long count = lua.checkInteger(2);
            int width = 0;
            int end = 0;
            while (width < count) {
                width += Math.max(1, FontUtils.wcwidth(value.charAt(end)));
                end += 1;
            }
            if (end > 1) lua.pushString(value.substring(0, end - 1));
            else lua.pushString("");
            return 1;
        });
        lua().setField(-2, "wtrunc");

        lua().setGlobal("unicode");
    }
}
