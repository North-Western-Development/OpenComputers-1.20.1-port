package li.cil.oc.fabric;

import li.cil.oc.common.transfer.FluidHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;

/**
 * Exposes OpenComputers' block entities to other Fabric mods: inventories and
 * tanks via the Transfer API, power acceptors and chargeable items via Team
 * Reborn Energy (if present).
 */
public final class FabricStorageProviders {
    private FabricStorageProviders() {
    }

    public static void register() {
        // Vanilla containers are already picked up by Fabric's fallback provider
        // for blocks; this covers OC blocks whose block entity is the container.
        ItemStorage.SIDED.registerFallback((level, pos, state, blockEntity, side) -> {
            if (blockEntity != null && isOurs(blockEntity) && blockEntity instanceof Container container) {
                return container instanceof WorldlyContainer ? InventoryStorage.of(container, side) : InventoryStorage.of(container, null);
            }
            return null;
        });

        FluidStorage.SIDED.registerFallback((level, pos, state, blockEntity, side) -> {
            if (blockEntity != null && isOurs(blockEntity) && blockEntity instanceof FluidHandler handler) {
                return new FluidHandlerStorage(handler);
            }
            return null;
        });

        if (FabricLoader.getInstance().isModLoaded("team_reborn_energy")) {
            TeamRebornEnergyCompat.registerProviders();
        }
    }

    static boolean isOurs(Object blockEntity) {
        return blockEntity.getClass().getName().startsWith("li.cil.oc.");
    }
}
