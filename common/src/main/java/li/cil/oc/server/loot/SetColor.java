package li.cil.oc.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.OptionalInt;

public final class SetColor extends LootItemConditionalFunction {
    private final OptionalInt color;

    private SetColor(LootItemCondition[] conditions, OptionalInt color) {
        super(conditions);
        this.color = color;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootFunctions.SET_COLOR.get();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext ctx) {
        if (stack.isEmpty()) return stack;
        if (color.isPresent()) {
            ItemColorizer.setColor(stack, color.getAsInt());
        } else ItemColorizer.removeColor(stack);
        return stack;
    }

    public static class Builder extends LootItemConditionalFunction.Builder<Builder> {
        private OptionalInt color = OptionalInt.empty();

        @Override
        protected Builder getThis() {
            return this;
        }

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
        public LootItemFunction build() {
            return new SetColor(getConditions(), color);
        }
    }

    public static Builder setColor() {
        return new Builder();
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetColor> {
        @Override
        public void serialize(JsonObject dst, SetColor src, JsonSerializationContext ctx) {
            super.serialize(dst, src, ctx);
            src.color.ifPresent(v -> dst.add("color", new JsonPrimitive(v)));
        }

        @Override
        public SetColor deserialize(JsonObject src, JsonDeserializationContext ctx, LootItemCondition[] conditions) {
            if (src.has("color")) {
                final int color = GsonHelper.getAsInt(src, "color");
                if (color < 0 || color > 0xFFFFFF) throw new JsonParseException("Invalid RGB color: " + color);
                return new SetColor(conditions, OptionalInt.of(color));
            } else return new SetColor(conditions, OptionalInt.empty());
        }
    }
}
