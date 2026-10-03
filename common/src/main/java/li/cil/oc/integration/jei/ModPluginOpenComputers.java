package li.cil.oc.integration.jei;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.client.gui.Relay;
import li.cil.oc.integration.util.ItemSearch;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * OpenComputers' JEI plugin. JEI only loads it on the client (Forge: {@link JeiPlugin} annotation,
 * Fabric: {@code "jei_mod_plugin"} entrypoint in fabric.mod.json).
 */
@JeiPlugin
public class ModPluginOpenComputers implements IModPlugin {
    private static boolean itemSearchRegistered = false;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(OpenComputers.ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ManualUsageHandler.ManualUsageRecipeCategory.INSTANCE.initialize(registration.getJeiHelpers().getGuiHelper());
        CallbackDocHandler.CallbackDocRecipeCategory.INSTANCE.initialize(registration.getJeiHelpers().getGuiHelper());
        // Manual first: it should always be in front of the callback documentation.
        registration.addRecipeCategories(ManualUsageHandler.ManualUsageRecipeCategory.INSTANCE);
        registration.addRecipeCategories(CallbackDocHandler.CallbackDocRecipeCategory.INSTANCE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ManualUsageHandler.ManualUsageRecipeCategory.TYPE, ManualUsageHandler.getRecipes(registration));
        registration.addRecipes(CallbackDocHandler.CallbackDocRecipeCategory.TYPE, CallbackDocHandler.getRecipes(registration));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(Relay.class, RelayGuiHandler.INSTANCE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (!itemSearchRegistered) {
            itemSearchRegistered = true;
            ItemSearch.stackFocusing.add((container, mouseX, mouseY) -> ModJEI.runtime.map(runtime -> {
                final ItemStack fromList = runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
                if (fromList != null) return fromList;
                final ItemStack fromBookmarks = runtime.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
                return fromBookmarks != null ? fromBookmarks : ItemStack.EMPTY;
            }).orElse(ItemStack.EMPTY));
            ItemSearch.focusedInput.add(() -> ModJEI.runtime.map(runtime -> runtime.getIngredientListOverlay().hasKeyboardFocus()).orElse(false));
        }

        ModJEI.runtime = Optional.of(jeiRuntime);
        ModJEI.ingredientRegistry = Optional.of(jeiRuntime.getIngredientManager());
    }

    @Override
    public void onRuntimeUnavailable() {
        ModJEI.runtime = Optional.empty();
        ModJEI.ingredientRegistry = Optional.empty();
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Only the preconfigured blocks and items have to be here.
        final Set<Item> useNbt = new LinkedHashSet<>();
        for (String name : new String[]{
                Constants.BlockName.Microcontroller,
                Constants.BlockName.Robot,
                Constants.ItemName.Drone,
                Constants.ItemName.Tablet}) {
            final ItemInfo info = Items.get(name);
            if (info == null) continue;
            final Item item = info.item() != null ? info.item() : (info.block() != null ? info.block().asItem() : null);
            if (item != null) useNbt.add(item);
        }
        registration.useNbtForSubtypes(useNbt.toArray(new Item[0]));

        // Separate preconfigured EEPROMs (Lua BIOS) from blank ones by their label.
        final ItemInfo eeprom = Items.get(Constants.ItemName.EEPROM);
        if (eeprom != null && eeprom.item() != null) {
            registration.registerSubtypeInterpreter(eeprom.item(), new ISubtypeInterpreter<ItemStack>() {
                @Override
                public Object getSubtypeData(ItemStack stack, UidContext context) {
                    final CompoundTag data = stack.getTagElement(Settings.namespace + "data");
                    if (data == null || !data.contains(Settings.namespace + "label")) return null;
                    return data.getString(Settings.namespace + "label");
                }

                @Override
                public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                    final Object data = getSubtypeData(stack, context);
                    return data != null ? data.toString() : "";
                }
            });
        }

        final ItemInfo floppy = Items.get(Constants.ItemName.Floppy);
        if (floppy != null && floppy.item() != null) {
            registration.registerSubtypeInterpreter(floppy.item(), new ISubtypeInterpreter<ItemStack>() {
                // Separate loot disks from normal floppies.
                @Override
                public Object getSubtypeData(ItemStack stack, UidContext context) {
                    if (!stack.hasTag()) return null;
                    final Tag lootFactory = stack.getTag().get(Settings.namespace + "lootFactory");
                    return lootFactory != null ? lootFactory.toString() : null;
                }

                @Override
                public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                    final Object data = getSubtypeData(stack, context);
                    return data != null ? data.toString() : "";
                }
            });
        }
    }
}
