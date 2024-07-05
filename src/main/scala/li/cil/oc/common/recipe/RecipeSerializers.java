package li.cil.oc.common.recipe;

import li.cil.oc.OpenComputers;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder("opencomputers")
public class RecipeSerializers {
    public static final RecipeSerializer<?> CRAFTING_LOOTDISK_CYCLING = null;
    public static final RecipeSerializer<?> CRAFTING_COLORIZE = null;
    public static final RecipeSerializer<?> CRAFTING_DECOLORIZE = null;
    public static final RecipeSerializer<?> CRAFTING_SHAPED_EXTENDED = null;
    public static final RecipeSerializer<?> CRAFTING_SHAPELESS_EXTENDED = null;

    @SubscribeEvent
    public static void registerSerializers(RegistryEvent.Register<RecipeSerializer<?>> e) {
        register(e.getRegistry(), "crafting_lootdisk_cycling", new SpecialRecipeSerializer<>(LootDiskCyclingRecipe::new));
        register(e.getRegistry(), "crafting_colorize", new ItemSpecialSerializer<>(ColorizeRecipe::new, ColorizeRecipe::targetItem));
        register(e.getRegistry(), "crafting_decolorize", new ItemSpecialSerializer<>(DecolorizeRecipe::new, DecolorizeRecipe::targetItem));
        register(e.getRegistry(), "crafting_shaped_extended", new ExtendedShapedRecipe.Serializer());
        register(e.getRegistry(), "crafting_shapeless_extended", new ExtendedShapelessRecipe.Serializer());
    }

    private static <S extends IForgeRegistryEntry<RecipeSerializer<?>> & RecipeSerializer<?>>
        void register(IForgeRegistry<RecipeSerializer<?>> registry, String name, S serializer) {

        serializer.setRegistryName(new ResourceLocation(OpenComputers.ID(), name));
        registry.register(serializer);
    }

    private RecipeSerializers() {
        throw new Error();
    }
}
