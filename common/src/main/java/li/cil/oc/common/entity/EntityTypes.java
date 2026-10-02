package li.cil.oc.common.entity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class EntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(OpenComputers.ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<Drone>> DRONE = ENTITY_TYPES.register("drone", () ->
        EntityType.Builder.<Drone>of(Drone::new, MobCategory.MISC)
            .sized(12 / 16f, 6 / 16f).fireImmune().build("drone"));

    public static void init() {
        ENTITY_TYPES.register();
    }

    private EntityTypes() {
        throw new Error();
    }
}
