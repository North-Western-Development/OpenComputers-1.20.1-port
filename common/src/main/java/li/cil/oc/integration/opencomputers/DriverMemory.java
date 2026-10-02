package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverMemory implements Item, li.cil.oc.api.driver.item.Memory, CallBudget {
    public static final DriverMemory INSTANCE = new DriverMemory();

    private DriverMemory() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.RAMTier1),
                Items.get(Constants.ItemName.RAMTier2),
                Items.get(Constants.ItemName.RAMTier3),
                Items.get(Constants.ItemName.RAMTier4),
                Items.get(Constants.ItemName.RAMTier5),
                Items.get(Constants.ItemName.RAMTier6));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new li.cil.oc.server.component.Memory(tier(stack));
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Memory;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.Memory item) return item.tier / 2;
        return Tier.One;
    }

    @Override
    public double amount(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.Memory memory) {
            final int[] sizes = Settings.get().ramSizes;
            return sizes[Math.min(Math.max(memory.tier, 0), sizes.length - 1)];
        }
        return 0.0;
    }

    @Override
    public double getCallBudget(ItemStack stack) {
        return Settings.get().callBudgets[Math.min(Math.max(tier(stack), Tier.One), Tier.Three)];
    }
}
