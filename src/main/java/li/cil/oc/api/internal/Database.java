package li.cil.oc.api.internal;

import net.minecraft.item.ItemStack;

/**
 * This interface is implemented by the database component.
 * <p/>
 * This allows getting actual item stack instances as referenced by callers
 * of other components' callbacks, without having to push a full representation
 * of an item stacks' NBT data to the VM (which can be both a memory and a
 * security issue).
 * <p/>
 * To use this, you'll usually want to accept an address and either an index
 * or a hash in your component's callback function, the address being that of
 * a database component in the same network as your component. Alternatively
 * you can omit requiring an address and just loop through the other nodes on
 * the network to look for one those that have a <tt>Database</tt> as host.
 */
public interface Database {
    /**
     * The number of slots in this database.
     */
    int size();

    /**
     * Get an item stack stored in the specified slot of this database.
     * <p/>
     * This will return <tt>null</tt> if there is no entry for the specified
     * slot. If there is an entry, this will return a <em>copy</em> of that
     * item stack, so it is safe to modify the returned stack.
     *
     * @param slot the slot of the item stack.
     * @return the item stack stored in that slot.
     */
    ItemStack getStackInSlot(int slot);

    /**
     * Get an item stack with the specified hash stored in this database.
     * <p/>
     * This will return <tt>null</tt> if there is no entry with a matching
     * hash. If there is an entry, this will return a <em>copy</em> of that
     * item stack, so it is safe to modify the returned stack.
     *
     * @param hash the hash of the item stack.
     * @return the item stack with the specified hash.
     */
    ItemStack findStackWithHash(String hash);
}
