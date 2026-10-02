package li.cil.oc.server.machine.luac;

import li.cil.oc.Settings;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersistenceAPI extends NativeLuaAPI {
    private String persistKey = "__persist" + UUID.randomUUID().toString().replaceAll("-", "");

    public PersistenceAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        final LuaState lua = lua();

        // Will be replaced by old value in load.
        ExtendedLuaState.pushScalaFunction(lua, l -> {
            l.pushString(persistKey);
            return 1;
        });
        lua.setGlobal("persistKey");

        if (Settings.get().allowPersistence) {
            // These tables must contain all java callbacks (i.e. C functions, since
            // they are wrapped on the native side using a C function, of course).
            // They are used when persisting/unpersisting the state so that the
            // persistence library knows which values it doesn't have to serialize
            // (since it cannot persist C functions).
            lua.newTable(); /* ... perms */
            lua.newTable(); /* ... uperms */

            final int perms = lua.getTop() - 1;
            final int uperms = lua.getTop();

            // Mark everything that's globally reachable at this point as permanent.
            lua.pushString("_G"); /* ... perms uperms k */
            lua.getGlobal("_G"); /* ... perms uperms k v */

            flattenAndStore(lua, perms, uperms); /* ... perms uperms */
            lua.setField(lua.getRegistryIndex(), "uperms"); /* ... perms */
            lua.setField(lua.getRegistryIndex(), "perms"); /* ... */
        }
    }

    private static void flattenAndStore(LuaState lua, int perms, int uperms) {
        /* ... k v */
        // We only care for tables and functions, any value types are safe.
        if (lua.isFunction(-1) || lua.isTable(-1)) {
            lua.pushValue(-2); /* ... k v k */
            lua.getTable(uperms); /* ... k v uperms[k] */
            assert lua.isNil(-1) : "duplicate permanent value named " + lua.toString(-3);
            lua.pop(1); /* ... k v */
            // If we have aliases its enough to store the value once.
            lua.pushValue(-1); /* ... k v v */
            lua.getTable(perms); /* ... k v perms[v] */
            final boolean isNew = lua.isNil(-1);
            lua.pop(1); /* ... k v */
            if (isNew) {
                lua.pushValue(-1); /* ... k v v */
                lua.pushValue(-3); /* ... k v v k */
                lua.rawSet(perms); /* ... k v ; perms[v] = k */
                lua.pushValue(-2); /* ... k v k */
                lua.pushValue(-2); /* ... k v k v */
                lua.rawSet(uperms); /* ... k v ; uperms[k] = v */
                // Recurse into tables.
                if (lua.isTable(-1)) {
                    // Enforce a deterministic order when determining the keys, to ensure
                    // the keys are the same when unpersisting again.
                    final String key = lua.toString(-2);
                    final List<String> childKeys = new ArrayList<>();
                    lua.pushNil(); /* ... k v nil */
                    while (lua.next(-2)) {
                        /* ... k v ck cv */
                        lua.pop(1); /* ... k v ck */
                        childKeys.add(lua.toString(-1));
                    }
                    /* ... k v */
                    // Note: the original (Scala) code discarded the result of the sort,
                    // so iteration order is the table's next() order. Keep it that way
                    // to stay compatible with existing saves.
                    for (String childKey : childKeys) {
                        lua.pushString(key + "." + childKey); /* ... k v ck */
                        lua.getField(-2, childKey); /* ... k v ck cv */
                        flattenAndStore(lua, perms, uperms); /* ... k v */
                    }
                    /* ... k v */
                }
                /* ... k v */
            }
            /* ... k v */
        }
        lua.pop(2); /* ... */
    }

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        if (nbt.contains("persistKey")) {
            persistKey = nbt.getString("persistKey");
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        nbt.putString("persistKey", persistKey);
    }

    public void configure() {
        final LuaState lua = lua();
        lua.getGlobal("eris");

        lua.getField(-1, "settings");
        lua.pushString("spkey");
        lua.pushString(persistKey);
        lua.call(2, 0);

        lua.getField(-1, "settings");
        lua.pushString("path");
        lua.pushBoolean(Settings.get().debugPersistence);
        lua.call(2, 0);

        lua.pop(1);
    }

    public byte[] persist(int index) {
        if (Settings.get().allowPersistence) {
            final LuaState lua = lua();
            configure();
            try {
                lua.gc(LuaState.GcAction.STOP, 0);
                lua.getGlobal("eris"); // ... eris
                lua.getField(-1, "persist"); // ... eris persist
                if (lua.isFunction(-1)) {
                    lua.getField(lua.getRegistryIndex(), "perms"); // ... eris persist perms
                    lua.pushValue(index); // ... eris persist perms obj
                    try {
                        lua.call(2, 1); // ... eris str?
                    } catch (Throwable e) {
                        lua.pop(1);
                        throw e;
                    }
                    if (lua.isString(-1)) {
                        // ... eris str
                        final byte[] result = lua.toByteArray(-1);
                        lua.pop(2); // ...
                        return result;
                    } // ... eris :(
                } // ... eris :(
                lua.pop(2); // ...
            } finally {
                lua.gc(LuaState.GcAction.RESTART, 0);
            }
        }
        return new byte[0];
    }

    public boolean unpersist(byte[] value) {
        if (Settings.get().allowPersistence) {
            final LuaState lua = lua();
            configure();
            try {
                lua.gc(LuaState.GcAction.STOP, 0);
                lua.getGlobal("eris"); // ... eris
                lua.getField(-1, "unpersist"); // ... eris unpersist
                if (lua.isFunction(-1)) {
                    lua.getField(lua.getRegistryIndex(), "uperms"); // ... eris persist uperms
                    lua.pushByteArray(value); // ... eris unpersist uperms str
                    lua.call(2, 1); // ... eris obj
                    lua.insert(-2); // ... obj eris
                    lua.pop(1);
                    return true;
                } // ... :(
                lua.pop(1);
            } finally {
                lua.gc(LuaState.GcAction.RESTART, 0);
            }
        }
        return false;
    }
}
