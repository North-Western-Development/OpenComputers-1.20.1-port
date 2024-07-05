package li.cil.oc.common.recipe;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class ItemSpecialSerializer<T extends Recipe<?>> extends ForgeRegistryEntry<RecipeSerializer<?>>
    implements RecipeSerializer<T> {

    private BiFunction<ResourceLocation, Item, T> ctor;
    private Function<T, Item> getter;

    public ItemSpecialSerializer(BiFunction<ResourceLocation, Item, T> ctor, Function<T, Item> getter) {
        this.ctor = ctor;
        this.getter = getter;
    }

    @Override
    public T fromJson(ResourceLocation recipeId, JsonObject json) {
        ResourceLocation loc = new ResourceLocation(GsonHelper.getAsString(json, "item"));
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
