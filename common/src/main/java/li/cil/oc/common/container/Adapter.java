package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class Adapter extends Player {
    public Adapter(MenuType<?> selfType, int id, Inventory playerInventory, Container adapter) {
        super(selfType, id, playerInventory, adapter);
        addSlotToContainer(80, 35, Slot.Upgrade);
        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Adapter.class;
    }
}
