package li.cil.oc.server.driver;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class keeps track of registered drivers and provides installation logic
 * for each registered driver.
 * <p>
 * Each component type must register its driver with this class to be used with
 * computers, since this class is used to determine whether an object is a
 * valid component or not.
 * <p>
 * All drivers must be installed once the game starts - in the init phase - and
 * are then injected into all computers started up past that point. A driver is
 * a set of functions made available to the computer. These functions will
 * usually require a component of the type the driver wraps to be installed in
 * the computer, but may also provide context-free functions.
 */
public final class Registry implements li.cil.oc.api.detail.DriverAPI {
    public static final Registry INSTANCE = new Registry();

    private Registry() {
    }

    public final List<DriverBlock> sidedBlocks = new ArrayList<>();

    public final List<DriverItem> items = new ArrayList<>();

    public final List<Converter> converters = new ArrayList<>();

    public final List<EnvironmentProvider> environmentProviders = new ArrayList<>();

    public final List<InventoryProvider> inventoryProviders = new ArrayList<>();

    public final List<Pair<ItemStack, Set<Class<?>>>> blacklist = new ArrayList<>();

    /**
     * Used to keep track of whether we're past the init phase.
     */
    public boolean locked = false;

    @Override
    public void add(DriverBlock driver) {
        if (locked) throw new IllegalStateException("Please register all drivers in the init phase.");
        if (!sidedBlocks.contains(driver)) {
            OpenComputers.log.debug("Registering block driver " + driver.getClass().getName() + ".");
            sidedBlocks.add(driver);
        }
    }

    @Override
    public void add(DriverItem driver) {
        if (locked) throw new IllegalStateException("Please register all drivers in the init phase.");
        if (!items.contains(driver)) {
            OpenComputers.log.debug("Registering item driver " + driver.getClass().getName() + ".");
            items.add(driver);
        }
    }

    @Override
    public void add(Converter converter) {
        if (locked) throw new IllegalStateException("Please register all converters in the init phase.");
        if (!converters.contains(converter)) {
            OpenComputers.log.debug("Registering converter " + converter.getClass().getName() + ".");
            converters.add(converter);
        }
    }

    @Override
    public void add(EnvironmentProvider provider) {
        if (locked) throw new IllegalStateException("Please register all environment providers in the init phase.");
        if (!environmentProviders.contains(provider)) {
            OpenComputers.log.debug("Registering environment provider " + provider.getClass().getName() + ".");
            environmentProviders.add(provider);
        }
    }

    @Override
    public void add(InventoryProvider provider) {
        if (locked) throw new IllegalStateException("Please register all inventory providers in the init phase.");
        if (!inventoryProviders.contains(provider)) {
            OpenComputers.log.debug("Registering inventory provider " + provider.getClass().getName() + ".");
            inventoryProviders.add(provider);
        }
    }

    @Override
    public DriverBlock driverFor(Level world, BlockPos pos, Direction side) {
        final List<DriverBlock> sidedDrivers = new ArrayList<>();
        for (DriverBlock driver : sidedBlocks) {
            if (driver.worksWith(world, pos, side)) sidedDrivers.add(driver);
        }
        if (!sidedDrivers.isEmpty()) return new CompoundBlockDriver(sidedDrivers.toArray(new DriverBlock[0]));
        return null;
    }

    @Override
    public DriverItem driverFor(ItemStack stack, Class<? extends EnvironmentHost> host) {
        if (stack.isEmpty()) return null;
        final List<HostAware> hostAware = new ArrayList<>();
        for (DriverItem driver : items) {
            if (driver instanceof HostAware aware && driver.worksWith(stack)) hostAware.add(aware);
        }
        if (!hostAware.isEmpty()) {
            for (HostAware driver : hostAware) {
                if (driver.worksWith(stack, host)) return (DriverItem) driver;
            }
            return null;
        }
        return driverFor(stack);
    }

