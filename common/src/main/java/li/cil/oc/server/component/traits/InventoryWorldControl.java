package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.platform.ComponentPlatform;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static li.cil.oc.util.ResultWrapper.result;

public interface InventoryWorldControl extends InventoryAware, WorldAware, SideRestricted {
    // Declared abstractly by InventoryAware and as a default by WorldAware; resolve the clash.
    @Override
    default net.minecraft.world.entity.player.Player fakePlayer() {
        return WorldAware.super.fakePlayer();
    }

    @Callback(doc = "function(side:number):boolean -- Compare the block on the specified side with the one in the selected slot. Returns true if equal.")
    default Object[] compare(Context context, Arguments args) {
        final Direction side = checkSideForAction(args, 0);
        final ItemStack stack = stackInSlot(selectedSlot());
        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem item) {
            final BlockPos blockPos = position().offset(side).toBlockPos();
            final BlockState state = world().getBlockState(blockPos);
            final boolean idMatches = item.getBlock() == state.getBlock();
            args.optBoolean(1, false); // TODO
            return result(idMatches);
        }
        return result(false);
    }

    @Callback(doc = "function(side:number[, count:number=64]):boolean -- Drops items from the selected slot towards the specified side.")
    default Object[] drop(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final int count = ExtendedArguments.optItemCount(args, 1);
        final ItemStack stack = inventory().getItem(selectedSlot());
        if (!stack.isEmpty() && stack.getCount() > 0) {
            final BlockPosition blockPos = position().offset(facing);
            final Optional<ItemHandler> target = InventoryUtils.inventoryAt(blockPos, facing.getOpposite());
            if (target.isPresent() && mayInteract(blockPos, facing.getOpposite(), target.get())) {
                if (!InventoryUtils.insertIntoInventory(stack, target.get(), count)) {
                    // Cannot drop into that inventory.
                    return result(false, "inventory full");
                } else if (stack.getCount() == 0) {
                    // Dropped whole stack.
                    inventory().setItem(selectedSlot(), ItemStack.EMPTY);
                } else {
                    // Dropped partial stack.
                    inventory().setChanged();
                }
            } else {
                // No inventory to drop into, drop into the world.
                final ItemStack dropped = inventory().removeItem(selectedSlot(), count);
                final Predicate<ItemEntity> validator = item -> ComponentPlatform.canTossItem(item, fakePlayer());
                if (!dropped.isEmpty()) {
                    if (InventoryUtils.spawnStackInWorld(position(), dropped, Optional.of(facing), Optional.of(validator)) == null)
                        fakePlayer().getInventory().add(dropped);
                }
            }

            context.pause(Settings.get().dropDelay);

            return result(true);
        } else return result(false);
    }

    /**
     * @param facing items to suck from
     * @return the number of items sucked
     */
    default int suckFromItems(Direction facing) {
        for (ItemEntity entity : suckableItems(facing)) {
            if (entity.isAlive() && !entity.hasPickUpDelay()) {
                final ItemStack stack = entity.getItem();
                final int size = stack.getCount();
                onSuckCollect(entity);
                if (stack.getCount() < size)
                    return size - stack.getCount();
                else if (!entity.isAlive())
                    return size;
            }
        }
        return 0;
    }

    @Callback(doc = "function(side:number[, count:number=64]):boolean -- Suck up items from the specified side.")
    default Object[] suck(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final int count = ExtendedArguments.optItemCount(args, 1);

        final BlockPosition blockPos = position().offset(facing);
        int extracted = 0;
        final Optional<ItemHandler> source = InventoryUtils.inventoryAt(blockPos, facing.getOpposite());
        if (source.isPresent()) {
            // Note: the original evaluated mayInteract here but ignored its result; kept as is.
            mayInteract(blockPos, facing.getOpposite());
            final ItemHandler own = InventoryUtils.asItemHandler(this.inventory());
            final List<Integer> slots = insertionSlots();
            extracted = InventoryUtils.extractAnyFromInventory(s -> InventoryUtils.insertIntoInventory(s, own, 64, false, Optional.of(slots)), source.get(), count);
        }
        if (extracted <= 0) {
            extracted = suckFromItems(facing);
        }
        if (extracted <= 0) {
            return result(false);
        } else {
            context.pause(Settings.get().suckDelay);
            return result(extracted);
        }
    }

    default List<ItemEntity> suckableItems(Direction side) {
        return entitiesOnSide(ItemEntity.class, side);
    }

    default void onSuckCollect(ItemEntity entity) {
        entity.playerTouch(fakePlayer());
    }
}
