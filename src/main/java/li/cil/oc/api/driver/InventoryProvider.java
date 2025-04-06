package li.cil.oc.api.driver;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Inventory providers are used to access contents of item inventories.
 * <br>
 * In OpenComputers an example for this would be servers, in other mods
 * this can be backpacks and the like. Inventory providers are used to
 * allow agents (robots, drones) to interact with such inventories using
 * the inventory controller upgrade, for example.
 * <br>
 * Implementations returned by {@link #getInventory} should save changes
 * back to the item stack when {@link IInventory#setChanged()} is called.
 * Return {@code null} if the specified stack is not supported.
 */
public interface InventoryProvider {
    /**
     * Checks whether this provider works for the specified item stack.
     *
     * @param stack  the item stack to check for.
     * @param player the player holding the item, may be {@code null}.
     * @return {@code true} if the stack is supported, {@code false} otherwise.
     */
    boolean worksWith(ItemStack stack, PlayerEntity player);

    /**
     * Get an inventory implementation that allows interfacing with the
     * item inventory represented by the specified item stack.
     * <br>
     * Note that the specified player may be {@code null}, but will
     * usually be the <em>fake player</em> of the agent using the
     * inventory controller upgrade to access the item inventory.
     *
     * @param stack  the item stack to get the inventory for.
     * @param player the player holding the item, may be {@code null}.
     * @return the inventory representing the contents, or {@code null}.
     */
    IInventory getInventory(ItemStack stack, PlayerEntity player);
}
