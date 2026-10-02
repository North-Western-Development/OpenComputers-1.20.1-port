package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static li.cil.oc.util.ResultWrapper.result;

public interface InventoryWorldControlMk2 extends InventoryAware, WorldAware, SideRestricted {
    @Callback(doc = "function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Drops the selected item stack into the specified slot of an inventory.")
    default Object[] dropIntoSlot(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final int count = ExtendedArguments.optItemCount(args, 2);
        final Direction fromSide = ExtendedArguments.optSideAny(args, 3, facing.getOpposite());
        final ItemStack stack = inventory().getItem(selectedSlot());
        if (!stack.isEmpty() && stack.getCount() > 0) {
            return withInventory(position().offset(facing), fromSide, inventory -> {
                final int slot = ExtendedArguments.checkSlot(args, inventory, 1);
                if (!InventoryUtils.insertIntoInventorySlot(stack, inventory, slot, count)) {
                    // Cannot drop into that inventory.
                    return result(false, "inventory full/invalid slot");
                } else if (stack.getCount() == 0) {
                    // Dropped whole stack.
                    this.inventory().setItem(selectedSlot(), ItemStack.EMPTY);
                } else {
                    // Dropped partial stack.
                    this.inventory().setChanged();
                }

                context.pause(Settings.get().dropDelay);

                return result(true);
            });
        } else return result(false);
    }

    @Callback(doc = "function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Sucks items from the specified slot of an inventory.")
    default Object[] suckFromSlot(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final int count = ExtendedArguments.optItemCount(args, 2);
        final Direction fromSide = ExtendedArguments.optSideAny(args, 3, facing.getOpposite());
        return withInventory(position().offset(facing), fromSide, inventory -> {
            final int slot = ExtendedArguments.checkSlot(args, inventory, 1);
            final ItemHandler own = InventoryUtils.asItemHandler(this.inventory());
            final List<Integer> slots = insertionSlots();
            final int extracted = InventoryUtils.extractFromInventorySlot(s -> InventoryUtils.insertIntoInventory(s, own, 64, false, Optional.of(slots)), inventory, slot, count);
            if (extracted > 0) {
                context.pause(Settings.get().suckDelay);
                return result(extracted);
            } else return result(false);
        });
    }

    private Object[] withInventory(BlockPosition blockPos, Direction fromSide, Function<ItemHandler, Object[]> f) {
        final Optional<ItemHandler> inventory = InventoryUtils.inventoryAt(blockPos, fromSide);
        if (inventory.isPresent() && mayInteract(blockPos, fromSide)) return f.apply(inventory.get());
        else return result(null, "no inventory");
    }
}
