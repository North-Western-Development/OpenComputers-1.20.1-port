package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.inventory.DatabaseInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DriverUpgradeDatabase implements Item, HostAware {
    public static final DriverUpgradeDatabase INSTANCE = new DriverUpgradeDatabase();

    private DriverUpgradeDatabase() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.DatabaseUpgradeTier1),
                Items.get(Constants.ItemName.DatabaseUpgradeTier2),
                Items.get(Constants.ItemName.DatabaseUpgradeTier3));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        return new li.cil.oc.server.component.UpgradeDatabase(new DatabaseInventory() {
            @Override
            public ItemStack container() {
                return stack;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        });
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.UpgradeDatabase item) return item.tier;
        return Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverUpgradeDatabase.INSTANCE.worksWith(stack))
                return li.cil.oc.server.component.UpgradeDatabase.class;
            else return null;
        }
    }
}
