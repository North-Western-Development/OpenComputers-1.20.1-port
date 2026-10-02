package li.cil.oc.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

/**
 * Reads {@code result.nbt} (SNBT string or JSON object) of OC's extended crafting recipes,
 * which vanilla's shaped / shapeless serializers drop.
 */
final class ExtendedResult {
    private ExtendedResult() {
    }

    static ItemStack withNbt(ItemStack result, JsonObject json) {
        if (!json.has("result") || !json.get("result").isJsonObject()) return result;
        final JsonObject resultJson = json.getAsJsonObject("result");
        if (!resultJson.has("nbt")) return result;
        final JsonElement nbt = resultJson.get("nbt");
        final String snbt = nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString();
        try {
            final CompoundTag tag = TagParser.parseTag(snbt);
            final ItemStack copy = result.copy();
            copy.setTag(tag);
            return copy;
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid result nbt: " + e.getMessage());
        }
    }
}
