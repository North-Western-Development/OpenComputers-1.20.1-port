package li.cil.oc.common.transfer;

import net.minecraft.world.item.ItemStack;

/**
 * Fluid handler for an item stack (buckets, tanks, ...). After a non-simulated
 * operation {@link #getContainer()} returns the resulting container item, which
 * may differ from the original stack (e.g. bucket -> water bucket).
 */
public interface ItemFluidHandler extends FluidHandler {
    ItemStack getContainer();
}
