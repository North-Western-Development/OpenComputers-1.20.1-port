package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Value;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Former implicit extension class for JNLua's {@link LuaState}; call as
 * {@code ExtendedLuaState.method(lua, args...)}.
 */
public final class ExtendedLuaState {
    private ExtendedLuaState() {
    }

    public static void pushScalaFunction(LuaState lua, Function<LuaState, Integer> f) {
        lua.pushJavaFunction(new JavaFunction() {
            @Override
            public int invoke(LuaState state) {
                return f.apply(state);
            }
        });
    }

    public static void pushValue(LuaState lua, Object value) {
        pushValue(lua, value, new IdentityHashMap<>());
    }

    public static void pushValue(LuaState lua, Object value, IdentityHashMap<Object, Integer> memo) {
        final boolean recursive = memo.size() > 0;
        final int oldTop = lua.getTop();
        if (value != null && memo.containsKey(value)) {
            lua.pushValue(memo.get(value));
        } else {
            if (value == null || value == ResultWrapper.unit) lua.pushNil();
            else if (value instanceof Boolean v) lua.pushBoolean(v);
            else if (value instanceof Byte v) lua.pushNumber(v.byteValue());
            else if (value instanceof Character v) lua.pushString(String.valueOf(v));
            else if (value instanceof Short v) lua.pushNumber(v.shortValue());
            else if (value instanceof Integer v) lua.pushNumber(v.intValue());
            else if (value instanceof Long v) lua.pushNumber(v.longValue());
            else if (value instanceof Float v) lua.pushNumber(v.floatValue());
            else if (value instanceof Double v) lua.pushNumber(v.doubleValue());
            else if (value instanceof String v) lua.pushString(v);
            else if (value instanceof byte[] v) lua.pushByteArray(v);
            else if (value.getClass().isArray()) pushList(lua, value, arrayIterator(value), memo);
            else if (value instanceof Value v && Settings.get().allowUserdata) lua.pushJavaObjectRaw(v);
                // Scala tuples (Products) are now commons-lang Pair / Triple.
            else if (value instanceof Pair<?, ?> v) pushList(lua, value, Arrays.asList(v.getLeft(), v.getRight()).iterator(), memo);
            else if (value instanceof Triple<?, ?, ?> v) pushList(lua, value, Arrays.asList(v.getLeft(), v.getMiddle(), v.getRight()).iterator(), memo);
                // Scala's Option (a Product) used to be pushed as a list; Optional is unwrapped instead.
            else if (value instanceof Optional<?> v) pushValue(lua, v.orElse(null), memo);
            else if (value instanceof Iterable<?> v) pushList(lua, value, v.iterator(), memo);
            else if (value instanceof Map<?, ?> v) pushTable(lua, value, v, memo);
            else {
                OpenComputers.log.warn("Tried to push an unsupported value of type to Lua: " + value.getClass().getName() + ".");
                lua.pushNil();
            }
            // Remove values kept on the stack for memoization if this is the
            // original call (not a recursive one, where we might need the memo
            // info even after returning).
            if (!recursive) {
                lua.setTop(oldTop + 1);
            }
        }
    }

    private static Iterator<Object> arrayIterator(Object array) {
        final int length = Array.getLength(array);
        final List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(array, i));
        }
        return list.iterator();
    }

    public static void pushList(LuaState lua, Object obj, Iterator<?> list, IdentityHashMap<Object, Integer> memo) {
        lua.newTable();
        final int tableIndex = lua.getTop();
        memo.put(obj, tableIndex);
        int count = 0;
        while (list.hasNext()) {
            final Object value = list.next();
            pushValue(lua, value, memo);
            lua.rawSet(tableIndex, count + 1);
            count = count + 1;
        }
        // Bring table back to top (in case memo values were pushed).
        lua.pushValue(tableIndex);
        lua.pushString("n");
        lua.pushInteger(count);
        lua.rawSet(-3);
    }

    public static void pushTable(LuaState lua, Object obj, Map<?, ?> map, IdentityHashMap<Object, Integer> memo) {
        lua.newTable(0, map.size());
        final int tableIndex = lua.getTop();
        memo.put(obj, tableIndex);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            final Object key = entry.getKey();
            if (key != null && key != ResultWrapper.unit) {
                pushValue(lua, key, memo);
                final int keyIndex = lua.getTop();
                pushValue(lua, entry.getValue(), memo);
                // Bring key to front, in case of memo from value push.
                // Cannot actually move because that might shift memo info.
                lua.pushValue(keyIndex);
                lua.insert(-2);
                lua.setTable(tableIndex);
            }
        }
        // Bring table back to top (in case memo values were pushed).
        lua.pushValue(tableIndex);
    }

    public static Object toSimpleJavaObject(LuaState lua, int index) {
        final LuaType type = lua.type(index);
        if (type == null) return null;
        switch (type) {
            case BOOLEAN:
                return lua.toBoolean(index);
            case NUMBER:
                return lua.toNumber(index);
            case STRING:
                return lua.toByteArray(index);
            case TABLE:
                return lua.toJavaObject(index, Map.class);
            case USERDATA:
                return lua.toJavaObjectRaw(index);
            default:
                return null;
        }
    }

    public static List<Object> toSimpleJavaObjects(LuaState lua, int start) {
        final List<Object> result = new ArrayList<>();
        for (int index = start; index <= lua.getTop(); index++) {
            result.add(toSimpleJavaObject(lua, index));
        }
        return result;
    }
}
