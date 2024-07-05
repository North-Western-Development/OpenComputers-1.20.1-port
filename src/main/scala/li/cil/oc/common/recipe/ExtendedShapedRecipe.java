package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class ExtendedShapedRecipe implements ICraftingRecipe, IShapedRecipe<CraftingInventory> {
    private ShapedRecipe wrapped;

    public ExtendedShapedRecipe(ShapedRecipe wrapped) {
        this.wrapped = ExtendedRecipe.patchRecipe(wrapped);
    }

    @Override
    public boolean matches(CraftingInventory inv, Level world) {
        return wrapped.matches(inv, world);
    }

    @Override
    public ItemStack assemble(CraftingInventory inv) {
        return ExtendedRecipe.addNBTToResult(this, wrapped.assemble(inv), inv);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return wrapped.canCraftInDimensions(w, h);
    }

    @Override
    public ItemStack getResultItem() {
        return wrapped.getResultItem();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        return wrapped.getRemainingItems(inv);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return wrapped.getIngredients();
    }

    @Override
    public ResourceLocation getId() {
        return wrapped.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CRAFTING_SHAPED_EXTENDED;
    }

    @Override
    public String getGroup() {
        return wrapped.getGroup();
    }

    @Override
    public int getRecipeWidth() {
        return wrapped.getRecipeWidth();
    }

    @Override
    public int getRecipeHeight() {
        return wrapped.getRecipeHeight();
    }

    public static final class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>>
        implements RecipeSerializer<ExtendedShapedRecipe> {

        @Override
        public ExtendedShapedRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ShapedRecipe wrapped = RecipeSerializer.SHAPED_RECIPE.fromJson(recipeId, json);
            return new ExtendedShapedRecipe(wrapped);
        }

        @Override
        public ExtendedShapedRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buff) {
            ShapedRecipe wrapped = RecipeSerializer.SHAPED_RECIPE.fromNetwork(recipeId, buff);
            return new ExtendedShapedRecipe(wrapped);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buff, ExtendedShapedRecipe recipe) {
            RecipeSerializer<ShapedRecipe> serializer =
                (RecipeSerializer<ShapedRecipe>) recipe.wrapped.getSerializer();
            serializer.toNetwork(buff, recipe.wrapped);
        }
    }
}
