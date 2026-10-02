package li.cil.oc.common.recipe;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.Loot;
import li.cil.oc.integration.util.Wrench;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootDiskCyclingRecipe implements CraftingRecipe {
    private final ResourceLocation id;

    private final CraftingBookCategory category;

    public final NonNullList<Ingredient> ingredients = NonNullList.create();

    public LootDiskCyclingRecipe(ResourceLocation id, CraftingBookCategory category) {
        this.id = id;
        this.category = category;
        ingredients.add(Ingredient.of(Loot.disksForCycling().toArray(new ItemStack[0])));
        ingredients.add(Ingredient.of(li.cil.oc.api.Items.get(Constants.ItemName.Wrench).createItemStack(1)));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public boolean matches(CraftingContainer crafting, Level world) {
        final List<ItemStack> stacks = collectStacks(crafting);
        return stacks.size() == 2 && stacks.stream().anyMatch(Loot::isLootDisk) && stacks.stream().anyMatch(Wrench::isWrench);
    }

    @Override
    public ItemStack assemble(CraftingContainer crafting, RegistryAccess registryAccess) {
        final List<ItemStack> lootDiskStacks = Loot.disksForCycling();
        final Optional<ItemStack> lootDisk = collectStacks(crafting).stream().filter(Loot::isLootDisk).findFirst();
        if (lootDisk.isPresent() && !lootDiskStacks.isEmpty()) {
            final String lootFactoryName = getLootFactoryName(lootDisk.get());
            int oldIndex = -1;
            for (int i = 0; i < lootDiskStacks.size(); i++) {
                if (getLootFactoryName(lootDiskStacks.get(i)).equals(lootFactoryName)) {
                    oldIndex = i;
                    break;
                }
            }
            final int newIndex = (oldIndex + 1) % lootDiskStacks.size();
            return lootDiskStacks.get(newIndex).copy();
        }
        return ItemStack.EMPTY;
    }

    public String getLootFactoryName(ItemStack stack) {
        return stack.getTag().getString(Settings.namespace + "lootFactory");
    }

    public List<ItemStack> collectStacks(CraftingContainer crafting) {
        final List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < crafting.getContainerSize(); i++) {
            final ItemStack stack = crafting.getItem(i);
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        final List<ItemStack> disks = Loot.disksForCycling();
        return disks.isEmpty() ? ItemStack.EMPTY : disks.get(0);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer crafting) {
        final NonNullList<ItemStack> result = NonNullList.withSize(crafting.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < crafting.getContainerSize(); slot++) {
            final ItemStack stack = crafting.getItem(slot);
            if (Wrench.isWrench(stack)) {
                result.set(slot, stack.copy());
                stack.setCount(0);
            }
        }
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CRAFTING_LOOTDISK_CYCLING.get();
    }
}
