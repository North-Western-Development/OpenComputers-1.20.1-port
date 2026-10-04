package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Value;
import li.cil.repack.org.luaj.vm2.LuaString;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * LuaJ function wrapping a Java function. The former implicit conversions are
 * {@link #wrapClosure(Function)} (function returning a single {@link LuaValue})
 * and {@link #wrapVarArgClosure(Function)} (function returning {@link Varargs}).
 */
public class ScalaClosure extends VarArgFunction {
    public final Function<Varargs, Varargs> f;

    public ScalaClosure(Function<Varargs, Varargs> f) {
        this.f = f;
    }

    @Override
    public Varargs invoke(Varargs args) {
        return f.apply(args);
    }

    public static ScalaClosure wrapClosure(Function<Varargs, LuaValue> f) {
        return new ScalaClosure(args -> {
            final LuaValue result = f.apply(args);
            if (result == LuaValue.NONE) return LuaValue.NONE;
            // A LuaValue is a Varargs already (of itself).
            return result;
        });
    }

    public static ScalaClosure wrapVarArgClosure(Function<Varargs, Varargs> f) {
        return new ScalaClosure(f);
    }

    public static LuaValue toLuaValue(Object value) {
        if (value == null || value == ResultWrapper.unit) return LuaValue.NIL;
        else if (value instanceof Boolean v) return LuaValue.valueOf(v);
        else if (value instanceof Byte v) return LuaValue.valueOf(v.byteValue());
        else if (value instanceof Character v) return LuaValue.valueOf(String.valueOf(v));
        else if (value instanceof Short v) return LuaValue.valueOf(v.shortValue());
        else if (value instanceof Integer v) return LuaValue.valueOf(v.intValue());
        else if (value instanceof Long v) return LuaValue.valueOf(v.longValue());
        else if (value instanceof Float v) return LuaValue.valueOf(v.floatValue());
        else if (value instanceof Double v) return LuaValue.valueOf(v.doubleValue());
        else if (value instanceof String v) return LuaValue.valueOf(v);
        else if (value instanceof byte[] v) return LuaValue.valueOf(v);
        else if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            final List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) list.add(Array.get(value, i));
            return toLuaList(list);
        } else if (value instanceof Value v && Settings.get().allowUserdata) return LuaValue.userdataOf(v);
            // Scala tuples (Products) are now commons-lang Pair / Triple.
        else if (value instanceof Pair<?, ?> v) return toLuaList(Arrays.asList(v.getLeft(), v.getRight()));
        else if (value instanceof Triple<?, ?, ?> v) return toLuaList(Arrays.asList(v.getLeft(), v.getMiddle(), v.getRight()));
            // Scala's Option (a Product) used to become a list; Optional is unwrapped instead.
        else if (value instanceof Optional<?> v) return toLuaValue(v.orElse(null));
        else if (value instanceof Iterable<?> v) return toLuaList(v);
        else if (value instanceof Map<?, ?> v) return toLuaTable(v);
        else {
            OpenComputers.log.warn("Tried to push an unsupported value of type to Lua: " + value.getClass().getName() + ".");
            return LuaValue.NIL;
        }
    }

    public static LuaValue toLuaList(Iterable<?> value) {
        final List<LuaValue> values = new ArrayList<>();
        for (Object v : value) values.add(toLuaValue(v));
        return LuaValue.listOf(values.toArray(new LuaValue[0]));
    }

    public static LuaValue toLuaTable(Map<?, ?> value) {
        final List<LuaValue> values = new ArrayList<>();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            values.add(toLuaValue(entry.getKey()));
            values.add(toLuaValue(entry.getValue()));
        }
        return LuaValue.tableOf(values.toArray(new LuaValue[0]));
    }

    public static Object toSimpleJavaObject(LuaValue value) {
        switch (value.type()) {
            case LuaValue.TBOOLEAN:
                return value.toboolean();
            case LuaValue.TNUMBER:
                return value.todouble();
            case LuaValue.TSTRING:
                if (value instanceof LuaString s) {
                    return Arrays.copyOfRange(s.m_bytes, s.m_offset, s.m_offset + s.m_length);
                }
                return value.tojstring(); // Waddafaq?
            case LuaValue.TTABLE: {
                final LuaTable table = value.checktable();
                final Map<Object, Object> result = new HashMap<>();
                for (LuaValue key : table.keys()) {
                    result.put(toSimpleJavaObject(key), toSimpleJavaObject(table.get(key)));
                }
                return result;
            }
            case LuaValue.TUSERDATA:
                return value.touserdata();
            default:
                return null;
        }
    }

    public static List<Object> toSimpleJavaObjects(Varargs args, int start) {
        final List<Object> result = new ArrayList<>();
        for (int index = start; index <= args.narg(); index++) {
            result.add(toSimpleJavaObject(args.arg(index)));
        }
        return result;
    }

    public static List<Object> toSimpleJavaObjects(Varargs args) {
        return toSimpleJavaObjects(args, 1);
    }
}
