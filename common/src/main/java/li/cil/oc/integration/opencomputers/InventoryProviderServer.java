package li.cil.oc.integration.opencomputers;

import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.common.inventory.ServerInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryProviderServer implements InventoryProvider {
    public static final InventoryProviderServer INSTANCE = new InventoryProviderServer();

    private InventoryProviderServer() {
    }

    @Override
    public boolean worksWith(ItemStack stack, Player player) {
        return DriverServer.INSTANCE.worksWith(stack);
    }

    @Override
    public Container getInventory(ItemStack stack, Player player) {
        return new ServerInventory() {
            @Override
            public ItemStack container() {
                return stack;
            }

            @Override
            public int rackSlot() {
                return -1;
            }
        };
    }
}
