package li.cil.oc.server.loot;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public final class LootFunctions {
    public static final ResourceLocation DYN_ITEM_DATA = new ResourceLocation(OpenComputers.ID, "item_data");
    public static final ResourceLocation DYN_VOLATILE_CONTENTS = new ResourceLocation(OpenComputers.ID, "volatile_contents");

    private static final DeferredRegister<LootItemFunctionType> REGISTRY = DeferredRegister.create(OpenComputers.ID, Registries.LOOT_FUNCTION_TYPE);

    public static final RegistrySupplier<LootItemFunctionType> SET_COLOR = register("set_color", new SetColor.Serializer());
    public static final RegistrySupplier<LootItemFunctionType> COPY_COLOR = register("copy_color", new CopyColor.Serializer());

    private static RegistrySupplier<LootItemFunctionType> register(String name, Serializer<? extends LootItemFunction> serializer) {
        return REGISTRY.register(name, () -> new LootItemFunctionType(serializer));
    }

    /**
     * Registers the loot function types; called from {@code common.Proxy.preInit()}
     * (during mod construction).
     */
    public static void init() {
        REGISTRY.register();
    }

    private LootFunctions() {
    }
}
