package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Shaped recipe whose result gets OC data applied (see {@link ExtendedRecipe}). Extends
 * {@link ShapedRecipe} (instead of wrapping it) so recipe book placement keeps working on
 * both loaders (formerly Forge's IShapedRecipe).
 */
public class ExtendedShapedRecipe extends ShapedRecipe {
    public ExtendedShapedRecipe(ShapedRecipe wrapped) {
        this(wrapped, wrapped.getResultItem(null));
    }

    public ExtendedShapedRecipe(ShapedRecipe wrapped, ItemStack result) {
        super(wrapped.getId(), wrapped.getGroup(), wrapped.category(), wrapped.getWidth(), wrapped.getHeight(),
            wrapped.getIngredients(), result, wrapped.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        return ExtendedRecipe.addNBTToResult(this, super.assemble(inv, registryAccess), inv);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CRAFTING_SHAPED_EXTENDED.get();
    }

    public static final class Serializer implements RecipeSerializer<ExtendedShapedRecipe> {
        @Override
        public ExtendedShapedRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final ShapedRecipe wrapped = RecipeSerializer.SHAPED_RECIPE.fromJson(recipeId, json);
            return new ExtendedShapedRecipe(wrapped, ExtendedResult.withNbt(wrapped.getResultItem(null), json));
        }

        @Override
        public ExtendedShapedRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buff) {
            final ShapedRecipe wrapped = RecipeSerializer.SHAPED_RECIPE.fromNetwork(recipeId, buff);
            return new ExtendedShapedRecipe(wrapped);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buff, ExtendedShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buff, recipe);
        }
    }
}
