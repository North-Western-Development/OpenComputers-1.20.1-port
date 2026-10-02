package li.cil.oc.integration.opencomputers;

import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.server.driver.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.Set;

/**
 * Base interface of OC's own item drivers (Scala trait {@code Item}).
 * <p>
 * Drivers that also implement {@link li.cil.oc.api.driver.item.HostAware} must
 * override {@code worksWith(ItemStack, Class)} and delegate to
 * {@code Item.super.worksWith(stack, host)} (Java does not allow inheriting a
 * default and an abstract method with the same signature).
 * The companion's {@code dataTag(stack)} is available as {@link #dataTagStatic(ItemStack)}.
 */
public interface Item extends DriverItem {
    default boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        if (!worksWith(stack)) return false;
        for (Pair<ItemStack, Set<Class<?>>> entry : Registry.INSTANCE.blacklist) {
            if (ItemStack.isSameItem(stack, entry.getLeft())) {
                for (Class<?> blacklistedHost : entry.getRight()) {
                    if (blacklistedHost.isAssignableFrom(host)) return false;
                }
            }
        }
        return true;
    }

    @Override
    default int tier(ItemStack stack) {
        return Tier.One;
    }

    @Override
    default CompoundTag dataTag(ItemStack stack) {
        return dataTagStatic(stack);
    }

    default boolean isOneOf(ItemStack stack, ItemInfo... items) {
        final ItemInfo info = Items.get(stack);
        for (ItemInfo item : items) {
            if (item != null && item.equals(info)) return true;
        }
        return false;
    }

    default boolean isAdapter(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Adapter.class.isAssignableFrom(host);
    }

    default boolean isComputer(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Case.class.isAssignableFrom(host);
    }

    default boolean isRobot(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Robot.class.isAssignableFrom(host);
    }

    default boolean isRotatable(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Rotatable.class.isAssignableFrom(host);
    }

    default boolean isServer(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Server.class.isAssignableFrom(host);
    }

    default boolean isTablet(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Tablet.class.isAssignableFrom(host);
    }

    default boolean isMicrocontroller(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Microcontroller.class.isAssignableFrom(host);
    }

    default boolean isDrone(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Drone.class.isAssignableFrom(host);
    }

    // ----------------------------------------------------------------------- //
    // Companion object.

    static CompoundTag dataTagStatic(ItemStack stack) {
        final CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.contains(Settings.namespace + "data")) {
            nbt.put(Settings.namespace + "data", new CompoundTag());
        }
        return nbt.getCompound(Settings.namespace + "data");
    }

    private static Optional<CompoundTag> getTag(CompoundTag tagCompound, String[] keys) {
        CompoundTag current = tagCompound;
        for (String key : keys) {
            if (!current.contains(key)) return Optional.empty();
            current = current.getCompound(key);
        }
        return Optional.ofNullable(current);
    }

    private static Optional<CompoundTag> getTag(ItemStack stack, String[] keys) {
        if (stack == null || stack.getCount() == 0 || stack.isEmpty()) return Optional.empty();
        else if (!stack.hasTag()) return Optional.empty();
        else return getTag(stack.getTag(), keys);
    }

    static Optional<String> address(ItemStack stack) {
        final String addressKey = "address";
        final Optional<CompoundTag> tag = getTag(stack, new String[]{Settings.namespace + "data", "node"});
        if (tag.isPresent() && tag.get().contains(addressKey)) {
            return Optional.of(tag.get().getString(addressKey));
        }
        return Optional.empty();
    }
}
