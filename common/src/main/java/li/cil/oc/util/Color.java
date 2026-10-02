package li.cil.oc.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class Color {
    private Color() {
    }

    public static final Map<DyeColor, Integer> rgbValues;

    static {
        final Map<DyeColor, Integer> map = new EnumMap<>(DyeColor.class);
        map.put(DyeColor.BLACK, 0x444444); // 0x1E1B1B
        map.put(DyeColor.RED, 0xB3312C);
        map.put(DyeColor.GREEN, 0x339911); // 0x3B511A
        map.put(DyeColor.BROWN, 0x51301A);
        map.put(DyeColor.BLUE, 0x6666FF); // 0x253192
        map.put(DyeColor.PURPLE, 0x7B2FBE);
        map.put(DyeColor.CYAN, 0x66FFFF); // 0x287697
        map.put(DyeColor.LIGHT_GRAY, 0xABABAB);
        map.put(DyeColor.GRAY, 0x666666); // 0x434343
        map.put(DyeColor.PINK, 0xD88198);
        map.put(DyeColor.LIME, 0x66FF66); // 0x41CD34
        map.put(DyeColor.YELLOW, 0xFFFF66); // 0xDECF2A
        map.put(DyeColor.LIGHT_BLUE, 0xAAAAFF); // 0x6689D3
        map.put(DyeColor.MAGENTA, 0xC354CD);
        map.put(DyeColor.ORANGE, 0xEB8844);
        map.put(DyeColor.WHITE, 0xF0F0F0);
        rgbValues = Collections.unmodifiableMap(map);
    }

    public static final Map<String, DyeColor> byName;

    /**
     * Dye item tags. On 1.16.5 this used Forge's {@code DyeColor.getTag}
     * ({@code forge:dyes/<color>}); since that is not available in vanilla we
     * check both the Forge ({@code forge:dyes/<color>}) and the Fabric
     * conventional ({@code c:<color>_dyes}) tags.
     */
    public static final Map<TagKey<Item>, DyeColor> byTag;

    static {
        final Map<String, DyeColor> names = new HashMap<>();
        final Map<TagKey<Item>, DyeColor> tags = new LinkedHashMap<>();
        for (DyeColor color : DyeColor.values()) {
            names.put(color.getName(), color);
            tags.put(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "dyes/" + color.getName())), color);
            tags.put(TagKey.create(Registries.ITEM, new ResourceLocation("c", color.getName() + "_dyes")), color);
        }
        byName = Collections.unmodifiableMap(names);
        byTag = Collections.unmodifiableMap(tags);
    }

    public static final DyeColor[] byTier = new DyeColor[]{DyeColor.LIGHT_GRAY, DyeColor.YELLOW, DyeColor.CYAN, DyeColor.MAGENTA};

    public static Optional<TagKey<Item>> findDye(ItemStack stack) {
        for (TagKey<Item> tag : byTag.keySet()) {
            if (stack.is(tag)) return Optional.of(tag);
        }
        return Optional.empty();
    }

    public static boolean isDye(ItemStack stack) {
        return stack.getItem() instanceof DyeItem || findDye(stack).isPresent();
    }

    public static DyeColor dyeColor(ItemStack stack) {
        if (stack.getItem() instanceof DyeItem dye) {
            return dye.getDyeColor();
        }
        return findDye(stack).map(byTag::get).orElse(DyeColor.MAGENTA);
    }
}
