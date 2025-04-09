package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.JSONUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ItemSpecialSerializer<T extends Recipe<?>>
    implements RecipeSerializer<T> {

    private BiFunction<ResourceLocation, ItemLike, T> ctor;
    private Function<T, Item> getter;

    public ItemSpecialSerializer(BiFunction<ResourceLocation, ItemLike, T> ctor, Function<T, Item> getter) {
        this.ctor = ctor;
        this.getter = getter;
    }

    @Override
    public T fromJson(ResourceLocation recipeId, JsonObject json) {
        ResourceLocation loc = new ResourceLocation(JSONUtils.getAsString(json, "item"));
        if (!ForgeRegistries.ITEMS.containsKey(loc)) {
            throw new JsonSyntaxException("Unknown item '" + loc + "'");
        }
        return ctor.apply(recipeId, ForgeRegistries.ITEMS.getValue(loc));
    }

    @Override
    public T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buff) {
        return ctor.apply(recipeId, buff.readRegistryIdUnsafe(ForgeRegistries.ITEMS));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buff, T recipe) {
        buff.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, getter.apply(recipe));
    }
}
