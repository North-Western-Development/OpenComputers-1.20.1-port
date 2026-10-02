package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.Converter;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public final class ConverterNBT implements Converter {
    public static final ConverterNBT INSTANCE = new ConverterNBT();

    private ConverterNBT() {
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof CompoundTag nbt) {
            output.put("oc:flatten", convert(nbt));
        }
    }

    private static Object convert(Tag nbt) {
        if (nbt instanceof ByteTag tag) return tag.getAsByte();
        if (nbt instanceof ShortTag tag) return tag.getAsShort();
        if (nbt instanceof IntTag tag) return tag.getAsInt();
        if (nbt instanceof LongTag tag) return tag.getAsLong();
        if (nbt instanceof FloatTag tag) return tag.getAsFloat();
        if (nbt instanceof DoubleTag tag) return tag.getAsDouble();
        if (nbt instanceof ByteArrayTag tag) return tag.getAsByteArray();
        if (nbt instanceof StringTag tag) return tag.getAsString();
        if (nbt instanceof ListTag tag) {
            final Object[] result = new Object[tag.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = convert(tag.get(i));
            }
            return result;
        }
        if (nbt instanceof CompoundTag tag) {
            final Map<String, Object> result = new HashMap<>();
            for (String key : tag.getAllKeys()) {
                result.put(key, convert(tag.get(key)));
            }
            return result;
        }
        if (nbt instanceof IntArrayTag tag) return tag.getAsIntArray();
        if (nbt instanceof LongArrayTag tag) return tag.getAsLongArray();
        // TODO(port): 1.16.5 threw a MatchError for unknown tag types.
        return null;
    }
}
