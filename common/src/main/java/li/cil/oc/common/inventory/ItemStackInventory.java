package li.cil.oc.common.inventory;

import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Inventory backed by an item stack's tag.
 * <p>
 * Stateful Scala trait turned interface: the item array lives in an {@link ItemsHolder}. The default
 * {@link #items()} keeps the holder in a weak map keyed by the inventory; implementers may override
 * {@code items()} as {@code return itemsHolder.get(this);} with a field holder for speed. The holder
 * lazily creates the item array and loads it from the container stack (the Scala trait did that on
 * construction).
 */
public interface ItemStackInventory extends Inventory {
    // The item stack that provides the inventory.
    ItemStack container();

    @Override
    default ItemStack[] items() {
        return ItemsHolder.FALLBACK.computeIfAbsent(this, k -> new ItemsHolder()).get(this);
    }

    // Load items from tag.
    default void reinitialize() {
        for (int i = 0; i < items().length; i++) {
            updateItems(i, ItemStack.EMPTY);
        }
        loadData(container().getOrCreateTag());
    }

    // Write items back to tag.
    @Override
    default void setChanged() {
        saveData(container().getOrCreateTag());
    }

    final class ItemsHolder {
        private static final Map<ItemStackInventory, ItemsHolder> FALLBACK = Collections.synchronizedMap(new WeakHashMap<>());

        private ItemStack[] items;

        public ItemStack[] get(ItemStackInventory inventory) {
            if (items == null) {
                ItemStack[] array = new ItemStack[inventory.getContainerSize()];
                Arrays.fill(array, ItemStack.EMPTY);
                items = array;
                // Initialize the list automatically if we have a container.
                ItemStack container = inventory.container();
                if (container != null && !container.isEmpty()) {
                    inventory.reinitialize();
                }
            }
            return items;
        }
    }
}
