package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.ContainerItemHandler;
import li.cil.oc.common.transfer.ItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * Inventory helpers working on {@link ItemHandler}s (formerly Forge's {@code IItemHandler}).
 * Default arguments of the Scala version are available as overloads.
 * The Scala type alias {@code Extractor = () => Int} is {@link IntSupplier} now.
 */
public final class InventoryUtils {
    private InventoryUtils() {
    }

    public static ContainerItemHandler asItemHandler(Container inventory, @Nullable Direction side) {
        return new ContainerItemHandler(inventory, side);
    }

    public static ContainerItemHandler asItemHandler(Container inventory) {
        return asItemHandler(inventory, null);
    }

    /**
     * Check if two item stacks are of equal type, ignoring the stack size.
     * <p/>
     * Optionally check for equality in NBT data.
     */
    public static boolean haveSameItemType(ItemStack stackA, ItemStack stackB, boolean checkNBT) {
        return !stackA.isEmpty() && !stackB.isEmpty() &&
                stackA.getItem() == stackB.getItem() &&
                (stackA.getDamageValue() == stackB.getDamageValue()) &&
                (!checkNBT || Objects.equals(stackA.getTag(), stackB.getTag()));
    }

    public static boolean haveSameItemType(ItemStack stackA, ItemStack stackB) {
        return haveSameItemType(stackA, stackB, false);
    }

    /**
     * Retrieves an actual inventory implementation for a specified world coordinate.
     * <p/>
     * This performs special handling for (double-)chests and also checks for
     * mine carts with chests.
     */
    public static Optional<ItemHandler> inventoryAt(BlockPosition position, @Nullable Direction side) {
        if (position.world.isPresent() && ExtendedWorld.blockExists(position.world.get(), position)) {
            final Level world = position.world.get();
            final ItemHandler handler = PlatformHooks.getItemHandler(world, position.toBlockPos(), side);
            if (handler != null) {
                return Optional.of(handler);
            }
            final BlockEntity blockEntity = ExtendedWorld.getBlockEntity(world, position);
            if (blockEntity instanceof Container container) {
                return Optional.of(asItemHandler(container, side));
            }
            for (Entity entity : world.getEntitiesOfClass(Entity.class, position.bounds())) {
                if (entity.isAlive()) {
                    final ItemHandler entityHandler = PlatformHooks.getItemHandler(entity, side);
                    if (entityHandler != null) {
                        return Optional.of(entityHandler);
                    }
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static Optional<ItemHandler> anyInventoryAt(BlockPosition position) {
        final List<Direction> sides = new ArrayList<>();
        sides.add(null);
        sides.addAll(List.of(Direction.values()));
        for (Direction side : sides) {
            final Optional<ItemHandler> inv = inventoryAt(position, side);
            if (inv.isPresent()) {
                return inv;
            }
        }
        return Optional.empty();
    }

    /**
     * Inserts a stack into an inventory.
     * <p/>
     * Only tries to insert into the specified slot. This <em>cannot</em> be
     * used to empty a slot. It can only insert stacks into empty slots and
     * merge additional items into an existing stack in the slot.
     * <p/>
     * The passed stack's size will be adjusted to reflect the number of items
     * inserted into the inventory, i.e. if 10 more items could fit into the
     * slot, the stack's size will be 10 smaller than before the call.
     * <p/>
     * This will return <tt>true</tt> if <em>at least</em> one item could be
     * inserted into the slot. It will return <tt>false</tt> if the passed
     * stack did not change. Note that it will also change the stack
     * when called with <tt>simulate = true</tt>.
     * <p/>
     * This takes care of handling special cases such as sided inventories,
     * maximum inventory and item stack sizes.
     * <p/>
     * The number of items inserted can be limited, to avoid unnecessary
     * changes to the inventory the stack may come from, for example.
     */
    public static boolean insertIntoInventorySlot(ItemStack stack, ItemHandler inventory, int slot, int limit, boolean simulate) {
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0) return false;
        final int amount = Math.min(stack.getCount(), limit);
        final ItemStack toInsert = stack.split(amount);
        final ItemStack remaining = inventory.insertItem(slot, toInsert, simulate);
        if (remaining != null) {
            final boolean result = remaining.getCount() < amount;
            // Note: ItemStack keeps its item when its count drops to zero, so this restores it.
            stack.grow(remaining.getCount());
            return result;
        }
        return true;
    }

    public static boolean insertIntoInventorySlot(ItemStack stack, ItemHandler inventory, int slot, int limit) {
        return insertIntoInventorySlot(stack, inventory, slot, limit, false);
    }

    public static boolean insertIntoInventorySlot(ItemStack stack, ItemHandler inventory, int slot) {
        return insertIntoInventorySlot(stack, inventory, slot, 64, false);
    }

    public static boolean insertIntoInventorySlot(ItemStack stack, Container inventory, Optional<Direction> side, int slot, int limit, boolean simulate) {
        return insertIntoInventorySlot(stack, asItemHandler(inventory, side.orElse(null)), slot, limit, simulate);
    }

    /**
     * Extracts a stack from an inventory.
     * <p/>
     * Only tries to extract from the specified slot. This <em>can</em> be used
     * to empty a slot. It will extract items using the specified consumer method
     * which is called with the extracted stack before the stack in the inventory
     * that we extract from is cleared from. This allows placing back excess
     * items with as few inventory updates as possible.
     * <p/>
     * The consumer is the only way to retrieve the actually extracted stack. It
     * is called with a separate stack instance, so it does not have to be copied
     * again.
     * <p/>
     * This will return the <tt>number</tt> of items extracted. It will return
     * <tt>zero</tt> if the stack in the slot did not change.
     * <p/>
     * This takes care of handling special cases such as sided inventories and
     * maximum stack sizes.
     * <p/>
     * The number of items extracted can be limited, to avoid unnecessary
     * changes to the inventory the stack is extracted from. Note that this could
     * also be achieved by a check in the consumer, but it saves some unnecessary
     * code repetition this way.
     */
    public static int extractFromInventorySlot(Consumer<ItemStack> consumer, ItemHandler inventory, int slot, int limit) {
        final ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0)
            return 0;
        int amount = Math.min(Math.min(stack.getMaxStackSize(), stack.getCount()), limit);
        final ItemStack simExtracted = inventory.extractItem(slot, amount, true);
        if (simExtracted != null) {
            final ItemStack extracted = simExtracted.copy();
            amount = extracted.getCount();
            consumer.accept(extracted);
            final int count = Math.max(amount - extracted.getCount(), 0);
            if (count > 0) {
                final ItemStack realExtracted = inventory.extractItem(slot, count, false);
                if (realExtracted == null || realExtracted.getCount() != count) {
                    OpenComputers.log.warn("Items may have been duplicated during inventory extraction. This means an IItemHandler instance acted differently between simulated and non-simulated extraction. Offender: " + inventory);
                }
            }
            return count;
        }
        return 0;
    }

    public static int extractFromInventorySlot(Consumer<ItemStack> consumer, ItemHandler inventory, int slot) {
        return extractFromInventorySlot(consumer, inventory, slot, 64);
    }

    public static int extractFromInventorySlot(Consumer<ItemStack> consumer, Container inventory, @Nullable Direction side, int slot, int limit) {
        return extractFromInventorySlot(consumer, asItemHandler(inventory, side), slot, limit);
    }

    /**
     * Inserts a stack into an inventory.
     * <p/>
     * This will try to fit the stack in any and as many as necessary slots in
     * the inventory. It will first try to merge the stack in stacks already
     * present in the inventory. After that it will try to fit the stack into
     * empty slots in the inventory.
     * <p/>
     * This uses the <tt>insertIntoInventorySlot</tt> method, and therefore
     * handles special cases such as sided inventories and stack size limits.
     * <p/>
     * This returns <tt>true</tt> if at least one item was inserted. The passed
     * item stack will be adjusted to reflect the number items inserted, by
     * having its size decremented accordingly.
     */
    public static boolean insertIntoInventory(ItemStack stack, ItemHandler inventory, int limit, boolean simulate, Optional<? extends Iterable<Integer>> slots) {
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0) return false;
        boolean success = false;
        int remaining = Math.min(limit, stack.getCount());
        final Iterable<Integer> range;
        if (slots.isPresent()) {
            range = slots.get();
        } else {
            final List<Integer> all = new ArrayList<>();
            for (int i = 0; i < inventory.getSlots(); i++) all.add(i);
            range = all;
        }

        for (int slot : range) {
            final int previousCount = stack.getCount();
            if (remaining > 0 && insertIntoInventorySlot(stack, inventory, slot, remaining, simulate)) {
                remaining -= previousCount - stack.getCount();
                success = true;
            }
            if (remaining <= 0) break;
        }

        return success;
    }

    public static boolean insertIntoInventory(ItemStack stack, ItemHandler inventory, int limit, boolean simulate) {
        return insertIntoInventory(stack, inventory, limit, simulate, Optional.empty());
    }

    public static boolean insertIntoInventory(ItemStack stack, ItemHandler inventory, int limit) {
        return insertIntoInventory(stack, inventory, limit, false, Optional.empty());
    }

    public static boolean insertIntoInventory(ItemStack stack, ItemHandler inventory) {
        return insertIntoInventory(stack, inventory, 64, false, Optional.empty());
    }

    public static boolean insertIntoInventory(ItemStack stack, Container inventory, Optional<Direction> side, int limit, boolean simulate, Optional<? extends Iterable<Integer>> slots) {
        return insertIntoInventory(stack, asItemHandler(inventory, side.orElse(null)), limit, simulate, slots);
    }

    /**
     * Extracts a slot from an inventory.
     * <p/>
     * This will try to extract a stack from any inventory slot. It will iterate
     * all slots until an item can be extracted from a slot.
     * <p/>
     * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
     * handles special cases such as sided inventories and stack size limits.
     * <p/>
     * This returns <tt>true</tt> if at least one item was extracted.
     */
    public static int extractAnyFromInventory(Consumer<ItemStack> consumer, ItemHandler inventory, int limit) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final int extracted = extractFromInventorySlot(consumer, inventory, slot, limit);
            if (extracted > 0)
                return extracted;
        }
        return 0;
    }

    public static int extractAnyFromInventory(Consumer<ItemStack> consumer, ItemHandler inventory) {
        return extractAnyFromInventory(consumer, inventory, 64);
    }

    public static int extractAnyFromInventory(Consumer<ItemStack> consumer, Container inventory, @Nullable Direction side, int limit) {
        return extractAnyFromInventory(consumer, asItemHandler(inventory, side), limit);
    }

    /**
     * Extracts an item stack from an inventory.
     * <p/>
     * This will try to remove items of the same type as the specified item stack
     * up to the number of the stack's size for all slots in the specified inventory.
     * If exact is true, the items colated will also match meta data
     * <p/>
     * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
     * handles special cases such as sided inventories and stack size limits.
     */
    public static ItemStack extractFromInventory(ItemStack stack, ItemHandler inventory, boolean simulate, boolean exact) {
        final ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (remaining.getCount() <= 0) continue;
            extractFromInventorySlot(stackInInv -> {
                if (stackInInv != null && remaining.getItem() == stackInInv.getItem() && (!exact || haveSameItemType(remaining, stackInInv, true))) {
                    final int transferred = Math.min(stackInInv.getCount(), remaining.getCount());
                    remaining.shrink(transferred);
                    if (!simulate) {
                        stackInInv.shrink(transferred);
                    }
                }
            }, inventory, slot, remaining.getCount());
        }
        return remaining;
    }

    public static ItemStack extractFromInventory(ItemStack stack, ItemHandler inventory, boolean simulate) {
        return extractFromInventory(stack, inventory, simulate, true);
    }

    public static ItemStack extractFromInventory(ItemStack stack, ItemHandler inventory) {
        return extractFromInventory(stack, inventory, false, true);
    }

    public static ItemStack extractFromInventory(ItemStack stack, Container inventory, @Nullable Direction side, boolean simulate, boolean exact) {
        return extractFromInventory(stack, asItemHandler(inventory, side), simulate, exact);
    }

    /**
     * Utility method for calling <tt>insertIntoInventory</tt> on an inventory
     * in the world.
     */
    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position, Optional<Direction> side, int limit, boolean simulate) {
        return inventoryAt(position, side.orElse(null)).map(inv -> insertIntoInventory(stack, inv, limit, simulate)).orElse(false);
    }

    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position, Optional<Direction> side, int limit) {
        return insertIntoInventoryAt(stack, position, side, limit, false);
    }

    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position, Optional<Direction> side) {
        return insertIntoInventoryAt(stack, position, side, 64, false);
    }

    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position) {
        return insertIntoInventoryAt(stack, position, Optional.empty(), 64, false);
    }

    /**
     * Utility method for calling <tt>extractFromInventory</tt> on an inventory
     * in the world.
     */
    @Nullable
    public static IntSupplier getExtractorFromInventoryAt(Consumer<ItemStack> consumer, BlockPosition position, @Nullable Direction side, int limit) {
        final Optional<ItemHandler> inventory = inventoryAt(position, side);
        if (inventory.isPresent()) {
            final ItemHandler inv = inventory.get();
            return () -> extractAnyFromInventory(consumer, inv, limit);
        }
        return null;
    }

    @Nullable
    public static IntSupplier getExtractorFromInventoryAt(Consumer<ItemStack> consumer, BlockPosition position, @Nullable Direction side) {
        return getExtractorFromInventoryAt(consumer, position, side, 64);
    }

    /**
     * Transfers some items between two inventories.
     * <p/>
     * This will try to extract up the specified number of items from any inventory,
     * then insert it into the specified sink inventory. If the insertion fails, the
     * items will remain in the source inventory.
     * <p/>
     * This uses the <tt>extractFromInventory</tt> and <tt>insertIntoInventory</tt>
     * methods, and therefore handles special cases such as sided inventories and
     * stack size limits.
     * <p/>
     * This returns <tt>true</tt> if at least one item was transferred.
     */
    public static int transferBetweenInventories(ItemHandler source, ItemHandler sink, int limit) {
        return extractAnyFromInventory(
                stack -> insertIntoInventory(stack, sink, limit), source, limit);
    }

    public static int transferBetweenInventories(ItemHandler source, ItemHandler sink) {
        return transferBetweenInventories(source, sink, 64);
    }

    public static int transferBetweenInventories(Container source, @Nullable Direction sourceSide, Container sink, Optional<Direction> sinkSide, int limit) {
        return transferBetweenInventories(asItemHandler(source, sourceSide), asItemHandler(sink, sinkSide.orElse(null)), limit);
    }

    /**
     * Like <tt>transferBetweenInventories</tt> but moving between specific slots.
     */
    public static int transferBetweenInventoriesSlots(ItemHandler source, int sourceSlot, ItemHandler sink, Optional<Integer> sinkSlot, int limit) {
        if (sinkSlot.isPresent()) {
            final int explicitSinkSlot = sinkSlot.get();
            return extractFromInventorySlot(
                    stack -> insertIntoInventorySlot(stack, sink, explicitSinkSlot, limit), source, sourceSlot, limit);
        } else {
            return extractFromInventorySlot(
                    stack -> insertIntoInventory(stack, sink, limit), source, sourceSlot, limit);
        }
    }

    public static int transferBetweenInventoriesSlots(ItemHandler source, int sourceSlot, ItemHandler sink, Optional<Integer> sinkSlot) {
        return transferBetweenInventoriesSlots(source, sourceSlot, sink, sinkSlot, 64);
    }

    public static int transferBetweenInventoriesSlots(Container source, @Nullable Direction sourceSide, int sourceSlot, Container sink, Optional<Direction> sinkSide, Optional<Integer> sinkSlot, int limit) {
        return transferBetweenInventoriesSlots(asItemHandler(source, sourceSide), sourceSlot, asItemHandler(sink, sinkSide.orElse(null)), sinkSlot, limit);
    }

    /**
     * Utility method for calling <tt>transferBetweenInventories</tt> on inventories
     * in the world.
     */
    @Nullable
    public static IntSupplier getTransferBetweenInventoriesAt(BlockPosition source, @Nullable Direction sourceSide, BlockPosition sink, Optional<Direction> sinkSide, int limit) {
        final Optional<ItemHandler> sourceInventory = inventoryAt(source, sourceSide);
        if (sourceInventory.isPresent()) {
            final Optional<ItemHandler> sinkInventory = inventoryAt(sink, sinkSide.orElse(null));
            if (sinkInventory.isPresent()) {
                final ItemHandler src = sourceInventory.get();
                final ItemHandler dst = sinkInventory.get();
                return () -> transferBetweenInventories(src, dst, limit);
            }
        }
        return null;
    }

    @Nullable
    public static IntSupplier getTransferBetweenInventoriesAt(BlockPosition source, @Nullable Direction sourceSide, BlockPosition sink, Optional<Direction> sinkSide) {
        return getTransferBetweenInventoriesAt(source, sourceSide, sink, sinkSide, 64);
    }

    /**
     * Utility method for calling <tt>transferBetweenInventoriesSlots</tt> on inventories
     * in the world.
     */
    @Nullable
    public static IntSupplier getTransferBetweenInventoriesSlotsAt(BlockPosition sourcePos, @Nullable Direction sourceSide, int sourceSlot, BlockPosition sinkPos, Optional<Direction> sinkSide, Optional<Integer> sinkSlot, int limit) {
        final Optional<ItemHandler> sourceInventory = inventoryAt(sourcePos, sourceSide);
        if (sourceInventory.isPresent()) {
            final Optional<ItemHandler> sinkInventory = inventoryAt(sinkPos, sinkSide.orElse(null));
            if (sinkInventory.isPresent()) {
                final ItemHandler src = sourceInventory.get();
                final ItemHandler dst = sinkInventory.get();
                return () -> transferBetweenInventoriesSlots(src, sourceSlot, dst, sinkSlot, limit);
            }
        }
        return null;
    }

    @Nullable
    public static IntSupplier getTransferBetweenInventoriesSlotsAt(BlockPosition sourcePos, @Nullable Direction sourceSide, int sourceSlot, BlockPosition sinkPos, Optional<Direction> sinkSide, Optional<Integer> sinkSlot) {
        return getTransferBetweenInventoriesSlotsAt(sourcePos, sourceSide, sourceSlot, sinkPos, sinkSide, sinkSlot, 64);
    }

    /**
     * Utility method mirroring dropAllSlots but instead piping slots into
     * a provided consumer for use with LootContext.
     */
    public static void forAllSlots(Container inventory, Consumer<ItemStack> dst) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && stack.getCount() > 0) {
                dst.accept(stack);
            }
        }
    }

    /**
     * Utility method for dropping contents from a single inventory slot into
     * the world.
     */
    public static boolean dropSlot(BlockPosition position, Container inventory, int slot, int count, Optional<Direction> direction) {
        final ItemStack stack = inventory.removeItem(slot, count);
        if (stack != null && !stack.isEmpty() && stack.getCount() > 0) {
            spawnStackInWorld(position, stack, direction);
            return true;
        }
        return false;
    }

    public static boolean dropSlot(BlockPosition position, Container inventory, int slot, int count) {
        return dropSlot(position, inventory, slot, count, Optional.empty());
    }

    /**
     * Utility method for dumping all inventory contents into the world.
     */
    public static void dropAllSlots(BlockPosition position, Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && stack.getCount() > 0) {
                inventory.setItem(slot, ItemStack.EMPTY);
                spawnStackInWorld(position, stack);
            }
        }
    }

    /**
     * Try inserting an item stack into a player inventory. If that fails, drop it into the world.
     */
    public static void addToPlayerInventory(ItemStack stack, Player player, boolean spawnInWorld) {
        if (!stack.isEmpty()) {
            if (player.getInventory().add(stack)) {
                player.getInventory().setChanged();
                if (player.containerMenu != null) {
                    player.containerMenu.broadcastChanges();
                }
            }
            if (stack.getCount() > 0 && spawnInWorld) {
                player.drop(stack, false, false);
            }
        }
    }

    public static void addToPlayerInventory(ItemStack stack, Player player) {
        addToPlayerInventory(stack, player, true);
    }

    /**
     * Utility method for spawning an item stack in the world.
     */
    @Nullable
    public static ItemEntity spawnStackInWorld(BlockPosition position, ItemStack stack, Optional<Direction> direction, Optional<Predicate<ItemEntity>> validator) {
        if (position.world.isPresent() && !stack.isEmpty() && stack.getCount() > 0) {
            final Level world = position.world.get();
            final RandomSource rng = world.random;
            final int ox = direction.map(Direction::getStepX).orElse(0);
            final int oy = direction.map(Direction::getStepY).orElse(0);
            final int oz = direction.map(Direction::getStepZ).orElse(0);
            final double tx = 0.1 * (rng.nextDouble() - 0.5) + ox * 0.65;
            final double ty = 0.1 * (rng.nextDouble() - 0.5) + oy * 0.75 + (ox + oz) * 0.25;
            final double tz = 0.1 * (rng.nextDouble() - 0.5) + oz * 0.65;
            final Vec3 dropPos = position.offset(0.5 + tx, 0.5 + ty, 0.5 + tz);
            final ItemEntity entity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack.copy());
            entity.setDeltaMovement(new Vec3(
                    0.0125 * (rng.nextDouble() - 0.5) + ox * 0.03,
                    0.0125 * (rng.nextDouble() - 0.5) + oy * 0.08 + (ox + oz) * 0.03,
                    0.0125 * (rng.nextDouble() - 0.5) + oz * 0.03));
            if (validator.map(v -> v.test(entity)).orElse(true)) {
                entity.setPickUpDelay(15);
                world.addFreshEntity(entity);
                return entity;
            } else return null;
        }
        return null;
    }

    @Nullable
    public static ItemEntity spawnStackInWorld(BlockPosition position, ItemStack stack, Optional<Direction> direction) {
        return spawnStackInWorld(position, stack, direction, Optional.empty());
    }

    @Nullable
    public static ItemEntity spawnStackInWorld(BlockPosition position, ItemStack stack) {
        return spawnStackInWorld(position, stack, Optional.empty(), Optional.empty());
    }
}
