package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.ItemStackArrayValue;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.InventorySource;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.function.Function;

import static li.cil.oc.util.ResultWrapper.result;

public interface WorldInventoryAnalytics extends WorldAware, SideRestricted, NetworkAware {
    @Callback(doc = "function(side:number):number -- Get the number of slots in the inventory on the specified side of the device.")
    default Object[] getInventorySize(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> result(inventory.getSlots()));
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get number of items in the specified slot of the inventory on the specified side of the device.")
    default Object[] getSlotStackSize(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            final ItemStack stack = inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 1));
            return result(stack == null || stack.isEmpty() ? 0 : stack.getCount());
        });
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the maximum number of items in the specified slot of the inventory on the specified side of the device.")
    default Object[] getSlotMaxStackSize(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            final ItemStack stack = inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 1));
            return result(stack == null || stack.isEmpty() ? 0 : stack.getMaxStackSize());
        });
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number[, checkNBT:boolean=false]):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the device are of the same type.")
    default Object[] compareStacks(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            final ItemStack stackA = inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 1));
            final ItemStack stackB = inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 2));
            return result(stackA == stackB || InventoryUtils.haveSameItemType(stackA, stackB, args.optBoolean(3, false)));
        });
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified slot in the inventory on the specified side with one in the database with the specified address.")
    default Object[] compareStackToDatabase(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            final int slot = ExtendedArguments.checkSlot(args, inventory, 1);
            final String dbAddress = args.checkString(2);
            final ItemStack stack = inventory.getStackInSlot(slot);
            return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
                final int dbSlot = ExtendedArguments.checkSlot(args, database.data, 3);
                final ItemStack dbStack = database.getStackInSlot(dbSlot);
                return result(InventoryUtils.haveSameItemType(stack, dbStack, args.optBoolean(4, false)));
            });
        });
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the device are equivalent (have shared OreDictionary IDs).")
    default Object[] areStacksEquivalent(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            final ItemStack stackA = inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 1));
            final ItemStack stackB = inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 2));
            return result(stackA == stackB ||
                    (!stackA.isEmpty() && !stackB.isEmpty() &&
                            InventoryAnalytics.haveSharedTags(stackA, stackB)));
        });
    }

    @Callback(doc = "function(side:number, slot:number):table -- Get a description of the stack in the inventory on the specified side of the device.")
    default Object[] getStackInSlot(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            final Direction facing = checkSideForAction(args, 0);
            return withInventory(facing, inventory -> result(inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 1))));
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number):userdata -- Get a description of all stacks in the inventory on the specified side of the device.")
    default Object[] getAllStacks(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            final Direction facing = checkSideForAction(args, 0);
            return withInventory(facing, inventory -> {
                final ItemStack[] stacks = new ItemStack[inventory.getSlots()];
                for (int i = 0; i < inventory.getSlots(); i++) {
                    stacks[i] = inventory.getStackInSlot(i);
                }
                return result(new ItemStackArrayValue(stacks));
            });
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number):string -- Get the the name of the inventory on the specified side of the device.")
    default Object[] getInventoryName(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            final Direction facing = checkSideForAction(args, 0);
            return withInventorySource(facing, source -> {
                if (source instanceof InventorySource.Block blockSource) {
                    final Optional<Block> block = blockAt(blockSource.position());
                    if (block.isPresent()) return result(BuiltInRegistries.BLOCK.getKey(block.get()).toString());
                } else if (source instanceof InventorySource.Entity entitySource) {
                    return result(BuiltInRegistries.ENTITY_TYPE.getKey(entitySource.entity().getType()).toString());
                }
                return result(null, "Unknown");
            });
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.")
    default Object[] store(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final String dbAddress = args.checkString(2);
        return withInventory(facing, inventory -> {
            final ItemStack stack = inventory.getStackInSlot(ExtendedArguments.checkSlot(args, inventory, 1));
            return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
                final int dbSlot = ExtendedArguments.checkSlot(args, database.data, 3);
                final boolean nonEmpty = database.getStackInSlot(dbSlot) != ItemStack.EMPTY; // zero size stacks
                database.setStackInSlot(dbSlot, stack.copy());
                return result(nonEmpty);
            });
        });
    }

    private Optional<Block> blockAt(BlockPosition position) {
        if (position.world.isPresent()) {
            final Level world = position.world.get();
            if (ExtendedWorld.blockExists(world, position)) {
                return Optional.ofNullable(ExtendedWorld.getBlock(world, position));
            }
        }
        return Optional.empty();
    }

    private Object[] withInventorySource(Direction side, Function<InventorySource, Object[]> f) {
        final Optional<InventorySource> source = InventoryUtils.inventorySourceAt(position().offset(side), side.getOpposite());
        if (source.isPresent() && mayInteract(source.get())) return f.apply(source.get());
        else return result(null, "no inventory");
    }

    private Object[] withInventory(Direction side, Function<ItemHandler, Object[]> f) {
        return withInventorySource(side, source -> f.apply(source.inventory()));
    }
}
