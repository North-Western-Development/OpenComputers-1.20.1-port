package li.cil.oc.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public final class CopyColor extends LootItemConditionalFunction {
    private CopyColor(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootFunctions.COPY_COLOR.get();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<Builder> {
        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyColor(getConditions());
        }
    }

    public static Builder copyColor() {
        return new Builder();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext ctx) {
        if (stack.isEmpty()) return stack;
        final BlockEntity te = ctx.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (te instanceof Colored colored) {
            // Can't use capability because it's already invalid - block breaks before drops are calculated.
            ItemColorizer.setColor(stack, colored.getColor());
        } else ItemColorizer.removeColor(stack);
        return stack;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<CopyColor> {
        @Override
        public CopyColor deserialize(JsonObject src, JsonDeserializationContext ctx, LootItemCondition[] conditions) {
            return new CopyColor(conditions);
        }
    }
}
