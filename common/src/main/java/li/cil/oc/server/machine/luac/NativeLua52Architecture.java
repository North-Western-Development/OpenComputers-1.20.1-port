package li.cil.oc.server.machine.luac;

import li.cil.oc.api.machine.Architecture;

@Architecture.Name("Lua 5.2")
public class NativeLua52Architecture extends NativeLuaArchitecture {
    public NativeLua52Architecture(li.cil.oc.api.machine.Machine machine) {
        super(machine);
    }

    @Override
    protected LuaStateFactory factory() {
        return LuaStateFactory.Lua52.INSTANCE;
    }
}
