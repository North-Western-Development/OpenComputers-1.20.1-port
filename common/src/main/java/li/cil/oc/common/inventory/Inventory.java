package li.cil.oc.common.inventory;

import li.cil.oc.Settings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Stateful Scala trait turned interface: implementers provide the backing {@link #items()} array.
 */
public interface Inventory extends SimpleInventory {
    String ItemsTag = Settings.namespace + "items";
    String SlotTag = "slot";
    String ItemTag = "item";

    ItemStack[] items();

    default void updateItems(int slot, ItemStack stack) {
        items()[slot] = stack == null ? ItemStack.EMPTY : stack;
    }

    // ----------------------------------------------------------------------- //

    @Override
    default ItemStack getItem(int slot) {
        if (slot >= 0 && slot < getContainerSize()) return items()[slot];
        return ItemStack.EMPTY;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < getContainerSize()) {
            if (stack.isEmpty() && items()[slot].isEmpty()) {
                return;
            }
            if (items()[slot] == stack) {
                return;
            }

            ItemStack oldStack = items()[slot];
            updateItems(slot, ItemStack.EMPTY);
            if (!oldStack.isEmpty()) {
                onItemRemoved(slot, oldStack);
            }
            if (!stack.isEmpty() && stack.getCount() >= getInventoryStackRequired()) {
                if (stack.getCount() > getMaxStackSize()) {
                    stack.setCount(getMaxStackSize());
                }
                updateItems(slot, stack);
            }

            if (!items()[slot].isEmpty()) {
                onItemAdded(slot, items()[slot]);
            }

            setChanged();
        }
    }

    @Override
    default Component getName() {
        return Component.translatable(Settings.namespace + "container." + inventoryName());
    }

    default String inventoryName() {
        return getClass().getSimpleName().toLowerCase();
    }

    @Override
    default boolean isEmpty() {
        for (ItemStack stack : items()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    // ----------------------------------------------------------------------- //

    default void loadData(CompoundTag nbt) {
        ListTag list = nbt.getList(ItemsTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (tag.contains(SlotTag)) {
                int slot = tag.getByte(SlotTag);
                if (slot >= 0 && slot < items().length) {
                    updateItems(slot, ItemStack.of(tag.getCompound(ItemTag)));
                }
            }
        }
    }

    default void saveData(CompoundTag nbt) {
        ListTag list = new ListTag();
        ItemStack[] items = items();
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack stack = items[slot];
            if (!stack.isEmpty()) {
                CompoundTag slotNbt = new CompoundTag();
                slotNbt.putByte(SlotTag, (byte) slot);
                slotNbt.put(ItemTag, stack.save(new CompoundTag()));
                list.add(slotNbt);
            }
        }
        nbt.put(ItemsTag, list);
    }

    // ----------------------------------------------------------------------- //

    default void onItemAdded(int slot, ItemStack stack) {
    }

    default void onItemRemoved(int slot, ItemStack stack) {
    }
}
