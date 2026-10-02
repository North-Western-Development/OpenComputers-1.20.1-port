package li.cil.oc.common.transfer;

import net.minecraft.world.item.ItemStack;

/**
 * Loader-agnostic slotted item storage, modelled after Forge's {@code IItemHandler}.
 * <p>
 * On Forge this wraps the {@code ITEM_HANDLER} capability; on Fabric it wraps
 * {@code Storage<ItemVariant>} from the Transfer API (simulation is done by
 * aborting a transaction). Obtain instances through
 * {@link li.cil.oc.common.platform.PlatformHooks}.
 */
public interface ItemHandler {
    int getSlots();

    /** The returned stack must not be modified. */
    ItemStack getStackInSlot(int slot);

    /** @return the remainder that could not be inserted. */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** @return the extracted stack (empty if nothing could be extracted). */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    default boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
