package li.cil.oc.common.recipe;

import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;

@ObjectHolder("opencomputers")
public class RecipeSerializers {
    public static final RecipeSerializer<?> CRAFTING_LOOTDISK_CYCLING = null;
    public static final RecipeSerializer<?> CRAFTING_COLORIZE = null;
    public static final RecipeSerializer<?> CRAFTING_DECOLORIZE = null;
    public static final RecipeSerializer<?> CRAFTING_SHAPED_EXTENDED = null;
    public static final RecipeSerializer<?> CRAFTING_SHAPELESS_EXTENDED = null;

    @SubscribeEvent
    public static void registerSerializers(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                helper -> {

        helper.register(new ResourceLocation(OpenComputers.ID(), "crafting_lootdisk_cycling"), new SpecialRecipeSerializer<>(LootDiskCyclingRecipe::new));
        helper.register(new ResourceLocation(OpenComputers.ID(), "crafting_colorize"), new ItemSpecialSerializer<>(ColorizeRecipe::new, ColorizeRecipe::targetItem));
        helper.register(new ResourceLocation(OpenComputers.ID(), "crafting_decolorize"), new ItemSpecialSerializer<>(DecolorizeRecipe::new, DecolorizeRecipe::targetItem));
        helper.register(new ResourceLocation(OpenComputers.ID(), "crafting_shaped_extended"), new ExtendedShapedRecipe.Serializer());
        helper.register(new ResourceLocation(OpenComputers.ID(), "crafting_shapeless_extended"), new ExtendedShapelessRecipe.Serializer());
                }
        );
    }

    private static <S extends IForgeRegistryEntry<IRecipeSerializer<?>> && RecipeSerializer<?>>
        void register(IForgeRegistry<RecipeSerializer<?>> registry, String name, S serializer) {

        serializer.setRegistryName(new ResourceLocation(OpenComputers.ID(), name));
        registry.register(serializer);
    }

    private RecipeSerializers() {
        throw new Error();
    }
}
