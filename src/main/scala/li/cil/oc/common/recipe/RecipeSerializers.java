package li.cil.oc.common.recipe;

import li.cil.oc.OpenComputers;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, OpenComputers.ID());

    public static final RegistryObject<RecipeSerializer<?>> CRAFTING_LOOTDISK_CYCLING =
        RECIPE_SERIALIZERS.register("crafting_lootdisk_cycling",
            () -> new SimpleRecipeSerializer<>(LootDiskCyclingRecipe::new));

    public static final RegistryObject<RecipeSerializer<?>> CRAFTING_COLORIZE =
        RECIPE_SERIALIZERS.register("crafting_colorize",
            () -> new ItemSpecialSerializer<>(ColorizeRecipe::new, ColorizeRecipe::targetItem));

    public static final RegistryObject<RecipeSerializer<?>> CRAFTING_DECOLORIZE =
        RECIPE_SERIALIZERS.register("crafting_decolorize",
            () -> new ItemSpecialSerializer<>(DecolorizeRecipe::new, DecolorizeRecipe::targetItem));

    public static final RegistryObject<RecipeSerializer<?>> CRAFTING_SHAPED_EXTENDED =
        RECIPE_SERIALIZERS.register("crafting_shaped_extended",
            ExtendedShapedRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<?>> CRAFTING_SHAPELESS_EXTENDED =
        RECIPE_SERIALIZERS.register("crafting_shapeless_extended",
            ExtendedShapelessRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }

    private RecipeSerializers() {
        throw new Error();
    }
}
