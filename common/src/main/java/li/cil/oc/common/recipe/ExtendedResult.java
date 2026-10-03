package li.cil.oc.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.ItemInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;

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
            loadEEPROMScripts(copy);
            return copy;
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid result nbt: " + e.getMessage());
        }
    }

    /**
     * EEPROM results may name a script (relative to {@link Settings#scriptPath}) instead of holding
     * the code / data bytes, e.g. the Lua BIOS recipe: {@code oc:data.oc:eeprom: "bios.lua"}.
     */
    private static void loadEEPROMScripts(ItemStack stack) {
        final ItemInfo info = li.cil.oc.api.Items.get(stack);
        if (info == null || !Constants.ItemName.EEPROM.equals(info.name()) || !stack.hasTag()) return;
        final CompoundTag data = stack.getTag().getCompound(Settings.namespace + "data");
        loadScript(data, Settings.namespace + "eeprom", Settings.get().eepromSize);
        loadScript(data, Settings.namespace + "userdata", Settings.get().eepromDataSize);
    }

    private static void loadScript(CompoundTag data, String key, int maxSize) {
        if (!data.contains(key, Tag.TAG_STRING)) return;
        final String path = data.getString(key);
        try (InputStream stream = OpenComputers.class.getResourceAsStream(Settings.scriptPath + path)) {
            if (stream == null) throw new JsonSyntaxException("Unknown EEPROM script '" + path + "'.");
            data.putByteArray(key, stream.readNBytes(maxSize));
        } catch (IOException e) {
            throw new JsonSyntaxException("Failed loading EEPROM script '" + path + "': " + e.getMessage());
        }
    }
}