    @Override
    public DriverItem driverFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (DriverItem driver : items) {
            if (driver.worksWith(stack)) return driver;
        }
        return null;
    }

    @Deprecated
    @Override
    public Class<?> environmentFor(ItemStack stack) {
        for (EnvironmentProvider provider : environmentProviders) {
            final Class<?> clazz = provider.getEnvironment(stack);
            if (clazz != null) return clazz;
        }
        return null;
    }

    @Override
    public Set<Class<?>> environmentsFor(ItemStack stack) {
        final Set<Class<?>> result = new HashSet<>();
        for (EnvironmentProvider provider : environmentProviders) {
            final Class<?> clazz = provider.getEnvironment(stack);
            if (clazz != null) result.add(clazz);
        }
        return result;
    }

    @Override
    public ItemHandler itemHandlerFor(ItemStack stack, Player player) {
        for (InventoryProvider provider : inventoryProviders) {
            if (provider.worksWith(stack, player)) {
                return InventoryUtils.asItemHandler(provider.getInventory(stack, player));
            }
        }
        return PlatformHooks.getItemHandler(stack);
    }

    @Override
    public List<DriverItem> itemDrivers() {
        return new ArrayList<>(items);
    }

    public void blacklistHost(ItemStack stack, Class<?> host) {
        for (Pair<ItemStack, Set<Class<?>>> entry : blacklist) {
            if (ItemStack.isSameItem(entry.getLeft(), stack)) {
                entry.getRight().add(host);
                return;
            }
        }
        final Set<Class<?>> hosts = new LinkedHashSet<>();
        hosts.add(host);
        blacklist.add(Pair.of(stack, hosts));
    }

    public Object[] convert(Object[] value) {
        if (value == null) return null;
        final Object[] result = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            result[i] = convertRecursively(value[i], new IdentityHashMap<>());
        }
        return result;
    }

    public Object convertRecursively(Object value, IdentityHashMap<Object, Object> memo) {
        return convertRecursively(value, memo, false);
    }

    public Object convertRecursively(Object valueRef, IdentityHashMap<Object, Object> memo, boolean force) {
        if (!force && valueRef != null && memo.containsKey(valueRef)) {
            return memo.get(valueRef);
        }
        if (valueRef == null || valueRef == ResultWrapper.unit) return null;
        if (valueRef instanceof Optional<?> optional) {
            // Formerly Scala's Option: None became nil.
            return optional.isPresent() ? convertRecursively(optional.get(), memo) : null;
        }

        if (valueRef instanceof Boolean || valueRef instanceof Byte || valueRef instanceof Character ||
                valueRef instanceof Short || valueRef instanceof Integer || valueRef instanceof Long ||
                valueRef instanceof Float || valueRef instanceof Double) return valueRef;
        if (valueRef instanceof Number number) return number.doubleValue();
        if (valueRef instanceof String) return valueRef;

        if (valueRef instanceof boolean[] || valueRef instanceof byte[] || valueRef instanceof Character[] ||
                valueRef instanceof short[] || valueRef instanceof Integer[] || valueRef instanceof long[] ||
                valueRef instanceof float[] || valueRef instanceof double[] || valueRef instanceof String[])
            return valueRef;

        if (valueRef instanceof Value) return valueRef;

        if (valueRef.getClass().isArray()) {
            final int length = java.lang.reflect.Array.getLength(valueRef);
            final List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) list.add(java.lang.reflect.Array.get(valueRef, i));
            return convertList(valueRef, list.iterator(), memo);
        }
        // Formerly Scala Products (tuples).
        if (valueRef instanceof Pair<?, ?> pair) {
            return convertList(valueRef, java.util.Arrays.<Object>asList(pair.getLeft(), pair.getRight()).iterator(), memo);
        }
        if (valueRef instanceof Triple<?, ?, ?> triple) {
            return convertList(valueRef, java.util.Arrays.<Object>asList(triple.getLeft(), triple.getMiddle(), triple.getRight()).iterator(), memo);
        }

        if (valueRef instanceof Map<?, ?> map) return convertMap(valueRef, new HashMap<>(map), memo);

        if (valueRef instanceof Iterable<?> iterable) return convertList(valueRef, iterable.iterator(), memo);

        final Object arg = valueRef;
        final HashMap<Object, Object> converted = new HashMap<>();
        memo.put(arg, converted);
        for (Converter converter : converters) {
            try {
                converter.convert(arg, converted);
            } catch (Throwable t) {
                OpenComputers.log.warn("Type converter threw an exception.", t);
            }
        }
        if (converted.isEmpty()) {
            memo.put(arg, arg.toString());
            return arg.toString();
        } else {
            // This is a little nasty but necessary because we need to keep the
            // 'converted' value up-to-date for any reference created to it in
            // the following convertRecursively call. For example:
            // - Converter C is called for A with map M.
            // - C puts A into M again.
            // - convertRecursively(M) encounters A in the memoization map, uses M.
            //   That M is then 'wrong', as in not fully converted. Hence the clear
            //   plus copy action afterwards.
            memo.put(converted, converted); // Makes convertMap re-use the map.
            convertRecursively(converted, memo, true);
            memo.remove(converted);
            if (converted.size() == 1 && converted.containsKey("oc:flatten")) {
                final Object value = converted.get("oc:flatten");
                memo.put(arg, value); // Update memoization map.
                return value;
            } else {
                return converted;
            }
        }
    }

    public Object[] convertList(Object obj, Iterator<?> list, IdentityHashMap<Object, Object> memo) {
        final List<Object> converted = new ArrayList<>();
        memo.put(obj, converted);
        while (list.hasNext()) {
            converted.add(convertRecursively(list.next(), memo));
        }
        return converted.toArray();
    }

    @SuppressWarnings("unchecked")
    public Object convertMap(Object obj, Map<?, ?> map, IdentityHashMap<Object, Object> memo) {
        Object existing = memo.get(obj);
        if (existing == null) {
            existing = new HashMap<Object, Object>();
            memo.put(obj, existing);
        }
        final Map<Object, Object> converted = (Map<Object, Object>) existing;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                converted.put(convertRecursively(entry.getKey(), memo), convertRecursively(entry.getValue(), memo));
            }
        }
        return memo.get(obj);
    }
}
