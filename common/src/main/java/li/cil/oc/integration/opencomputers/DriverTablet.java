package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.item.Tablet;
import li.cil.oc.common.item.data.TabletData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class DriverTablet implements Item {
    public static final DriverTablet INSTANCE = new DriverTablet();

    private DriverTablet() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.Tablet));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        Tablet.Server.INSTANCE.cache.invalidate(Tablet.getOrCreateId(stack));
        final TabletData data = new TabletData(stack);
        for (ItemStack fs : data.items) {
            if (!fs.isEmpty() && DriverFileSystem.INSTANCE.worksWith(fs)) {
                final ManagedEnvironment environment = DriverFileSystem.INSTANCE.createEnvironment(fs, host);
                if (environment != null && environment.node() instanceof Component component) {
                    component.setVisibility(Visibility.Network);
                    environment.saveData(dataTag(stack));
                    return environment;
                }
                return null;
            }
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Tablet;
    }

    @Override
    public CompoundTag dataTag(ItemStack stack) {
        final TabletData data = new TabletData(stack);
        int index = -1;
        for (int i = 0; i < data.items.length; i++) {
            final ItemStack fs = data.items[i];
            if (!fs.isEmpty() && DriverFileSystem.INSTANCE.worksWith(fs)) {
                index = i;
                break;
            }
        }
        if (index >= 0 && stack.hasTag() && stack.getTag().contains(Settings.namespace + "items")) {
            final CompoundTag baseTag = stack.getTag().getList(Settings.namespace + "items", Tag.TAG_COMPOUND).getCompound(index);
            if (!baseTag.contains("item")) {
                baseTag.put("item", new CompoundTag());
            }
            final CompoundTag itemTag = baseTag.getCompound("item");
            if (!itemTag.contains("tag")) {
                itemTag.put("tag", new CompoundTag());
            }
            final CompoundTag stackTag = itemTag.getCompound("tag");
            if (!stackTag.contains(Settings.namespace + "data")) {
                stackTag.put(Settings.namespace + "data", new CompoundTag());
            }
            return stackTag.getCompound(Settings.namespace + "data");
        } else return new CompoundTag();
    }
}
