package li.cil.oc.common.entity;

import li.cil.oc.OpenComputers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OpenComputers.ID());

    public static final RegistryObject<EntityType<Drone>> DRONE =
        ENTITY_TYPES.register("drone", () ->
            EntityType.Builder.of(Drone::new, MobCategory.MISC)
                .sized(12 / 16f, 6 / 16f)
                .fireImmune()
                .build("drone")
        );

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private EntityTypes() {
        throw new Error();
    }
}
