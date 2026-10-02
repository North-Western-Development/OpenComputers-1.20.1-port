package li.cil.oc.common.tileentity.traits;

import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Note: tiles that also implement {@link net.minecraft.world.MenuProvider} have to override
 * {@code getDisplayName()} themselves (Java cannot pick between MenuProvider's abstract and the
 * inventory's default declaration); return {@code getName()} as the Scala code did.
 */
public interface Inventory extends TileEntityTrait, li.cil.oc.common.inventory.Inventory {
    final class State {
        public ItemStack[] items;
    }

    /** Provided by {@link TileEntity}. */
    State inventoryState();

    @Override
    default ItemStack[] items() {
        final State state = inventoryState();
        if (state.items == null) {
            synchronized (state) {
                if (state.items == null) {
                    final ItemStack[] items = new ItemStack[getContainerSize()];
                    Arrays.fill(items, ItemStack.EMPTY);
                    state.items = items;
                }
            }
        }
        return state.items;
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(Inventory self, CompoundTag nbt) {
        self.loadData(nbt);
    }

    static void onSaveForServer(Inventory self, CompoundTag nbt) {
        self.saveData(nbt);
    }

    // ----------------------------------------------------------------------- //

    @Override
    default boolean stillValid(Player player) {
        return player.distanceToSqr(x() + 0.5, y() + 0.5, z() + 0.5) <= 64;
    }

    // ----------------------------------------------------------------------- //

    default void forAllLoot(Consumer<ItemStack> dst) {
        InventoryUtils.forAllSlots(this, dst);
    }

    default boolean dropSlot(int slot, int count, Optional<Direction> direction) {
        return InventoryUtils.dropSlot(new li.cil.oc.util.BlockPosition(x(), y(), z(), getLevel()), this, slot, count, direction);
    }

    default boolean dropSlot(int slot, int count) {
        return dropSlot(slot, count, Optional.empty());
    }

    default boolean dropSlot(int slot) {
        return dropSlot(slot, getMaxStackSize(), Optional.empty());
    }

    default void dropAllSlots() {
        InventoryUtils.dropAllSlots(new li.cil.oc.util.BlockPosition(x(), y(), z(), getLevel()), this);
    }

    default ItemEntity spawnStackInWorld(ItemStack stack, Optional<Direction> direction) {
        return InventoryUtils.spawnStackInWorld(new li.cil.oc.util.BlockPosition(x(), y(), z(), getLevel()), stack, direction);
    }

    default ItemEntity spawnStackInWorld(ItemStack stack) {
        return spawnStackInWorld(stack, Optional.empty());
    }
}
