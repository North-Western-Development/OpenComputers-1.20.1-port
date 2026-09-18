package li.cil.oc.server.loot;

import li.cil.oc.OpenComputers;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class LootFunctions {
    public static final ResourceLocation DYN_ITEM_DATA = new ResourceLocation(OpenComputers.ID(), "item_data");
    public static final ResourceLocation DYN_VOLATILE_CONTENTS = new ResourceLocation(OpenComputers.ID(), "volatile_contents");

    private static final DeferredRegister<LootItemFunctionType> REGISTER = DeferredRegister.create(Registry.LOOT_FUNCTION_REGISTRY, OpenComputers.ID());
    public static final RegistryObject<LootItemFunctionType> SET_COLOR_REG = REGISTER.register("set_color", () -> new LootItemFunctionType(new SetColor.Serializer()));
    public static final RegistryObject<LootItemFunctionType> COPY_COLOR_REG = REGISTER.register("copy_color", () -> new LootItemFunctionType(new CopyColor.Serializer()));
//    public static final LootItemFunctionType SET_COLOR = register("set_color", new SetColor.Serializer());
//    public static final LootItemFunctionType COPY_COLOR = register("copy_color", new CopyColor.Serializer());

//    private static LootItemFunctionType register(String name, Serializer<? extends LootItemFunction> serializer) {
//        LootItemFunctionType type = new LootItemFunctionType(serializer);
//        Registry.register(Registry.LOOT_FUNCTION_TYPE, new ResourceLocation(OpenComputers.ID(), name), type);
//        return type;
//    }

    public static void init(IEventBus bus) {
        LootFunctions.REGISTER.register(bus);
    }

    private LootFunctions() {
    }
}
