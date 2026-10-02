package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.world.item.ItemStack;

import static li.cil.oc.util.ResultWrapper.result;

public interface InventoryControl extends InventoryAware {
    @Callback(doc = "function():number -- The size of this device's internal inventory.")
    default Object[] inventorySize(Context context, Arguments args) {
        return result(inventory().getContainerSize());
    }

    @Callback(doc = "function([slot:number]):number -- Get the currently selected slot; set the selected slot if specified.")
    default Object[] select(Context context, Arguments args) {
        final int slot = optSlot(args, 0);
        if (slot != selectedSlot()) {
            setSelectedSlot(slot);
        }
        return result(selectedSlot() + 1);
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Get the number of items in the specified slot, otherwise in the selected slot.")
    default Object[] count(Context context, Arguments args) {
        final int slot = optSlot(args, 0);
        final ItemStack stack = stackInSlot(slot);
        return result(stack.isEmpty() ? 0 : stack.getCount());
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Get the remaining space in the specified slot, otherwise in the selected slot.")
    default Object[] space(Context context, Arguments args) {
        final int slot = optSlot(args, 0);
        final ItemStack stack = stackInSlot(slot);
        if (!stack.isEmpty()) {
            return result(Math.min(inventory().getMaxStackSize(), stack.getMaxStackSize()) - stack.getCount());
        } else return result(inventory().getMaxStackSize());
    }

    @Callback(doc = "function(otherSlot:number[, checkNBT:boolean=false]):boolean -- Compare the contents of the selected slot to the contents of the specified slot.")
    default Object[] compareTo(Context context, Arguments args) {
        final int slot = ExtendedArguments.checkSlot(args, inventory(), 0);
        final ItemStack stackA = stackInSlot(selectedSlot());
        final ItemStack stackB = stackInSlot(slot);
        if (!stackA.isEmpty() && !stackB.isEmpty()) {
            return result(InventoryUtils.haveSameItemType(stackA, stackB, args.optBoolean(1, false)));
        } else return result(stackA.isEmpty() && stackB.isEmpty());
    }

    @Callback(doc = "function(toSlot:number[, amount:number]):boolean -- Move up to the specified amount of items from the selected slot into the specified slot.")
    default Object[] transferTo(Context context, Arguments args) {
        final int slot = ExtendedArguments.checkSlot(args, inventory(), 0);
        final int count = ExtendedArguments.optItemCount(args, 1);
        if (slot == selectedSlot() || count == 0) {
            return result(true);
        }
        final ItemStack from = stackInSlot(selectedSlot());
        final ItemStack to = stackInSlot(slot);
        if (!from.isEmpty() && !to.isEmpty()) {
            if (InventoryUtils.haveSameItemType(from, to, true)) {
                final int space = Math.min(inventory().getMaxStackSize(), to.getMaxStackSize()) - to.getCount();
                final int amount = Math.min(count, Math.min(space, from.getCount()));
                if (amount > 0) {
                    from.shrink(amount);
                    to.grow(amount);
                    assert from.getCount() >= 0;
                    if (from.getCount() == 0) {
                        inventory().setItem(selectedSlot(), ItemStack.EMPTY);
                    }
                    inventory().setChanged();
                    return result(true);
                } else return result(false);
            } else if (count >= from.getCount()) {
                inventory().setItem(slot, from);
                inventory().setItem(selectedSlot(), to);
                return result(true);
            } else return result(false);
        } else if (!from.isEmpty()) {
            inventory().setItem(slot, inventory().removeItem(selectedSlot(), count));
            return result(true);
        } else return result(false);
    }
}
