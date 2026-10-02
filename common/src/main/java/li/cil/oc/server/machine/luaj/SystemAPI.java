package li.cil.oc.server.machine.luaj;

import li.cil.oc.Settings;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;

public class SystemAPI extends LuaJAPI {
    public SystemAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        final LuaTable system = LuaValue.tableOf();

        // Whether bytecode may be loaded directly.
        system.set("allowBytecode", new ScalaClosure(args -> LuaValue.valueOf(Settings.get().allowBytecode)));

        // Whether custom __gc callbacks are allowed.
        system.set("allowGC", new ScalaClosure(args -> LuaValue.valueOf(Settings.get().allowGC)));

        // How long programs may run without yielding before we stop them.
        system.set("timeout", new ScalaClosure(args -> LuaValue.valueOf(Settings.get().timeout)));

        lua().set("system", system);
    }
}
