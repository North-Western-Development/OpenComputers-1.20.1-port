package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ExtendedShapelessRecipe implements CraftingRecipe {
    private ShapelessRecipe wrapped;

    public ExtendedShapelessRecipe(ShapelessRecipe wrapped) {
        this.wrapped = ExtendedRecipe.patchRecipe(wrapped);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        return wrapped.matches(inv, world);
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        return ExtendedRecipe.addNBTToResult(this, wrapped.assemble(inv, registryAccess), inv);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return wrapped.canCraftInDimensions(w, h);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return wrapped.getResultItem(registryAccess);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
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
        return RecipeSerializers.CRAFTING_SHAPELESS_EXTENDED;
    }

    @Override
    public String getGroup() {
        return wrapped.getGroup();
    }

    @Override
    public CraftingBookCategory category() {
        return wrapped.category();
    }

    public static final class Serializer implements RecipeSerializer<ExtendedShapelessRecipe> {

        @Override
        public ExtendedShapelessRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ShapelessRecipe wrapped = RecipeSerializer.SHAPELESS_RECIPE.fromJson(recipeId, json);
            return new ExtendedShapelessRecipe(wrapped);
        }

        @Override
        public ExtendedShapelessRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buff) {
            ShapelessRecipe wrapped = RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(recipeId, buff);
            return new ExtendedShapelessRecipe(wrapped);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buff, ExtendedShapelessRecipe recipe) {
            RecipeSerializer<net.minecraft.world.item.crafting.ShapelessRecipe> serializer =
                    (RecipeSerializer<ShapelessRecipe>) recipe.wrapped.getSerializer();
            serializer.toNetwork(buff, recipe.wrapped);
        }
    }
}
