package li.cil.oc.client.gui.traits;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container screens implementing this must override {@code slotClicked} and only
 * call {@code super.slotClicked} if {@link #isSlotClickAllowed(Slot)} returns true.
 */
public interface LockedHotbar {
    ItemStack lockedStack();

    default boolean isSlotClickAllowed(Slot slot) {
        return slot == null || !ItemStack.isSameItem(slot.getItem(), lockedStack());
    }
}
