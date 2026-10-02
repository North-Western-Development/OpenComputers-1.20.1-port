package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverComponentBus implements Item, Processor {
    public static final DriverComponentBus INSTANCE = new DriverComponentBus();

    private DriverComponentBus() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.ComponentBusTier1),
                Items.get(Constants.ItemName.ComponentBusTier2),
                Items.get(Constants.ItemName.ComponentBusTier3),
                Items.get(Constants.ItemName.ComponentBusCreative));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.ComponentBus;
    }

    @Override
    public int tier(ItemStack stack) {
        // Clamp item tier because the creative bus needs to fit into tier 3 slots.
        if (stack.getItem() instanceof li.cil.oc.common.item.ComponentBus bus) return Math.min(bus.tier, Tier.Three);
        return Tier.One;
    }

    @Override
    public int supportedComponents(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.ComponentBus bus) return Settings.get().cpuComponentSupport[bus.tier];
        return Tier.One;
    }

    @Override
    public Class<? extends Architecture> architecture(ItemStack stack) {
        return null;
    }
}
