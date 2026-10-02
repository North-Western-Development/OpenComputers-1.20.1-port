package li.cil.oc.common.inventory;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.Slot;
import li.cil.oc.common.container.ContainerTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface DiskDriveMountableInventory extends ItemStackInventory, MenuProvider {
    default int tier() {
        return 1;
    }

    @Override
    default int getContainerSize() {
        return 1;
    }

    @Override
    default String inventoryName() {
        return "diskdrive";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0) return false;
        DriverItem driver = Driver.driverFor(stack, li.cil.oc.common.tileentity.DiskDrive.class);
        return driver != null && Slot.Floppy.equals(driver.slot(stack));
    }

    @Override
    default Component getDisplayName() {
        return Component.empty();
    }

    @Override
    default AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.DiskDrive(ContainerTypes.DISK_DRIVE.get(), id, playerInventory, this);
    }
}
