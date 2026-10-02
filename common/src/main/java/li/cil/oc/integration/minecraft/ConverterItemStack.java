package li.cil.oc.integration.minecraft;

import li.cil.oc.Settings;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ConverterItemStack implements Converter {
    public static final ConverterItemStack INSTANCE = new ConverterItemStack();

    private ConverterItemStack() {
    }

    public static Object getTagValue(CompoundTag tag, String key) {
        switch (tag.getTagType(key)) {
            case Tag.TAG_INT:
                return tag.getInt(key);
            case Tag.TAG_STRING:
                return tag.getString(key);
            case Tag.TAG_BYTE:
                return tag.getByte(key);
            case Tag.TAG_COMPOUND:
                return tag.getCompound(key);
            case Tag.TAG_LIST:
                return tag.getList(key, Tag.TAG_STRING);
            default:
                return null;
        }
    }

    public static Object withTag(CompoundTag tag, String key, int tagId, Function<Object, Object> f) {
        if (tag.contains(key, tagId)) {
            final Object value = getTagValue(tag, key);
            return value != null ? f.apply(value) : null;
        }
        return null;
    }

    public static Object withCompound(CompoundTag tag, String key, Function<CompoundTag, Object> f) {
        return withTag(tag, key, Tag.TAG_COMPOUND, value -> value instanceof CompoundTag compound ? f.apply(compound) : null);
    }

    public static Object withList(CompoundTag tag, String key, Function<ListTag, Object> f) {
        // Note: 1.16.5 checked for TAG_STRING here, which never matched list tags (so lore was never reported).
        return withTag(tag, key, Tag.TAG_LIST, value -> value instanceof ListTag list ? f.apply(list) : null);
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack stack) {
            if (Settings.get().insertIdsInConverters) {
                output.put("id", Item.getId(stack.getItem()));
                output.put("oreNames", stack.getTags().map(tag -> tag.location().toString()).toArray(String[]::new));
            }
            output.put("damage", stack.getDamageValue());
            output.put("maxDamage", stack.getMaxDamage());
            output.put("size", stack.getCount());
            output.put("maxSize", stack.getMaxStackSize());
            output.put("hasTag", stack.hasTag());
            output.put("name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            output.put("label", stack.getHoverName().getString());

            // custom mod tags
            if (stack.hasTag()) {
                final CompoundTag tags = stack.getTag();

                //Lore tags
                withCompound(tags, "display", display -> withList(display, "Lore", lore -> {
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < lore.size(); i++) {
                        if (i > 0) sb.append("\n");
                        sb.append(lore.getString(i));
                    }
                    return output.put("lore", sb.toString());
                }));

                withTag(tags, "Energy", Tag.TAG_INT, energy -> output.put("Energy", energy));

                if (Settings.get().allowItemStackNBTTags) {
                    output.put("tag", ItemUtils.saveTag(stack.getTag()));
                }
            }

            final List<Map<String, Object>> enchantments = new ArrayList<>();
            for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
                final Map<String, Object> map = new HashMap<>();
                map.put("name", String.valueOf(BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey())));
                map.put("label", entry.getKey().getFullname(entry.getValue()).getString());
                map.put("level", entry.getValue());
                enchantments.add(map);
            }
            if (!enchantments.isEmpty()) {
                output.put("enchantments", enchantments);
            }
        }
    }
}
