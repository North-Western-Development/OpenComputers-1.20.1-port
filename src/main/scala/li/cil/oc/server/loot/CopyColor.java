package li.cil.oc.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;

public final class CopyColor implements LootItemFunction {
    private CopyColor() {
        super();
    }

    @Override
    public @NotNull LootItemFunctionType getType() {
        return LootFunctions.COPY_COLOR_REG.get();
    }

    public static class Builder implements LootItemFunction.Builder {
        @Override
        public @NotNull LootItemFunction build() {
            return new CopyColor();
        }
    }

    public static Builder copyColor() {
        return new Builder();
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext ctx) {
        if (stack.isEmpty()) return stack;
        BlockEntity te = ctx.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (te != null && te instanceof Colored) {
            // Can't use capability because it's already invalid - block breaks before drops are calculated.
            ItemColorizer.setColor(stack, ((Colored) te).getColor());
        }
        else ItemColorizer.removeColor(stack);
        return stack;
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<CopyColor> {
        @Override
        public void serialize(JsonObject jsonObject, CopyColor copyColor, JsonSerializationContext ctx) {
        }

        @Override
        public CopyColor deserialize(JsonObject src, JsonDeserializationContext ctx) {
            return new CopyColor();
        }
    }
}
