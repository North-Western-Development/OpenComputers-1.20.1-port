package li.cil.oc.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * @author asie, Vexatos
 */
public final class ItemColorizer {
    private ItemColorizer() {
    }

    /**
     * Return whether the specified armor ItemStack has a color.
     */
    public static boolean hasColor(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("display") && stack.getTag().getCompound("display").contains("color");
    }

    /**
     * Return the color for the specified armor ItemStack.
     */
    public static int getColor(ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("display")) {
                final CompoundTag displayTag = tag.getCompound("display");
                return displayTag.contains("color") ? displayTag.getInt("color") : -1;
            } else return -1;
        } else return -1;
    }

    public static void removeColor(ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        if (tag != null) {
            final CompoundTag displayTag = tag.getCompound("display");
            if (displayTag.contains("color")) displayTag.remove("color");
            if (displayTag.isEmpty()) tag.remove("display");
            if (tag.isEmpty()) stack.setTag(null);
        }
    }

    public static void setColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt("color", color);
    }
}
