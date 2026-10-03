package li.cil.oc.server.machine;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArgumentsImpl implements Arguments {
    public final List<Object> args;

    public ArgumentsImpl(List<Object> args) {
        this.args = args;
    }

    public ArgumentsImpl(Object[] args) {
        this(Arrays.asList(args));
    }

    @Override
    public Iterator<Object> iterator() {
        return args.iterator();
    }

    @Override
    public int count() {
        return args.size();
    }

    @Override
    public Object checkAny(int index) {
        checkIndex(index, "value");
        final Object arg = args.get(index);
        if (arg == ResultWrapper.unit || (arg instanceof Optional<?> o && o.isEmpty())) return null;
        return arg;
    }

    @Override
    public Object optAny(int index, Object def) {
        if (!isDefined(index)) return def;
        return checkAny(index);
    }

    @Override
    public boolean checkBoolean(int index) {
        checkIndex(index, "boolean");
        final Object value = args.get(index);
        if (value instanceof Boolean b) return b;
        throw typeError(index, value, "boolean");
    }

    @Override
    public boolean optBoolean(int index, boolean def) {
        if (!isDefined(index)) return def;
        return checkBoolean(index);
    }

    @Override
    public double checkDouble(int index) {
        checkIndex(index, "number");
        final Object value = args.get(index);
        if (value instanceof Number n) return n.doubleValue();
        throw typeError(index, value, "number");
    }

    @Override
    public double optDouble(int index, double def) {
        if (!isDefined(index)) return def;
        return checkDouble(index);
    }

    // Somewhat mimics Lua 5.3+ number conversion (KosmosPrime fork): NaN has no integer representation,
    // out of range values saturate (instead of wrapping for longs). Rejecting non-integral or
    // infinite values would be more correct, but breaks existing code (like file:read(math.huge)).
    @Override
    public int checkInteger(int index) {
        checkIndex(index, "integer");
        final Object value = args.get(index);
        if (value instanceof Double || value instanceof Float) {
            final double d = ((Number) value).doubleValue();
            if (Double.isNaN(d)) throw intError(index, value);
            return (int) d; // Saturating.
        }
        if (value instanceof Long l) return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, l));
        if (value instanceof Number n) return n.intValue();
        throw typeError(index, value, "integer");
    }

    @Override
    public long checkLong(int index) {
        checkIndex(index, "integer");
        final Object value = args.get(index);
        if (value instanceof Double || value instanceof Float) {
            final double d = ((Number) value).doubleValue();
            if (Double.isNaN(d)) throw intError(index, value);
            return (long) d; // Saturating.
        }
        if (value instanceof Number n) return n.longValue();
        throw typeError(index, value, "integer");
    }

    @Override
    public long optLong(int index, long def) {
        if (!isDefined(index)) return def;
        return checkLong(index);
    }

    @Override
    public int optInteger(int index, int def) {
        if (!isDefined(index)) return def;
        return checkInteger(index);
    }

    @Override
    public String checkString(int index) {
        checkIndex(index, "string");
        final Object value = args.get(index);
        if (value instanceof String s) return s;
        if (value instanceof byte[] b) return new String(b, StandardCharsets.UTF_8);
        throw typeError(index, value, "string");
    }

    @Override
    public String optString(int index, String def) {
        if (!isDefined(index)) return def;
        return checkString(index);
    }

    @Override
    public byte[] checkByteArray(int index) {
        checkIndex(index, "string");
        final Object value = args.get(index);
        if (value instanceof String s) return s.getBytes(StandardCharsets.UTF_8);
        if (value instanceof byte[] b) return b;
        throw typeError(index, value, "string");
    }

    @Override
    public byte[] optByteArray(int index, byte[] def) {
        if (!isDefined(index)) return def;
        return checkByteArray(index);
    }

    @Override
    public Map checkTable(int index) {
        checkIndex(index, "table");
        final Object value = args.get(index);
        if (value instanceof Map<?, ?> m) return m;
        throw typeError(index, value, "table");
    }

    @Override
    public Map optTable(int index, Map def) {
        if (!isDefined(index)) return def;
        return checkTable(index);
    }

    @Override
    public ItemStack checkItemStack(int index) {
        final Map<?, ?> map = checkTable(index);
        if (map.get("name") instanceof String name) {
            final int damage = map.get("damage") instanceof Number number ? number.intValue() : 0;
            final Object rawTag = map.get("tag");
            final Optional<CompoundTag> tag;
            if (rawTag instanceof byte[] ba) tag = toNbtTagCompound(ba);
            else if (rawTag instanceof String s) tag = toNbtTagCompound(s.getBytes(StandardCharsets.UTF_8));
            else tag = Optional.empty();
            return makeStack(name, damage, tag);
        }
        throw new IllegalArgumentException("invalid item stack");
    }

    @Override
    public ItemStack optItemStack(int index, ItemStack def) {
        if (!isDefined(index)) return def;
        return checkItemStack(index);
    }

    @Override
    public boolean isBoolean(int index) {
        return index >= 0 && index < count() && args.get(index) instanceof Boolean;
    }

    @Override
    public boolean isDouble(int index) {
        if (index < 0 || index >= count()) return false;
        return args.get(index) instanceof Number;
    }

    @Override
    public boolean isInteger(int index) {
        if (index < 0 || index >= count()) return false;
        final Object value = args.get(index);
        if (value instanceof Double || value instanceof Float) return !Double.isNaN(((Number) value).doubleValue());
        return value instanceof Number;
    }

    @Override
    public boolean isLong(int index) {
        return isInteger(index);
    }

    @Override
    public boolean isString(int index) {
        if (index < 0 || index >= count()) return false;
        final Object value = args.get(index);
        return value instanceof String || value instanceof byte[];
    }

    @Override
    public boolean isByteArray(int index) {
        return isString(index);
    }

    @Override
    public boolean isTable(int index) {
        return index >= 0 && index < count() && args.get(index) instanceof Map<?, ?>;
    }

    @Override
    public boolean isItemStack(int index) {
        if (!isTable(index)) return false;
        final Object name = checkTable(index).get("name");
        return name instanceof String || name instanceof byte[];
    }

    @Override
    public Object[] toArray() {
        final Object[] result = new Object[args.size()];
        for (int i = 0; i < result.length; i++) {
            final Object value = args.get(i);
            result[i] = value instanceof byte[] b ? new String(b, StandardCharsets.UTF_8) : value;
        }
        return result;
    }

    private boolean isDefined(int index) {
        return index >= 0 && index < args.size() && args.get(index) != null;
    }

    private void checkIndex(int index, String name) {
        if (index < 0) throw new IndexOutOfBoundsException();
        else if (args.size() <= index) throw new IllegalArgumentException(
                "bad arguments #" + (index + 1) + " (" + name + " expected, got no value)");
    }

    private IllegalArgumentException typeError(int index, Object have, String want) {
        return new IllegalArgumentException(
                "bad argument #" + (index + 1) + " (" + want + " expected, got " + typeName(have) + ")");
    }

    private IllegalArgumentException intError(int index, Object have) {
        return new IllegalArgumentException(
                "bad argument #" + (index + 1) + " (" + typeName(have) + " has no integer representation)");
    }

    private static String typeName(Object value) {
        if (value == null || value == ResultWrapper.unit || (value instanceof Optional<?> o && o.isEmpty())) return "nil";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return "integer";
        if (value instanceof Number) return "number";
        if (value instanceof String) return "string";
        if (value instanceof byte[]) return "string";
        if (value instanceof Map<?, ?>) return "table";
        return value.getClass().getSimpleName();
    }

    private static ItemStack makeStack(String name, int damage, Optional<CompoundTag> tag) {
        final ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null) throw new IllegalArgumentException("invalid item stack");
        final Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty() || item.get() == Items.AIR) throw new IllegalArgumentException("invalid item stack");
        final ItemStack stack = new ItemStack(item.get(), 1);
        stack.setDamageValue(damage);
        tag.ifPresent(stack::setTag);
        return stack;
    }

    private static Optional<CompoundTag> toNbtTagCompound(byte[] data) {
        return Optional.ofNullable(ItemUtils.loadTag(data));
    }
}
