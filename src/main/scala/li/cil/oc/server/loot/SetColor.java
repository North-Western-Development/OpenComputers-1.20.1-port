package li.cil.oc.server.loot;

import java.util.OptionalInt;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

public final class SetColor implements LootItemFunction {
    private final OptionalInt color;

    private SetColor(OptionalInt color) {
        this.color = color;
    }

    @Override
    public @NotNull LootItemFunctionType getType() {
        return LootFunctions.SET_COLOR;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext ctx) {
        if (stack.isEmpty()) return stack;
        if (color.isPresent()) {
            ItemColorizer.setColor(stack, color.getAsInt());
        }
        else ItemColorizer.removeColor(stack);
        return stack;
    }

    public static class Builder implements LootItemFunction.Builder {
        private OptionalInt color = OptionalInt.empty();

        public Builder withoutColor() {
            color = OptionalInt.empty();
            return this;
        }

        public Builder withColor(int color) {
            if (color < 0 || color > 0xFFFFFF) throw new IllegalArgumentException("Invalid RGB color: " + color);
            this.color = OptionalInt.of(color);
            return this;
        }

        @Override
        public @NotNull LootItemFunction build() {
            return new SetColor(color);
        }
    }

    public static Builder setColor() {
        return new Builder();
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<SetColor> {
        @Override
        public void serialize(JsonObject dst, SetColor src, JsonSerializationContext ctx) {
            src.color.ifPresent(v -> dst.add("color", new JsonPrimitive(v)));
        }

        @Override
        public SetColor deserialize(JsonObject src, JsonDeserializationContext ctx) {
            if (src.has("color")) {
                int color = GsonHelper.getAsInt(src, "color");
                if (color < 0 || color > 0xFFFFFF) throw new JsonParseException("Invalid RGB color: " + color);
                return new SetColor(OptionalInt.of(color));
            }
            else return new SetColor(OptionalInt.empty());
        }
    }
}
