package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * Shapeless recipe whose result gets OC data applied (see {@link ExtendedRecipe}).
 */
public class ExtendedShapelessRecipe extends ShapelessRecipe {
    public ExtendedShapelessRecipe(ShapelessRecipe wrapped) {
        this(wrapped, wrapped.getResultItem(null));
    }

    public ExtendedShapelessRecipe(ShapelessRecipe wrapped, ItemStack result) {
        super(wrapped.getId(), wrapped.getGroup(), wrapped.category(), result, wrapped.getIngredients());
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        return ExtendedRecipe.addNBTToResult(this, super.assemble(inv, registryAccess), inv);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CRAFTING_SHAPELESS_EXTENDED.get();
    }

    public static final class Serializer implements RecipeSerializer<ExtendedShapelessRecipe> {
        @Override
        public ExtendedShapelessRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final ShapelessRecipe wrapped = RecipeSerializer.SHAPELESS_RECIPE.fromJson(recipeId, json);
            return new ExtendedShapelessRecipe(wrapped, ExtendedResult.withNbt(wrapped.getResultItem(null), json));
        }

        @Override
        public ExtendedShapelessRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buff) {
            final ShapelessRecipe wrapped = RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(recipeId, buff);
            return new ExtendedShapelessRecipe(wrapped);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buff, ExtendedShapelessRecipe recipe) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buff, recipe);
        }
    }
}
