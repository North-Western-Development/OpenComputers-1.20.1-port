package li.cil.oc.common.inventory;

import li.cil.oc.Settings;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.integration.opencomputers.DriverUpgradeDatabase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface DatabaseInventory extends ItemStackInventory, MenuProvider {
    default int tier() {
        return DriverUpgradeDatabase.INSTANCE.tier(container());
    }

    @Override
    default int getContainerSize() {
        return Settings.get().databaseEntriesPerTier[tier()];
    }

    @Override
    default String inventoryName() {
        return "database";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default int getInventoryStackRequired() {
        return 1;
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return stack != container();
    }

    @Override
    default Component getDisplayName() {
        return Component.empty();
    }

    @Override
    default AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Database(ContainerTypes.DATABASE.get(), id, playerInventory, container(), this, tier());
    }
}
