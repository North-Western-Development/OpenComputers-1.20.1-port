package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.util.ExtendedInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class DriverDiskDriveMountable implements Item, HostAware {
    public static final DriverDiskDriveMountable INSTANCE = new DriverDiskDriveMountable();

    private DriverDiskDriveMountable() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.DiskDriveMountable));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host instanceof li.cil.oc.api.internal.Rack rack) {
            return new li.cil.oc.server.component.DiskDriveMountable(rack, ExtendedInventory.asList(rack).indexOf(stack));
        }
        return null; // Welp.
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.RackMountable;
    }

    @Override
    public CompoundTag dataTag(ItemStack stack) {
        return stack.getOrCreateTag();
    }
}
