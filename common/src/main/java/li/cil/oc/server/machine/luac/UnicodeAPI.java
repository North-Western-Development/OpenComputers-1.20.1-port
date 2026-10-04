package li.cil.oc.server.machine.luac;

import li.cil.oc.util.ExtendedLuaState;
import li.cil.oc.util.ExtendedUnicodeHelper;
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
            final StringBuilder builder = new StringBuilder();
            for (int i = 1; i <= lua.getTop(); i++) builder.appendCodePoint(lua.checkInt32(i));
            lua.pushString(builder.toString());
            return 1;
        });
        lua().setField(-2, "char");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushInteger(ExtendedUnicodeHelper.length(lua.checkString(1)));
            return 1;
        });
        lua().setField(-2, "len");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushString(lua.checkString(1).toLowerCase());
            return 1;
        });
        lua().setField(-2, "lower");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushString(ExtendedUnicodeHelper.reverse(lua.checkString(1)));
            return 1;
        });
        lua().setField(-2, "reverse");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String string = lua.checkString(1);
            final int i1 = lua.checkInt32(2);
            final int i2 = lua.getTop() > 2 ? lua.checkInt32(3) : Integer.MAX_VALUE;
            lua.pushString(ExtendedUnicodeHelper.sub(string, i1, i2));
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
            lua.pushInteger(ExtendedUnicodeHelper.wlen(value));
            return 1;
        });
        lua().setField(-2, "wlen");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushString(ExtendedUnicodeHelper.wtrunc(lua.checkString(1), lua.checkInteger(2)));
            return 1;
        });
        lua().setField(-2, "wtrunc");

        lua().setGlobal("unicode");
    }
}
