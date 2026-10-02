package li.cil.oc.common.inventory;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.Lifecycle;
import li.cil.oc.integration.opencomputers.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Inventory whose items provide component environments.
 * <p>
 * Stateful Scala trait turned interface: the state lives in a {@link ComponentState} returned by
 * {@link #componentState()}. The default implementation keeps it in a weak map keyed by the
 * inventory; implementers should override it to return a field
 * ({@code private final ComponentInventory.ComponentState componentState = new ComponentInventory.ComponentState();})
 * for speed. Scala's {@code isSizeInventoryReady} var is
 * {@link #isSizeInventoryReady()} / {@link #setIsSizeInventoryReady(boolean)}, the protected
 * {@code updatingComponents} buffer is {@link #updatingComponents()}.
 */
public interface ComponentInventory extends Inventory, Environment {
    final class ComponentState {
        private static final Map<ComponentInventory, ComponentState> FALLBACK = Collections.synchronizedMap(new WeakHashMap<>());

        private Optional<ManagedEnvironment>[] components;
        private boolean isSizeInventoryReady = true;
        private final List<ManagedEnvironment> updatingComponents = new ArrayList<>();
    }

    default ComponentState componentState() {
        return ComponentState.FALLBACK.computeIfAbsent(this, k -> new ComponentState());
    }

    EnvironmentHost host();

    @SuppressWarnings("unchecked")
    default Optional<ManagedEnvironment>[] components() {
        ComponentState state = componentState();
        if (state.components == null && state.isSizeInventoryReady) {
            Optional<ManagedEnvironment>[] array = new Optional[getContainerSize()];
            Arrays.fill(array, Optional.empty());
            state.components = array;
        }
        if (state.components == null) return new Optional[0];
        return state.components;
    }

    default boolean isSizeInventoryReady() {
        return componentState().isSizeInventoryReady;
    }

    default void setIsSizeInventoryReady(boolean value) {
        componentState().isSizeInventoryReady = value;
    }

    default List<ManagedEnvironment> updatingComponents() {
        return componentState().updatingComponents;
    }

    // ----------------------------------------------------------------------- //

    default void updateComponents() {
        List<ManagedEnvironment> updatingComponents = updatingComponents();
        if (!updatingComponents.isEmpty()) {
            // Iterate by index, since the list may change during iteration (e.g. because
            // a component removed itself / another component, such as the self-
            // destruct card from Computronics).
            int i = 0;
            while (i < updatingComponents.size()) {
                updatingComponents.get(i).update();
                i += 1;
            }
        }
    }

    // ----------------------------------------------------------------------- //

    default void connectComponents() {
        Optional<ManagedEnvironment>[] components = components();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (slot >= components.length) continue;
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && components[slot].isEmpty() && isComponentSlot(slot, stack)) {
                Optional<ManagedEnvironment> result = Optional.empty();
                DriverItem driver = Driver.driverFor(stack);
                if (driver != null) {
                    ManagedEnvironment component = driver.createEnvironment(stack, host());
                    if (component != null) {
                        applyLifecycleState(component, Lifecycle.LifecycleState.Constructing);
                        try {
                            component.loadData(dataTag(driver, stack));
                        }
                        catch (Throwable e) {
                            OpenComputers.log.warn("An item component of type '" + component.getClass().getName() + "' (provided by driver '" + driver.getClass().getName() + "') threw an error while loading.", e);
                        }
                        if (component.canUpdate()) {
                            assert !updatingComponents().contains(component);
                            updatingComponents().add(component);
                        }
                        result = Optional.of(component);
                    }
                }
                components[slot] = result;
            }
        }
        // Make sure our node is connected.
        Network.joinNewNetwork(node());
        for (Optional<ManagedEnvironment> entry : components) {
            if (entry.isPresent()) {
                ManagedEnvironment component = entry.get();
                applyLifecycleState(component, Lifecycle.LifecycleState.Initializing);
                connectItemNode(component.node());
                applyLifecycleState(component, Lifecycle.LifecycleState.Initialized);
            }
        }
    }

    default void disconnectComponents() {
        for (Optional<ManagedEnvironment> entry : components()) {
            if (entry.isPresent()) {
                ManagedEnvironment component = entry.get();
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposing);
                if (component.node() != null) component.node().remove();
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposed);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    default void saveData(CompoundTag nbt) {
        saveComponents();
        Inventory.super.saveData(nbt); // Save items after updating their tags.
    }

    default void saveComponents() {
        Optional<ManagedEnvironment>[] components = components();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                if (slot >= components.length) {
                    // isSizeInventoryReady was added to resolve issues where an inventory was used before its
                    // nbt data had been parsed. See https://github.com/MightyPirates/OpenComputers/issues/2522
                    // If this error is hit again, perhaps another subtype needs to handle nbt loading like Case does
                    OpenComputers.log.error("ComponentInventory components length " + components.length + " does not accommodate inventory size " + getContainerSize());
                    return;
                }
                else if (components[slot].isPresent()) {
                    // We're guaranteed to have a driver for entries.
                    save(components[slot].get(), Driver.driverFor(stack), stack);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default void onItemAdded(int slot, ItemStack stack) {
        Optional<ManagedEnvironment>[] components = components();
        if (slot >= 0 && slot < components.length && isComponentSlot(slot, stack)) {
            DriverItem driver = Driver.driverFor(stack);
            if (driver == null) return;
            ManagedEnvironment component = driver.createEnvironment(stack, host());
            if (component == null) return; // No environment (e.g. RAM).
            synchronized (this) {
                components[slot] = Optional.of(component);
                applyLifecycleState(component, Lifecycle.LifecycleState.Constructing);
                try {
                    component.loadData(dataTag(driver, stack));
                }
                catch (Throwable e) {
                    OpenComputers.log.warn("An item component of type '" + component.getClass().getName() + "' (provided by driver '" + driver.getClass().getName() + "') threw an error while loading.", e);
                }
                if (component.canUpdate()) {
                    assert !updatingComponents().contains(component);
                    updatingComponents().add(component);
                }
                applyLifecycleState(component, Lifecycle.LifecycleState.Initializing);
                connectItemNode(component.node());
                applyLifecycleState(component, Lifecycle.LifecycleState.Initialized);
                save(component, driver, stack);
            }
        }
    }

    @Override
    default void onItemRemoved(int slot, ItemStack stack) {
        Optional<ManagedEnvironment>[] components = components();
        if (slot >= 0 && slot < components.length && components[slot].isPresent()) {
            // Uninstall component previously in that slot.
            ManagedEnvironment component = components[slot].get();
            synchronized (this) {
                // Note to self: we have to remove the node from the network *before*
                // saving, to allow file systems to close their handles before they
                // are saved (otherwise hard drives would restore all handles after
                // being installed into a different computer, even!)
                components[slot] = Optional.empty();
                updatingComponents().remove(component);
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposing);
                if (component.node() != null) component.node().remove();
                DriverItem driver = Driver.driverFor(stack);
                if (driver != null) save(component, driver, stack);
                // However, nodes then may add themselves to a network again, to
                // ensure they have an address that gets sent to the client, used
                // for associating some components with each other. So we do it again.
                // TODO Should be possible to avoid this with lifecycle state now.
                if (component.node() != null) component.node().remove();
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposed);
            }
        }
    }

    default boolean isComponentSlot(int slot, ItemStack stack) {
        return true;
    }

    default void connectItemNode(Node node) {
        if (this.node() != null && node != null) {
            this.node().connect(node);
        }
    }

    default CompoundTag dataTag(DriverItem driver, ItemStack stack) {
        CompoundTag tag = driver.dataTag(stack);
        return tag != null ? tag : Item.dataTagStatic(stack);
    }

    default void save(ManagedEnvironment component, DriverItem driver, ItemStack stack) {
        try {
            CompoundTag tag = dataTag(driver, stack);
            // Clear the tag compound before saving to get the same behavior as
            // in tile entities (otherwise entries have to be cleared manually).
            for (String key : new ArrayList<>(tag.getAllKeys())) {
                tag.remove(key);
            }
            component.saveData(tag);
        }
        catch (Throwable e) {
            OpenComputers.log.warn("An item component of type '" + component.getClass().getName() + "' (provided by driver '" + driver.getClass().getName() + "') threw an error while saving.", e);
        }
    }

    default void applyLifecycleState(Object component, Lifecycle.LifecycleState state) {
        if (component instanceof Lifecycle lifecycle) {
            lifecycle.onLifecycleStateChange(state);
        }
    }
}
