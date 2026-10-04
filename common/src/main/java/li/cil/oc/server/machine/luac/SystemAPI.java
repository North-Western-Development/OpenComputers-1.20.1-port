package li.cil.oc.server.machine.luac;

import li.cil.oc.Settings;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaType;

import java.util.ArrayList;
import java.util.List;

public class SystemAPI extends NativeLuaAPI {
    public SystemAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        // Until we get to ingame screens we log to Java's stdout.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final List<String> parts = new ArrayList<>();
            for (int i = 1; i <= lua.getTop(); i++) {
                final LuaType type = lua.type(i);
                final String part;
                switch (type) {
                    case NIL:
                        part = "nil";
                        break;
                    case BOOLEAN:
                        part = String.valueOf(lua.toBoolean(i));
                        break;
                    case NUMBER:
                        part = lua.isInteger(i) ? String.valueOf(lua.toInteger(i)) : String.valueOf(lua.toNumber(i));
                        break;
                    case STRING:
                        part = lua.toString(i);
                        break;
                    case TABLE:
                        part = "table";
                        break;
                    case FUNCTION:
                        part = "function";
                        break;
                    case THREAD:
                        part = "thread";
                        break;
                    default:
                        part = "userdata";
                        break;
                }
                parts.add(part);
            }
            System.out.println(String.join("  ", parts));
            return 0;
        });
        lua().setGlobal("print");

        // Create system table, avoid magic global non-tables.
        lua().newTable();

        // Whether bytecode may be loaded directly.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushBoolean(Settings.get().allowBytecode);
            return 1;
        });
        lua().setField(-2, "allowBytecode");

        // Whether custom __gc callbacks are allowed.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushBoolean(Settings.get().allowGC);
            return 1;
        });
        lua().setField(-2, "allowGC");

        // How long programs may run without yielding before we stop them.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushNumber(Settings.get().timeout);
            return 1;
        });
        lua().setField(-2, "timeout");

        lua().setGlobal("system");
    }
}
