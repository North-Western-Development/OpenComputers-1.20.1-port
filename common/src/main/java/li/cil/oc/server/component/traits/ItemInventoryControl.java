package li.cil.oc.server.component.traits;

import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static li.cil.oc.util.ResultWrapper.result;

public interface ItemInventoryControl extends InventoryAware {
    @Callback(doc = "function(slot:number):number -- The size of an item inventory in the specified slot.")
    default Object[] getItemInventorySize(Context context, Arguments args) {
        return withItemInventory(ExtendedArguments.checkSlot(args, inventory(), 0), itemInventory -> result(itemInventory.getSlots()));
    }

    @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Drops an item from the selected slot into the specified slot in the item inventory.")
    default Object[] dropIntoItemInventory(Context context, Arguments args) {
        return withItemInventory(ExtendedArguments.checkSlot(args, inventory(), 0), itemInventory -> {
            final int slot = ExtendedArguments.checkSlot(args, itemInventory, 1);
            final int count = ExtendedArguments.optItemCount(args, 2);
            return result(InventoryUtils.extractFromInventorySlot((s, sim) -> InventoryUtils.insertIntoInventorySlot(s, itemInventory, slot, 64, sim), inventory(), null, selectedSlot(), count));
        });
    }

    @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Sucks an item out of the specified slot in the item inventory.")
    default Object[] suckFromItemInventory(Context context, Arguments args) {
        return withItemInventory(ExtendedArguments.checkSlot(args, inventory(), 0), itemInventory -> {
            final int slot = ExtendedArguments.checkSlot(args, itemInventory, 1);
            final int count = ExtendedArguments.optItemCount(args, 2);
            final ItemHandler own = InventoryUtils.asItemHandler(inventory());
            final List<Integer> slots = insertionSlots();
            return result(InventoryUtils.extractFromInventorySlot((s, sim) -> InventoryUtils.insertIntoInventory(s, own, 64, sim, Optional.of(slots)), itemInventory, slot, count));
        });
    }

    private Object[] withItemInventory(int slot, Function<ItemHandler, Object[]> f) {
        final ItemStack stack = inventory().getItem(slot);
        if (stack != null) {
            final ItemHandler itemInventory = Driver.itemHandlerFor(stack, fakePlayer());
            if (itemInventory != null) return f.apply(itemInventory);
        }
        return result(0, "no item inventory");
    }
}
