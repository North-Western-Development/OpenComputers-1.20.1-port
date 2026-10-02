package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class Raid extends Player {
    public Raid(MenuType<?> selfType, int id, Inventory playerInventory, Container raid) {
        super(selfType, id, playerInventory, raid);
        addSlotToContainer(60, 23, Slot.HDD, Tier.Three);
        addSlotToContainer(80, 23, Slot.HDD, Tier.Three);
        addSlotToContainer(100, 23, Slot.HDD, Tier.Three);
        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Raid.class;
    }
}
