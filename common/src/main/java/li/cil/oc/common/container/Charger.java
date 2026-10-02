package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.integration.util.ItemCharge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Charger extends Player {
    public Charger(MenuType<?> selfType, int id, Inventory playerInventory, Container charger) {
        super(selfType, id, playerInventory, charger);
        addSlot(new StaticComponentSlot(this, otherInventory, slots.size(), 80, 35, getHostClass(), "tablet", Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!container.canPlaceItem(getContainerSlot(), stack)) return false;
                return ItemCharge.canCharge(stack);
            }
        });
        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Charger.class;
    }
}
