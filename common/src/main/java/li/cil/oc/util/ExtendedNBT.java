package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * NBT helpers. The former implicit conversions ({@code toNbt}) are explicit
 * static overloads now, and the former extension classes for {@code INBT},
 * {@code CompoundNBT} and {@code ListNBT} are static helpers taking the tag
 * as first argument, e.g. {@code ExtendedNBT.setNewCompoundTag(nbt, "name", t -> ...)}.
 */
public final class ExtendedNBT {
    private ExtendedNBT() {
    }

    public static ByteTag toNbt(boolean value) {
        return ByteTag.valueOf(value);
    }

    public static ByteTag toNbt(byte value) {
        return ByteTag.valueOf(value);
    }

    public static ShortTag toNbt(short value) {
        return ShortTag.valueOf(value);
    }

    public static IntTag toNbt(int value) {
        return IntTag.valueOf(value);
    }

    public static LongTag toNbt(long value) {
        return LongTag.valueOf(value);
    }

    public static FloatTag toNbt(float value) {
        return FloatTag.valueOf(value);
    }

    public static DoubleTag toNbt(double value) {
        return DoubleTag.valueOf(value);
    }

    public static ByteArrayTag toNbt(byte[] value) {
        return new ByteArrayTag(value);
    }

    public static IntArrayTag toNbt(int[] value) {
        return new IntArrayTag(value);
    }

