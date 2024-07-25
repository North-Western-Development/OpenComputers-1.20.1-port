package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistryEntry;

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
    public ItemStack assemble(CraftingContainer inv) {
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

    public static final class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>>
        implements RecipeSerializer<ExtendedShapelessRecipe> {

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
            RecipeSerializer<ShapelessRecipe> serializer =
                (RecipeSerializer<ShapelessRecipe>) recipe.wrapped.getSerializer();
            serializer.toNetwork(buff, recipe.wrapped);
        }
    }
}
