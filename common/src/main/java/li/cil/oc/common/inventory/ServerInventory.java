package li.cil.oc.common.inventory;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.internal.Server;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.util.ItemUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface ServerInventory extends ItemStackInventory, MenuProvider {
    int rackSlot();

    default int tier() {
        return Math.max(ItemUtils.caseTier(container()), 0);
    }

    @Override
    default int getContainerSize() {
        return InventorySlots.server[tier()].length;
    }

    @Override
    default String inventoryName() {
        return "server";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default boolean stillValid(Player player) {
        return false;
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        DriverItem driver = Driver.driverFor(stack, Server.class);
        if (driver == null) return false;
        InventorySlots.InventorySlot provided = InventorySlots.server[tier()][slot];
        return driver.slot(stack).equals(provided.slot) && driver.tier(stack) <= provided.tier;
    }

    @Override
    default Component getDisplayName() {
        return getName();
    }

    @Override
    default AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Server(ContainerTypes.SERVER.get(), id, playerInventory, container(), this, tier(), rackSlot());
    }
}
