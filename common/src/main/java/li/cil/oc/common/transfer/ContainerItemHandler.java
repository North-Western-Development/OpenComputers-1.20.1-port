package li.cil.oc.common.transfer;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * {@link ItemHandler} view of a vanilla {@link Container}, honouring the slot
 * restrictions of {@link WorldlyContainer} when a side is given. Equivalent to
 * Forge's {@code InvWrapper} / {@code SidedInvWrapper}; usable on both loaders.
 */
public class ContainerItemHandler implements ItemHandler {
    protected final Container container;
    @Nullable
    protected final Direction side;
    private final int[] slots;

    public ContainerItemHandler(Container container) {
        this(container, null);
    }

    public ContainerItemHandler(Container container, @Nullable Direction side) {
        this.container = container;
        this.side = side;
        if (side != null && container instanceof WorldlyContainer worldly) {
            this.slots = worldly.getSlotsForFace(side);
        } else {
            this.slots = null;
        }
    }

    public Container getContainer() {
        return container;
    }

    private int toContainerSlot(int slot) {
        return slots == null ? slot : slots[slot];
    }

    @Override
    public int getSlots() {
        return slots == null ? container.getContainerSize() : slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
        return container.getItem(toContainerSlot(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= getSlots()) return stack;
        final int realSlot = toContainerSlot(slot);
        if (!container.canPlaceItem(realSlot, stack)) return stack;
        if (side != null && container instanceof WorldlyContainer worldly && !worldly.canPlaceItemThroughFace(realSlot, stack, side)) {
            return stack;
        }

        final ItemStack existing = container.getItem(realSlot);
        final int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, stack)) return stack;
            final int space = limit - existing.getCount();
            if (space <= 0) return stack;
            final int moved = Math.min(space, stack.getCount());
            if (!simulate) {
                final ItemStack merged = existing.copy();
                merged.grow(moved);
                container.setItem(realSlot, merged);
                container.setChanged();
            }
            final ItemStack remainder = stack.copy();
            remainder.shrink(moved);
            return remainder;
        } else {
            final int moved = Math.min(limit, stack.getCount());
            if (!simulate) {
                final ItemStack placed = stack.copy();
                placed.setCount(moved);
                container.setItem(realSlot, placed);
                container.setChanged();
            }
            final ItemStack remainder = stack.copy();
            remainder.shrink(moved);
            return remainder;
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
        final int realSlot = toContainerSlot(slot);
        final ItemStack existing = container.getItem(realSlot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        if (side != null && container instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(realSlot, existing, side)) {
            return ItemStack.EMPTY;
        }
        final int moved = Math.min(amount, existing.getCount());
        if (simulate) {
            final ItemStack result = existing.copy();
            result.setCount(moved);
            return result;
        }
        final ItemStack result = container.removeItem(realSlot, moved);
        container.setChanged();
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return container.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < getSlots() && container.canPlaceItem(toContainerSlot(slot), stack);
    }
}
