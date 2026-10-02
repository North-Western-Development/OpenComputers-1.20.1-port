package li.cil.oc.server.machine.luaj;

import li.cil.oc.server.machine.ArchitectureAPI;
import li.cil.repack.org.luaj.vm2.Globals;

public abstract class LuaJAPI extends ArchitectureAPI {
    public final LuaJLuaArchitecture owner;

    protected LuaJAPI(LuaJLuaArchitecture owner) {
        super(owner.machine);
        this.owner = owner;
    }

    protected Globals lua() {
        return owner.lua;
    }

    /**
     * Rethrows checked exceptions unchanged from within closures (Scala did
     * not distinguish checked exceptions).
     */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
