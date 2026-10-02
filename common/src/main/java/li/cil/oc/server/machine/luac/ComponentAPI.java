package li.cil.oc.server.machine.luac;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaState;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ComponentAPI extends NativeLuaAPI {
    public ComponentAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Map<String, String> components = components();
            synchronized (components) {
                final String filter = lua.isString(1) ? lua.toString(1) : null;
                final boolean exact = lua.isBoolean(2) ? lua.toBoolean(2) : true;
                lua.newTable(0, components.size());
                for (Map.Entry<String, String> entry : components.entrySet()) {
                    final String name = entry.getValue();
                    if (filter == null || (exact ? name.equals(filter) : name.contains(filter))) {
                        lua.pushString(entry.getKey());
                        lua.pushString(name);
                        lua.rawSet(-3);
                    }
                }
                return 1;
            }
        });
        lua().setField(-2, "list");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Map<String, String> components = components();
            synchronized (components) {
                final String name = components.get(lua.checkString(1));
                if (name != null) {
                    lua.pushString(name);
                    return 1;
                }
                lua.pushNil();
                lua.pushString("no such component");
                return 2;
            }
        });
        lua().setField(-2, "type");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Map<String, String> components = components();
            synchronized (components) {
                final String address = lua.checkString(1);
                if (components.get(address) != null) {
                    lua.pushInteger(owner.machine.host().componentSlot(address));
                    return 1;
                }
                lua.pushNil();
                lua.pushString("no such component");
                return 2;
            }
        });
        lua().setField(-2, "slot");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> withComponent(lua.checkString(1), component -> {
            lua.newTable();
            for (Map.Entry<String, Callback> entry : machine.methods(component.host()).entrySet()) {
                final Callback annotation = entry.getValue();
                lua.pushString(entry.getKey());
                lua.newTable();
                lua.pushBoolean(annotation.direct());
                lua.setField(-2, "direct");
                lua.pushBoolean(annotation.getter());
                lua.setField(-2, "getter");
                lua.pushBoolean(annotation.setter());
                lua.setField(-2, "setter");
                lua.rawSet(-3);
            }
            return 1;
        }));
        lua().setField(-2, "methods");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String address = lua.checkString(1);
            final String method = lua.checkString(2);
            final List<Object> args = ExtendedLuaState.toSimpleJavaObjects(lua, 3);
            return owner.invoke(() -> machine.invoke(address, method, args.toArray()));
        });
        lua().setField(-2, "invoke");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> withComponent(lua.checkString(1), component -> {
            final String method = lua.checkString(2);
            final Map<String, Callback> methods = machine.methods(component.host());
            return owner.documentation(() -> {
                final Callback callback = methods.get(method);
                return callback != null ? callback.doc() : null;
            });
        }));
        lua().setField(-2, "doc");

        lua().setGlobal("component");
    }

    private int withComponent(String address, Function<Component, Integer> f) {
        final Node target = node().network().node(address);
        if (target instanceof Component component && (component.canBeSeenFrom(node()) || component == node())) {
            return f.apply(component);
        }
        lua().pushNil();
        lua().pushString("no such component");
        return 2;
    }
}
