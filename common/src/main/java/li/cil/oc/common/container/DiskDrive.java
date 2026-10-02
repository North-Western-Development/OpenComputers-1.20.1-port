package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class DiskDrive extends Player {
    public DiskDrive(MenuType<?> selfType, int id, Inventory playerInventory, Container drive) {
        super(selfType, id, playerInventory, drive);
        addSlotToContainer(80, 35, Slot.Floppy);
        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.DiskDrive.class;
    }
}
