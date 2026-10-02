package li.cil.oc.util;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.Tier;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static Optional<String> getDisplayName(CompoundTag nbt) {
        if (nbt.contains("display")) {
            final CompoundTag displayNbt = nbt.getCompound("display");
            if (displayNbt.contains("Name"))
                return Optional.ofNullable(displayNbt.getString("Name"));
        }
        return Optional.empty();
    }

    public static void setDisplayName(CompoundTag nbt, String name) {
        if (!nbt.contains("display")) {
            nbt.put("display", new CompoundTag());
        }
        nbt.getCompound("display").putString("Name", name);
    }

    private static boolean is(ItemInfo descriptor, String name) {
        return descriptor == li.cil.oc.api.Items.get(name);
    }

    public static int caseTier(ItemStack stack) {
        final ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
        if (is(descriptor, Constants.BlockName.CaseTier1)) return Tier.One;
        else if (is(descriptor, Constants.BlockName.CaseTier2)) return Tier.Two;
        else if (is(descriptor, Constants.BlockName.CaseTier3)) return Tier.Three;
        else if (is(descriptor, Constants.BlockName.CaseCreative)) return Tier.Four;
        else if (is(descriptor, Constants.ItemName.MicrocontrollerCaseTier1)) return Tier.One;
        else if (is(descriptor, Constants.ItemName.MicrocontrollerCaseTier2)) return Tier.Two;
        else if (is(descriptor, Constants.ItemName.MicrocontrollerCaseCreative)) return Tier.Four;
        else if (is(descriptor, Constants.ItemName.DroneCaseTier1)) return Tier.One;
        else if (is(descriptor, Constants.ItemName.DroneCaseTier2)) return Tier.Two;
        else if (is(descriptor, Constants.ItemName.DroneCaseCreative)) return Tier.Four;
        else if (is(descriptor, Constants.ItemName.ServerTier1)) return Tier.One;
        else if (is(descriptor, Constants.ItemName.ServerTier2)) return Tier.Two;
        else if (is(descriptor, Constants.ItemName.ServerTier3)) return Tier.Three;
        else if (is(descriptor, Constants.ItemName.ServerCreative)) return Tier.Four;
        else if (is(descriptor, Constants.ItemName.TabletCaseTier1)) return Tier.One;
        else if (is(descriptor, Constants.ItemName.TabletCaseTier2)) return Tier.Two;
        else if (is(descriptor, Constants.ItemName.TabletCaseCreative)) return Tier.Four;
        else return Tier.None;
    }

    public static String caseNameWithTierSuffix(String name, int tier) {
        return name + (tier == Tier.Four ? "creative" : Integer.toString(tier + 1));
    }

    public static CompoundTag loadTag(byte[] data) {
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            return NbtIo.readCompressed(bais);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] saveStack(ItemStack stack) {
        final CompoundTag tag = new CompoundTag();
        stack.save(tag);
        return saveTag(tag);
    }

    public static byte[] saveTag(CompoundTag tag) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(tag, baos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    private record FilteredInputs(List<ItemStack> inputs, int outputSize) {
    }

    private static FilteredInputs getFilteredInputs(List<ItemStack> inputs, int outputSize) {
        final List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack input : inputs) {
            if (!input.isEmpty() &&
                    input.getCount() / outputSize > 0 &&
                    // Strip out buckets, because those are returned when crafting, and
                    // we have no way of returning the fluid only (and I can't be arsed
                    // to make it output fluids into fluiducts or such, sorry).
                    !(input.getItem() instanceof BucketItem)) {
                filtered.add(input);
            }
        }
        return new FilteredInputs(filtered, outputSize);
    }

    private static boolean isInputBlacklisted(ItemStack stack) {
        final Item item = stack.getItem();
        // Note: the Scala version compared ResourceLocations against the configured
        // strings (which never matched); compare the string form instead.
        if (item instanceof BlockItem blockItem) {
            return Settings.get().disassemblerInputBlacklist.contains(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
        } else {
            return Settings.get().disassemblerInputBlacklist.contains(BuiltInRegistries.ITEM.getKey(item).toString());
        }
    }

    public static ItemStack[] getIngredients(RecipeManager manager, ItemStack stack) {
        return getIngredients(manager, RegistryAccess.EMPTY, stack);
    }

    public static ItemStack[] getIngredients(RecipeManager manager, RegistryAccess registryAccess, ItemStack stack) {
        try {
            FilteredInputs found = null;
            for (CraftingRecipe recipe : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
                final ItemStack result = recipe.getResultItem(registryAccess);
                if (result.isEmpty() || !ItemStack.isSameItem(result, stack)) continue;
                final FilteredInputs candidate;
                if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
                    candidate = getFilteredInputs(resolveOreDictEntries(recipe.getIngredients()), result.getCount());
                } else continue;
                if (candidate.inputs().stream().noneMatch(ItemUtils::isInputBlacklisted)) {
                    found = candidate;
                    break;
                }
            }
            if (found == null) {
                return new ItemStack[0];
            }
            final List<ItemStack> ingredients = found.inputs();
            final int count = found.outputSize();

            // Avoid positive feedback loops.
            if (ingredients.stream().anyMatch(ingredient -> ItemStack.isSameItem(ingredient, stack))) {
                return new ItemStack[0];
            }
            // Merge equal items for size division by output size.
            final List<ItemStack> merged = new ArrayList<>();
            for (ItemStack ingredient : ingredients) {
                final Optional<ItemStack> entry = merged.stream().filter(s -> ItemStack.isSameItem(s, ingredient)).findFirst();
                if (entry.isPresent()) entry.get().grow(ingredient.getCount());
                else merged.add(ingredient.copy());
            }
            merged.forEach(s -> s.setCount(s.getCount() / count));
            // Split items up again to 'disassemble them individually'.
            final List<ItemStack> distinct = new ArrayList<>();
            for (ItemStack ingredient : merged) {
                final int size = Math.max(ingredient.getCount(), 1);
                ingredient.setCount(1);
                for (int i = 0; i < size; i++) {
                    distinct.add(ingredient.copy());
                }
            }
            return distinct.toArray(new ItemStack[0]);
        } catch (Throwable t) {
            OpenComputers.log.warn("Whoops, something went wrong when trying to figure out an item's parts.", t);
            return new ItemStack[0];
        }
    }

    private static Random rng;

    private static Random rng() {
        if (rng == null) rng = new Random();
        return rng;
    }

    private static List<ItemStack> resolveOreDictEntries(NonNullList<Ingredient> entries) {
        final List<ItemStack> result = new ArrayList<>();
        for (Ingredient ing : entries) {
            if (ing != null && ing.getItems().length > 0) {
                final ItemStack[] items = ing.getItems();
                result.add(items[rng().nextInt(items.length)]);
            }
        }
        return result;
    }
}
