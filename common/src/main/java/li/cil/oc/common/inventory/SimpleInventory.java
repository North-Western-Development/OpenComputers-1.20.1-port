package li.cil.oc.common.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface SimpleInventory extends Container, Nameable {
    @Override
    default boolean hasCustomName() {
        return false;
    }

    @Override
    default Component getDisplayName() {
        return getName();
    }

    @Override
    default int getMaxStackSize() {
        return 64;
    }

    // Items required in a slot before it's set to null (for ghost stacks).
    default int getInventoryStackRequired() {
        return 1;
    }

    @Override
    default void startOpen(Player player) {
    }

    @Override
    default void stopOpen(Player player) {
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < getContainerSize()) {
            ItemStack stack = getItem(slot);
            ItemStack result;
            if (stack.getCount() - amount < getInventoryStackRequired()) {
                setItem(slot, ItemStack.EMPTY);
                result = stack;
            }
            else {
                result = stack.split(amount);
                setChanged();
            }
            return result.getCount() > 0 ? result : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < getContainerSize()) {
            ItemStack stack = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    default void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }
}
