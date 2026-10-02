package li.cil.oc.common.recipe;

import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vexatos
 */
public class DecolorizeRecipe extends CustomRecipe {
    public final Item targetItem;

    public DecolorizeRecipe(ResourceLocation id, CraftingBookCategory category, ItemLike target) {
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
        return targets.size() == 1 && other.size() == 1 && other.stream().allMatch(stack -> stack.getItem() == Items.WATER_BUCKET);
    }

    @Override
    public ItemStack assemble(CraftingContainer crafting, RegistryAccess registryAccess) {
        ItemStack targetStack = ItemStack.EMPTY;

        for (ItemStack stack : stacks(crafting)) {
            if (stack.getItem() == targetItem) {
                targetStack = stack.copy();
                targetStack.setCount(1);
            } else if (stack.getItem() != Items.WATER_BUCKET) {
                return ItemStack.EMPTY;
            }
        }

        if (targetStack.isEmpty()) return ItemStack.EMPTY;

        ItemColorizer.removeColor(targetStack);
        return targetStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CRAFTING_DECOLORIZE.get();
    }
}
