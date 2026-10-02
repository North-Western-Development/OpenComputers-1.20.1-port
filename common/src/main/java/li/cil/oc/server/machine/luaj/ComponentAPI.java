package li.cil.oc.server.machine.luaj;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ComponentAPI extends LuaJAPI {
    public ComponentAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        // Component interaction stuff.
        final LuaTable component = LuaValue.tableOf();

        component.set("list", new ScalaClosure(args -> {
            final Map<String, String> components = components();
            synchronized (components) {
                final String filter = args.isstring(1) ? args.tojstring(1) : null;
                final boolean exact = args.optboolean(2, false);
                final LuaTable table = LuaValue.tableOf(0, components.size());
                for (Map.Entry<String, String> entry : components.entrySet()) {
                    final String name = entry.getValue();
                    if (filter == null || (exact ? name.equals(filter) : name.contains(filter))) {
                        table.set(entry.getKey(), name);
                    }
                }
                return table;
            }
        }));

        component.set("type", new ScalaClosure(args -> {
            final Map<String, String> components = components();
            synchronized (components) {
                final String name = components.get(args.checkjstring(1));
                if (name != null) return LuaValue.valueOf(name);
                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
            }
        }));

        component.set("slot", new ScalaClosure(args -> {
            final Map<String, String> components = components();
            synchronized (components) {
                final String address = args.checkjstring(1);
                if (components.get(address) != null) {
                    return LuaValue.valueOf(machine.host().componentSlot(address));
                }
                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
            }
        }));

        component.set("methods", new ScalaClosure(args -> withComponent(args.checkjstring(1), c -> {
            final LuaTable table = LuaValue.tableOf();
            for (Map.Entry<String, Callback> entry : machine.methods(c.host()).entrySet()) {
                final Callback annotation = entry.getValue();
                table.set(entry.getKey(), LuaValue.tableOf(new LuaValue[]{
                        LuaValue.valueOf("direct"),
                        LuaValue.valueOf(annotation.direct()),
                        LuaValue.valueOf("getter"),
                        LuaValue.valueOf(annotation.getter()),
                        LuaValue.valueOf("setter"),
                        LuaValue.valueOf(annotation.setter())}));
            }
            return table;
        })));

        component.set("invoke", new ScalaClosure(args -> {
            final String address = args.checkjstring(1);
            final String method = args.checkjstring(2);
            final List<Object> params = ScalaClosure.toSimpleJavaObjects(args, 3);
            return owner.invoke(() -> machine.invoke(address, method, params.toArray()));
        }));

        component.set("doc", new ScalaClosure(args -> withComponent(args.checkjstring(1), c -> {
            final String method = args.checkjstring(2);
            final Map<String, Callback> methods = machine.methods(c.host());
            return owner.documentation(() -> {
                final Callback callback = methods.get(method);
                return callback != null ? callback.doc() : null;
            });
        })));

        lua().set("component", component);
    }

    private Varargs withComponent(String address, Function<Component, Varargs> f) {
        final Node target = node().network().node(address);
        if (target instanceof Component c && (c.canBeSeenFrom(node()) || c == node())) {
            return f.apply(c);
        }
        return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
    }
}
