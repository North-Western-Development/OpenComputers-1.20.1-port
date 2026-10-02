package li.cil.oc.integration.opencomputers;

import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.common.inventory.DatabaseInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryProviderDatabase implements InventoryProvider {
    public static final InventoryProviderDatabase INSTANCE = new InventoryProviderDatabase();

    private InventoryProviderDatabase() {
    }

    @Override
    public boolean worksWith(ItemStack stack, Player player) {
        return DriverUpgradeDatabase.INSTANCE.worksWith(stack);
    }

    @Override
    public Container getInventory(ItemStack stack, Player player) {
        return new DatabaseInventory() {
            @Override
            public ItemStack container() {
                return stack;
            }

            @Override
            public boolean stillValid(Player other) {
                // Same as on 1.16.5 (compares the parameter with itself).
                return other == other;
            }
        };
    }
}
