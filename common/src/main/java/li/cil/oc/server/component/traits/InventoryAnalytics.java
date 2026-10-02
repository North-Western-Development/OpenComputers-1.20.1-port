package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.stream.Collectors;

import static li.cil.oc.util.ResultWrapper.result;

public interface InventoryAnalytics extends InventoryAware, NetworkAware {
    @Callback(doc = "function([slot:number]):table -- Get a description of the stack in the specified slot or the selected slot.")
    default Object[] getStackInInternalSlot(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            final int slot = optSlot(args, 0);
            return result(inventory().getItem(slot));
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function(otherSlot:number):boolean -- Get whether the stack in the selected slot is equivalent to the item in the specified slot (have shared OreDictionary IDs).")
    default Object[] isEquivalentTo(Context context, Arguments args) {
        final int slot = ExtendedArguments.checkSlot(args, inventory(), 0);
        final ItemStack stackA = stackInSlot(selectedSlot());
        final ItemStack stackB = stackInSlot(slot);
        if (!stackA.isEmpty() && !stackB.isEmpty()) {
            return result(haveSharedTags(stackA, stackB));
        } else return result(stackA.isEmpty() && stackB.isEmpty());
    }

    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.")
    default Object[] storeInternal(Context context, Arguments args) {
        final int localSlot = ExtendedArguments.checkSlot(args, inventory(), 0);
        final String dbAddress = args.checkString(1);
        final ItemStack localStack = inventory().getItem(localSlot);
        return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
            final int dbSlot = ExtendedArguments.checkSlot(args, database.data, 2);
            final boolean nonEmpty = database.getStackInSlot(dbSlot) != ItemStack.EMPTY; // zero size stacks!
            database.setStackInSlot(dbSlot, localStack.copy());
            return result(nonEmpty);
        });
    }

    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified slot with one in the database with the specified address.")
    default Object[] compareToDatabase(Context context, Arguments args) {
        final int localSlot = ExtendedArguments.checkSlot(args, inventory(), 0);
        final String dbAddress = args.checkString(1);
        final ItemStack localStack = inventory().getItem(localSlot);
        return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
            final int dbSlot = ExtendedArguments.checkSlot(args, database.data, 2);
            final ItemStack dbStack = database.getStackInSlot(dbSlot);
            return result(InventoryUtils.haveSameItemType(localStack, dbStack, args.optBoolean(3, false)));
        });
    }

    /** Whether the items of the two stacks share at least one item tag (formerly: Forge item tags). */
    static boolean haveSharedTags(ItemStack stackA, ItemStack stackB) {
        final Set<TagKey<Item>> tagsA = stackA.getTags().collect(Collectors.toSet());
        return stackB.getTags().anyMatch(tagsA::contains);
    }
}
