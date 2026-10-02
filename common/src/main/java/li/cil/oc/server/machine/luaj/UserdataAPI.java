package li.cil.oc.server.machine.luaj;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Value;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserdataAPI extends LuaJAPI {
    public UserdataAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        final LuaTable userdata = LuaValue.tableOf();

        userdata.set("apply", new ScalaClosure(args -> {
            final Value value = (Value) args.checkuserdata(1, Value.class);
            final List<Object> params = ScalaClosure.toSimpleJavaObjects(args, 2);
            return owner.invoke(() -> Registry.INSTANCE.convert(new Object[]{value.apply(machine, new ArgumentsImpl(params))}));
        }));

        userdata.set("unapply", new ScalaClosure(args -> {
            final Value value = (Value) args.checkuserdata(1, Value.class);
            final List<Object> params = ScalaClosure.toSimpleJavaObjects(args, 2);
            return owner.invoke(() -> {
                value.unapply(machine, new ArgumentsImpl(params));
                return null;
            });
        }));

        userdata.set("call", new ScalaClosure(args -> {
            final Value value = (Value) args.checkuserdata(1, Value.class);
            final List<Object> params = ScalaClosure.toSimpleJavaObjects(args, 2);
            return owner.invoke(() -> Registry.INSTANCE.convert(value.call(machine, new ArgumentsImpl(params))));
        }));

        userdata.set("dispose", new ScalaClosure(args -> {
            final Value value = (Value) args.checkuserdata(1, Value.class);
            try {
                value.dispose(machine);
            } catch (Throwable t) {
                OpenComputers.log.warn("Error in dispose method of userdata of type " + value.getClass().getName(), t);
            }
            return LuaValue.NIL;
        }));

        userdata.set("methods", new ScalaClosure(args -> {
            final Object value = args.checkuserdata(1, Value.class);
            final List<LuaValue> entries = new ArrayList<>();
            for (Map.Entry<String, Callback> entry : machine.methods(value).entrySet()) {
                entries.add(LuaValue.valueOf(entry.getKey()));
                entries.add(LuaValue.valueOf(entry.getValue().direct()));
            }
            return LuaValue.tableOf(entries.toArray(new LuaValue[0]));
        }));

        userdata.set("invoke", new ScalaClosure(args -> {
            final Value value = (Value) args.checkuserdata(1, Value.class);
            final String method = args.checkjstring(2);
            final List<Object> params = ScalaClosure.toSimpleJavaObjects(args, 3);
            return owner.invoke(() -> machine.invoke(value, method, params.toArray()));
        }));

        userdata.set("doc", new ScalaClosure(args -> {
            final Value value = (Value) args.checkuserdata(1, Value.class);
            final String method = args.checkjstring(2);
            return owner.documentation(() -> {
                final Callback callback = machine.methods(value).get(method);
                if (callback == null) throw new java.util.NoSuchElementException("key not found: " + method);
                return callback.doc();
            });
        }));

        lua().set("userdata", userdata);
    }
}
