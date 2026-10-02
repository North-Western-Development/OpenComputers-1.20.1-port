package li.cil.oc.util;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

/**
 * Former implicit extension class turning a {@link Container} into a mutable
 * indexed sequence of its stacks. Use {@link #asList(Container)} for a live
 * {@link List} view (supports {@code get}/{@code set}/{@code size}, iteration,
 * streams), or the {@code length}/{@code apply}/{@code update} helpers.
 */
public final class ExtendedInventory {
    private ExtendedInventory() {
    }

    public static int length(Container inventory) {
        return inventory.getContainerSize();
    }

    public static ItemStack apply(Container inventory, int idx) {
        return inventory.getItem(idx);
    }

    public static void update(Container inventory, int idx, ItemStack elem) {
        inventory.setItem(idx, elem);
    }

    public static List<ItemStack> asList(Container inventory) {
        return new ContainerList(inventory);
    }

    private static final class ContainerList extends AbstractList<ItemStack> implements RandomAccess {
        private final Container inventory;

        ContainerList(Container inventory) {
            this.inventory = inventory;
        }

        @Override
        public ItemStack get(int index) {
            return inventory.getItem(index);
        }

        @Override
        public ItemStack set(int index, ItemStack element) {
            final ItemStack previous = inventory.getItem(index);
            inventory.setItem(index, element);
            return previous;
        }

        @Override
        public int size() {
            return inventory.getContainerSize();
        }
    }
}
