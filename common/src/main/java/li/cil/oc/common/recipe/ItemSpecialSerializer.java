package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;

/**
 * Serializer for special recipes parameterized with one item ({@code "item": "..."}).
 */
public class ItemSpecialSerializer<T extends CraftingRecipe> implements RecipeSerializer<T> {
    @FunctionalInterface
    public interface Factory<T> {
        T create(ResourceLocation id, CraftingBookCategory category, ItemLike target);
    }

    private final Factory<T> ctor;
    private final Function<T, Item> getter;

    public ItemSpecialSerializer(Factory<T> ctor, Function<T, Item> getter) {
        this.ctor = ctor;
        this.getter = getter;
    }

    @Override
    public T fromJson(ResourceLocation recipeId, JsonObject json) {
        final ResourceLocation loc = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        if (!BuiltInRegistries.ITEM.containsKey(loc)) {
            throw new JsonSyntaxException("Unknown item '" + loc + "'");
        }
        final CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
        return ctor.create(recipeId, category, BuiltInRegistries.ITEM.get(loc));
    }

    @Override
    public T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buff) {
        final CraftingBookCategory category = buff.readEnum(CraftingBookCategory.class);
        final Item item = buff.readById(BuiltInRegistries.ITEM);
        return ctor.create(recipeId, category, item);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buff, T recipe) {
        buff.writeEnum(recipe.category());
        buff.writeId(BuiltInRegistries.ITEM, getter.apply(recipe));
    }
}
