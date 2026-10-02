package li.cil.oc.common.item.data;

import li.cil.oc.api.Persistable;
import net.minecraft.world.item.ItemStack;

public abstract class ItemData implements Persistable {
    public final String itemName;

    protected ItemData(String itemName) {
        this.itemName = itemName;
    }

    public void loadData(ItemStack stack) {
        if (stack.hasTag()) {
            // Because ItemStack's load function doesn't copy the compound tag,
            // but keeps it as is, leading to oh so fun bugs!
            loadData(stack.getTag().copy());
        }
    }

    public void saveData(ItemStack stack) {
        saveData(stack.getOrCreateTag());
    }

    public ItemStack createItemStack() {
        if (itemName == null) return ItemStack.EMPTY;
        else {
            final ItemStack stack = li.cil.oc.api.Items.get(itemName).createItemStack(1);
            saveData(stack);
            return stack;
        }
    }
}
