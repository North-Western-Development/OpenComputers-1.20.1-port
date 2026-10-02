package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.util.ExtendedArguments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface InventoryAware {
    Player fakePlayer();

    Container inventory();

    int selectedSlot();

    void setSelectedSlot(int value);

    default List<Integer> insertionSlots() {
        final List<Integer> slots = new ArrayList<>();
        final int size = inventory().getContainerSize();
        for (int i = selectedSlot(); i < size; i++) slots.add(i);
        for (int i = 0; i < selectedSlot(); i++) slots.add(i);
        return slots;
    }

    // ----------------------------------------------------------------------- //

    default int optSlot(Arguments args, int n) {
        // Note: like the original, this always checks argument 0.
        if (args.count() > 0 && args.checkAny(0) != null) return ExtendedArguments.checkSlot(args, inventory(), 0);
        else return selectedSlot();
    }

    /** Formerly returned a StackOption; now the stack, {@link ItemStack#EMPTY} meaning none. */
    default ItemStack stackInSlot(int slot) {
        final ItemStack stack = inventory().getItem(slot);
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
