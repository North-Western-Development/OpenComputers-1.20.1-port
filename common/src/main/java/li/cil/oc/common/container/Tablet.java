package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.integration.opencomputers.DriverScreen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Tablet extends Player {
    public final ItemStack stack;

    public Tablet(MenuType<?> selfType, int id, Inventory playerInventory, ItemStack stack, Container tablet, String slot1, int tier1) {
        super(selfType, id, playerInventory, tablet);
        this.stack = stack;
        addSlot(new StaticComponentSlot(this, otherInventory, otherInventory.getContainerSize() - 1, 80, 35, getHostClass(), slot1, tier1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (DriverScreen.INSTANCE.worksWith(stack, getHostClass())) return false;
                return super.mayPlace(stack);
            }
        });

        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.item.TabletWrapper.class;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return player == playerInventory.player;
    }
}
