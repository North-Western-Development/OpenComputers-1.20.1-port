package li.cil.oc.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ItemStackWrapper implements Comparable<ItemStackWrapper>, Cloneable {
    public final ItemStack inner;

    public ItemStackWrapper(ItemStack inner) {
        this.inner = inner;
    }

    public int id() {
        return inner.getItem() != null ? Item.getId(inner.getItem()) : 0;
    }

    public int damage() {
        return inner.getItem() != null ? inner.getDamageValue() : 0;
    }

    @Override
    public int compareTo(ItemStackWrapper that) {
        if (this.id() == that.id()) return this.damage() - that.damage();
        else return this.id() - that.id();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id(), damage());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemStackWrapper that) {
            return compareTo(that) == 0;
        }
        return false;
    }

    @Override
    public ItemStackWrapper clone() {
        return new ItemStackWrapper(inner);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
