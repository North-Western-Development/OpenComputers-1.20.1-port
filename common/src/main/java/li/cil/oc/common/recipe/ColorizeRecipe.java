package li.cil.oc.common.recipe;

import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * @author asie, Vexatos
 */
public class ColorizeRecipe extends CustomRecipe {
    public final Item targetItem;

    public ColorizeRecipe(ResourceLocation id, CraftingBookCategory category, ItemLike target) {
        super(id, category);
        this.targetItem = target.asItem();
    }

    public Item targetItem() {
        return targetItem;
    }

    private static List<ItemStack> stacks(CraftingContainer crafting) {
        final List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < crafting.getContainerSize(); i++) {
            final ItemStack stack = crafting.getItem(i);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    @Override
    public boolean matches(CraftingContainer crafting, Level world) {
        final List<ItemStack> stacks = stacks(crafting);
        final List<ItemStack> targets = stacks.stream().filter(stack -> stack.getItem() == targetItem).toList();
        final List<ItemStack> other = stacks.stream().filter(stack -> !targets.contains(stack)).toList();
        return targets.size() == 1 && !other.isEmpty() && other.stream().allMatch(Color::isDye);
    }

    @Override
    public ItemStack assemble(CraftingContainer crafting, RegistryAccess registryAccess) {
        ItemStack targetStack = ItemStack.EMPTY;
        final int[] color = new int[]{0, 0, 0};
        int colorCount = 0;
        int maximum = 0;

        for (ItemStack stack : stacks(crafting)) {
            if (stack.getItem() == targetItem) {
                targetStack = stack.copy();
                targetStack.setCount(1);
            } else {
                if (!Color.isDye(stack))
                    return ItemStack.EMPTY;

                final float[] itemColor = Color.dyeColor(stack).getTextureDiffuseColors();
                final int red = (int) (itemColor[0] * 255.0F);
                final int green = (int) (itemColor[1] * 255.0F);
                final int blue = (int) (itemColor[2] * 255.0F);
                maximum += Math.max(red, Math.max(green, blue));
                color[0] += red;
                color[1] += green;
                color[2] += blue;
                colorCount = colorCount + 1;
            }
        }

        if (targetStack.isEmpty()) return ItemStack.EMPTY;

        if (targetItem == targetStack.getItem()) {
            if (ItemColorizer.hasColor(targetStack)) {
                final int itemColor = ItemColorizer.getColor(targetStack);
                final float red = (float) (itemColor >> 16 & 255) / 255.0F;
                final float green = (float) (itemColor >> 8 & 255) / 255.0F;
                final float blue = (float) (itemColor & 255) / 255.0F;
                maximum = (int) ((float) maximum + Math.max(red, Math.max(green, blue)) * 255.0F);
                color[0] = (int) ((float) color[0] + red * 255.0F);
                color[1] = (int) ((float) color[1] + green * 255.0F);
                color[2] = (int) ((float) color[2] + blue * 255.0F);
                colorCount = colorCount + 1;
            }
        }

        int red = color[0] / colorCount;
        int green = color[1] / colorCount;
        int blue = color[2] / colorCount;
        final float max = (float) maximum / (float) colorCount;
        final float div = (float) Math.max(red, Math.max(green, blue));
        red = (int) ((float) red * max / div);
        green = (int) ((float) green * max / div);
        blue = (int) ((float) blue * max / div);
        ItemColorizer.setColor(targetStack, (red << 16) | (green << 8) | blue);
        return targetStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CRAFTING_COLORIZE.get();
    }
}