    public static ByteArrayTag toNbt(boolean[] value) {
        final byte[] bytes = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            bytes[i] = value[i] ? (byte) 1 : (byte) 0;
        }
        return new ByteArrayTag(bytes);
    }

    public static StringTag toNbt(String value) {
        return StringTag.valueOf(value);
    }

    public static CompoundTag toNbt(ItemStack value) {
        final CompoundTag nbt = new CompoundTag();
        if (value != null) {
            value.save(nbt);
        }
        return nbt;
    }

    public static CompoundTag toNbt(Consumer<CompoundTag> value) {
        final CompoundTag nbt = new CompoundTag();
        value.accept(nbt);
        return nbt;
    }

    public static CompoundTag toNbt(Map<String, ?> value) {
        final CompoundTag nbt = new CompoundTag();
        for (Map.Entry<String, ?> entry : value.entrySet()) {
            final String key = entry.getKey();
            final Object v = entry.getValue();
            if (v instanceof Boolean b) nbt.put(key, toNbt((boolean) b));
            else if (v instanceof Byte b) nbt.put(key, toNbt((byte) b));
            else if (v instanceof Short s) nbt.put(key, toNbt((short) s));
            else if (v instanceof Integer i) nbt.put(key, toNbt((int) i));
            else if (v instanceof Long l) nbt.put(key, toNbt((long) l));
            else if (v instanceof Float f) nbt.put(key, toNbt((float) f));
            else if (v instanceof Double d) nbt.put(key, toNbt((double) d));
            else if (v instanceof byte[] a) nbt.put(key, toNbt(a));
            else if (v instanceof int[] a) nbt.put(key, toNbt(a));
            else if (v instanceof String s) nbt.put(key, toNbt(s));
            else if (v instanceof ItemStack s) nbt.put(key, toNbt(s));
        }
        return nbt;
    }

    // ----------------------------------------------------------------------- //

    private static Object[] mapToList(Map<?, ?> value) {
        final List<Map.Entry<Number, Object>> entries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            // Ignore, can be stuff like the 'n' introduced by Lua's `pack`.
            if (entry.getKey() instanceof Number k) {
                entries.add(Map.entry(k, entry.getValue() == null ? NULL : entry.getValue()));
            }
        }
        entries.sort(Comparator.comparingInt(e -> e.getKey().intValue()));
        return entries.stream().map(e -> e.getValue() == NULL ? null : e.getValue()).toArray();
    }

    private static final Object NULL = new Object();

    private static Object[] asList(Object value) {
        if (value instanceof Object[] v) return v;
        if (value != null && value.getClass().isArray()) {
            final int length = java.lang.reflect.Array.getLength(value);
            final Object[] result = new Object[length];
            for (int i = 0; i < length; i++) result[i] = java.lang.reflect.Array.get(value, i);
            return result;
        }
        if (value instanceof Map<?, ?> v) return mapToList(v);
        if (value instanceof List<?> v) return v.toArray();
        if (value instanceof String v) return asList(v.getBytes(StandardCharsets.UTF_8));
        throw new IllegalArgumentException("Illegal or missing value.");
    }

    @SuppressWarnings("unchecked")
    private static <K> Map<K, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> v) return (Map<K, ?>) v;
        throw new IllegalArgumentException("Illegal value.");
    }

    public static Tag typedMapToNbt(Map<?, ?> map) {
        final Map<String, ?> typeAndValue = asMap(map);
        final Object nbtType = typeAndValue.get("type");
        final Object nbtValue = typeAndValue.get("value");
        if (nbtType instanceof Number n) {
            switch (n.intValue()) {
                case Tag.TAG_BYTE:
                    if (nbtValue instanceof Number v) return ByteTag.valueOf(v.byteValue());
                    throw new IllegalArgumentException("Illegal or missing value.");

                case Tag.TAG_SHORT:
                    if (nbtValue instanceof Number v) return ShortTag.valueOf(v.shortValue());
                    throw new IllegalArgumentException("Illegal or missing value.");

                case Tag.TAG_INT:
                    if (nbtValue instanceof Number v) return IntTag.valueOf(v.intValue());
                    throw new IllegalArgumentException("Illegal or missing value.");

                case Tag.TAG_LONG:
                    if (nbtValue instanceof Number v) return LongTag.valueOf(v.longValue());
                    throw new IllegalArgumentException("Illegal or missing value.");

                case Tag.TAG_FLOAT:
                    if (nbtValue instanceof Number v) return FloatTag.valueOf(v.floatValue());
                    throw new IllegalArgumentException("Illegal or missing value.");

                case Tag.TAG_DOUBLE:
                    if (nbtValue instanceof Number v) return DoubleTag.valueOf(v.doubleValue());
                    throw new IllegalArgumentException("Illegal or missing value.");

                case Tag.TAG_BYTE_ARRAY: {
                    final Object[] list = asList(nbtValue);
                    final byte[] bytes = new byte[list.length];
                    for (int i = 0; i < list.length; i++) {
                        if (list[i] instanceof Number v) bytes[i] = v.byteValue();
                        else throw new IllegalArgumentException("Illegal value.");
                    }
                    return new ByteArrayTag(bytes);
                }

                case Tag.TAG_STRING:
                    if (nbtValue instanceof String v) return StringTag.valueOf(v);
                    if (nbtValue instanceof byte[] v) return StringTag.valueOf(new String(v, StandardCharsets.UTF_8));
                    throw new IllegalArgumentException("Illegal or missing value.");

                case Tag.TAG_LIST: {
                    final ListTag list = new ListTag();
                    for (Object v : asList(nbtValue)) {
                        list.add(typedMapToNbt(asMap(v)));
                    }
                    return list;
                }

                case Tag.TAG_COMPOUND: {
                    final CompoundTag nbt = new CompoundTag();
                    final Map<String, ?> values = asMap(nbtValue);
                    for (Map.Entry<String, ?> entry : values.entrySet()) {
                        final String name = entry.getKey();
                        try {
                            nbt.put(name, typedMapToNbt(asMap(entry.getValue())));
                        } catch (Throwable t) {
                            throw new IllegalArgumentException("Error converting entry '" + name + "': " + t.getMessage());
                        }
                    }
                    return nbt;
                }

                case Tag.TAG_INT_ARRAY: {
                    final Object[] list = asList(nbtValue);
                    final int[] ints = new int[list.length];
                    for (int i = 0; i < list.length; i++) {
                        if (list[i] instanceof Number v) ints[i] = v.intValue();
                        else throw new IllegalArgumentException();
                    }
                    return new IntArrayTag(ints);
                }

                default:
                    throw new IllegalArgumentException("Unsupported NBT type '" + n + "'.");
            }
        } else if (nbtType != null) {
            throw new IllegalArgumentException("Illegal NBT type '" + nbtType + "'.");
        } else {
            throw new IllegalArgumentException("Missing NBT type.");
        }
    }

    // ----------------------------------------------------------------------- //
    // Former implicit Iterable[X] => Iterable[XNBT] conversions.

    public static List<ByteTag> booleanIterableToNbt(Iterable<Boolean> value) {
        final List<ByteTag> result = new ArrayList<>();
        for (Boolean v : value) result.add(toNbt((boolean) v));
        return result;
    }

    public static List<ByteTag> byteIterableToNbt(Iterable<Byte> value) {
        final List<ByteTag> result = new ArrayList<>();
        for (Byte v : value) result.add(toNbt((byte) v));
        return result;
    }

    public static List<ShortTag> shortIterableToNbt(Iterable<Short> value) {
        final List<ShortTag> result = new ArrayList<>();
        for (Short v : value) result.add(toNbt((short) v));
        return result;
    }

    public static List<IntTag> intIterableToNbt(Iterable<Integer> value) {
        final List<IntTag> result = new ArrayList<>();
        for (Integer v : value) result.add(toNbt((int) v));
        return result;
    }

    public static List<IntArrayTag> intArrayIterableToNbt(Iterable<int[]> value) {
        final List<IntArrayTag> result = new ArrayList<>();
        for (int[] v : value) result.add(toNbt(v));
        return result;
    }

    public static List<LongTag> longIterableToNbt(Iterable<Long> value) {
        final List<LongTag> result = new ArrayList<>();
        for (Long v : value) result.add(toNbt((long) v));
        return result;
    }

    public static List<FloatTag> floatIterableToNbt(Iterable<Float> value) {
        final List<FloatTag> result = new ArrayList<>();
        for (Float v : value) result.add(toNbt((float) v));
        return result;
    }

    public static List<DoubleTag> doubleIterableToNbt(Iterable<Double> value) {
        final List<DoubleTag> result = new ArrayList<>();
        for (Double v : value) result.add(toNbt((double) v));
        return result;
    }

    public static List<ByteArrayTag> byteArrayIterableToNbt(Iterable<byte[]> value) {
        final List<ByteArrayTag> result = new ArrayList<>();
        for (byte[] v : value) result.add(toNbt(v));
        return result;
    }

    public static List<StringTag> stringIterableToNbt(Iterable<String> value) {
        final List<StringTag> result = new ArrayList<>();
        for (String v : value) result.add(toNbt(v));
        return result;
    }

    public static List<CompoundTag> writableIterableToNbt(Iterable<Consumer<CompoundTag>> value) {
        final List<CompoundTag> result = new ArrayList<>();
        for (Consumer<CompoundTag> v : value) result.add(toNbt(v));
        return result;
    }

    public static List<CompoundTag> itemStackIterableToNbt(Iterable<ItemStack> value) {
        final List<CompoundTag> result = new ArrayList<>();
        for (ItemStack v : value) result.add(toNbt(v));
        return result;
    }

    // ----------------------------------------------------------------------- //
    // ExtendedINBT

    public static Map<String, Object> toTypedMap(Tag nbt) {
        final Object value;
        if (nbt instanceof ByteTag tag) value = tag.getAsByte();
        else if (nbt instanceof ShortTag tag) value = tag.getAsShort();
        else if (nbt instanceof IntTag tag) value = tag.getAsInt();
        else if (nbt instanceof LongTag tag) value = tag.getAsLong();
        else if (nbt instanceof FloatTag tag) value = tag.getAsFloat();
        else if (nbt instanceof DoubleTag tag) value = tag.getAsDouble();
        else if (nbt instanceof ByteArrayTag tag) value = tag.getAsByteArray();
        else if (nbt instanceof StringTag tag) value = tag.getAsString();
        else if (nbt instanceof ListTag tag) value = map(tag, ExtendedNBT::toTypedMap);
        else if (nbt instanceof CompoundTag tag) {
            final Map<String, Object> entries = new HashMap<>();
            for (String key : tag.getAllKeys()) {
                entries.put(key, toTypedMap(tag.get(key)));
            }
            value = entries;
        } else if (nbt instanceof IntArrayTag tag) value = tag.getAsIntArray();
        else throw new IllegalArgumentException();
        final Map<String, Object> result = new HashMap<>();
        result.put("type", nbt.getId());
        result.put("value", value);
        return result;
    }

    // ----------------------------------------------------------------------- //
    // ExtendedCompoundNBT

    public static CompoundTag setNewCompoundTag(CompoundTag nbt, String name, Consumer<CompoundTag> f) {
        final CompoundTag t = new CompoundTag();
        f.accept(t);
        nbt.put(name, t);
        return nbt;
    }

    public static CompoundTag setNewTagList(CompoundTag nbt, String name, Iterable<? extends Tag> values) {
        final ListTag t = new ListTag();
        append(t, values);
        nbt.put(name, t);
        return nbt;
    }

    public static CompoundTag setNewTagList(CompoundTag nbt, String name, Tag... values) {
        return setNewTagList(nbt, name, java.util.Arrays.asList(values));
    }

    public static Optional<Direction> getDirection(CompoundTag nbt, String name) {
        final byte id = nbt.getByte(name);
        if (id < 0 || id > Direction.values().length) return Optional.empty();
        return Optional.ofNullable(Direction.from3DDataValue(id));
    }

    public static void setDirection(CompoundTag nbt, String name, Optional<Direction> d) {
        if (d.isPresent()) nbt.putByte(name, (byte) d.get().ordinal());
        else nbt.putByte(name, (byte) -1);
    }

    public static boolean[] getBooleanArray(CompoundTag nbt, String name) {
        final byte[] bytes = nbt.getByteArray(name);
        final boolean[] result = new boolean[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i] == 1;
        }
        return result;
    }

    public static void setBooleanArray(CompoundTag nbt, String name, boolean[] value) {
        nbt.put(name, toNbt(value));
    }

    // ----------------------------------------------------------------------- //
    // ExtendedListNBT

    public static void appendNewCompoundTag(ListTag nbt, Consumer<CompoundTag> f) {
        final CompoundTag t = new CompoundTag();
        f.accept(t);
        nbt.add(t);
    }

    public static void append(ListTag nbt, Iterable<? extends Tag> values) {
        for (Tag value : values) {
            nbt.add(value);
        }
    }

    public static void append(ListTag nbt, Tag... values) {
        append(nbt, java.util.Arrays.asList(values));
    }

    /**
     * Iterates over a copy of the list, casting each entry to the requested tag type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Tag> void foreach(ListTag nbt, Consumer<T> f) {
        final ListTag iterable = nbt.copy();
        for (Tag tag : iterable) {
            f.accept((T) tag);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Tag, V> List<V> map(ListTag nbt, Function<T, V> f) {
        final ListTag iterable = nbt.copy();
        final List<V> buffer = new ArrayList<>(iterable.size());
        for (Tag tag : iterable) {
            buffer.add(f.apply((T) tag));
        }
        return buffer;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Tag> T[] toTagArray(ListTag nbt, Class<T> clazz) {
        final List<T> list = map(nbt, (T t) -> t);
        return list.toArray((T[]) java.lang.reflect.Array.newInstance(clazz, list.size()));
    }
}
