package li.cil.oc.server.machine.luac;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Value;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.util.ExtendedLuaState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class UserdataAPI extends NativeLuaAPI {
    public UserdataAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final CompoundTag nbt = new CompoundTag();
            final Persistable persistable = (Persistable) lua.toJavaObjectRaw(1);
            lua.pushString(persistable.getClass().getName());
            persistable.saveData(nbt);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);
            try {
                NbtIo.write(nbt, dos);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            lua.pushByteArray(baos.toByteArray());
            return 2;
        });
        lua().setField(-2, "save");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            try {
                final String className = lua.toString(1);
                final Class<?> clazz = Class.forName(className);
                final Persistable persistable = (Persistable) clazz.getDeclaredConstructor().newInstance();
                final byte[] data = lua.toByteArray(2);
                final ByteArrayInputStream bais = new ByteArrayInputStream(data);
                final DataInputStream dis = new DataInputStream(bais);
                final CompoundTag nbt = NbtIo.read(dis);
                persistable.loadData(nbt);
                lua.pushJavaObjectRaw(persistable);
                return 1;
            } catch (Throwable t) {
                OpenComputers.log.warn("Error in userdata load function.", t);
                throw NativeLuaAPI.<RuntimeException>sneakyThrow(t);
            }
        });
        lua().setField(-2, "load");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Value value = (Value) lua.toJavaObjectRaw(1);
            final List<Object> args = ExtendedLuaState.toSimpleJavaObjects(lua, 2);
            return owner.invoke(() -> Registry.INSTANCE.convert(new Object[]{value.apply(machine, new ArgumentsImpl(args))}));
        });
        lua().setField(-2, "apply");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Value value = (Value) lua.toJavaObjectRaw(1);
            final List<Object> args = ExtendedLuaState.toSimpleJavaObjects(lua, 2);
            return owner.invoke(() -> {
                value.unapply(machine, new ArgumentsImpl(args));
                return null;
            });
        });
        lua().setField(-2, "unapply");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Value value = (Value) lua.toJavaObjectRaw(1);
            final List<Object> args = ExtendedLuaState.toSimpleJavaObjects(lua, 2);
            return owner.invoke(() -> Registry.INSTANCE.convert(value.call(machine, new ArgumentsImpl(args))));
        });
        lua().setField(-2, "call");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Value value = (Value) lua.toJavaObjectRaw(1);
            try {
                value.dispose(machine);
            } catch (Throwable t) {
                OpenComputers.log.warn("Error in dispose method of userdata of type " + value.getClass().getName(), t);
            }
            return 0;
        });
        lua().setField(-2, "dispose");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Value value = (Value) lua.toJavaObjectRaw(1);
            final Map<String, Boolean> result = new HashMap<>();
            for (Map.Entry<String, Callback> entry : machine.methods(value).entrySet()) {
                result.put(entry.getKey(), entry.getValue().direct());
            }
            ExtendedLuaState.pushValue(lua, result);
            return 1;
        });
        lua().setField(-2, "methods");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Value value = (Value) lua.toJavaObjectRaw(1);
            final String method = lua.checkString(2);
            final List<Object> args = ExtendedLuaState.toSimpleJavaObjects(lua, 3);
            return owner.invoke(() -> machine.invoke(value, method, args.toArray()));
        });
        lua().setField(-2, "invoke");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final Value value = (Value) lua.toJavaObjectRaw(1);
            final String method = lua.checkString(2);
            return owner.documentation(() -> {
                final Callback callback = machine.methods(value).get(method);
                if (callback == null) throw new NoSuchElementException("key not found: " + method);
                return callback.doc();
            });
        });
        lua().setField(-2, "doc");

        lua().setGlobal("userdata");
    }
}
