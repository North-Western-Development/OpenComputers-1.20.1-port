package li.cil.oc.server.machine.luac;

import li.cil.oc.server.machine.ArchitectureAPI;
import li.cil.repack.com.naef.jnlua.LuaState;

public abstract class NativeLuaAPI extends ArchitectureAPI {
    public final NativeLuaArchitecture owner;

    protected NativeLuaAPI(NativeLuaArchitecture owner) {
        super(owner.machine);
        this.owner = owner;
    }

    protected LuaState lua() {
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
