package li.cil.oc.common.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public final class RecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(OpenComputers.ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<SimpleCraftingRecipeSerializer<LootDiskCyclingRecipe>> CRAFTING_LOOTDISK_CYCLING =
        SERIALIZERS.register("crafting_lootdisk_cycling", () -> new SimpleCraftingRecipeSerializer<>(LootDiskCyclingRecipe::new));
    public static final RegistrySupplier<ItemSpecialSerializer<ColorizeRecipe>> CRAFTING_COLORIZE =
        SERIALIZERS.register("crafting_colorize", () -> new ItemSpecialSerializer<>(ColorizeRecipe::new, ColorizeRecipe::targetItem));
    public static final RegistrySupplier<ItemSpecialSerializer<DecolorizeRecipe>> CRAFTING_DECOLORIZE =
        SERIALIZERS.register("crafting_decolorize", () -> new ItemSpecialSerializer<>(DecolorizeRecipe::new, DecolorizeRecipe::targetItem));
    public static final RegistrySupplier<ExtendedShapedRecipe.Serializer> CRAFTING_SHAPED_EXTENDED =
        SERIALIZERS.register("crafting_shaped_extended", ExtendedShapedRecipe.Serializer::new);
    public static final RegistrySupplier<ExtendedShapelessRecipe.Serializer> CRAFTING_SHAPELESS_EXTENDED =
        SERIALIZERS.register("crafting_shapeless_extended", ExtendedShapelessRecipe.Serializer::new);

    private static boolean initialized;

    /** Called from {@code common.Proxy.preInit()}. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        SERIALIZERS.register();
    }

    private RecipeSerializers() {
        throw new Error();
    }
}
