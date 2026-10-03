package li.cil.oc.integration.projectred;

import mrtjp.projectred.api.IScrewdriver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Wrench callbacks for ProjectRed screwdrivers (registered via {@link li.cil.oc.api.IMC}).
 */
public final class EventHandlerProjectRed {
    private EventHandlerProjectRed() {
    }

    public static boolean useWrench(final Player player, final BlockPos pos, final boolean changeDurability) {
        final ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof IScrewdriver wrench) {
            if (!wrench.canUse(player, stack)) {
                return false;
            }
            if (changeDurability) {
                wrench.damageScrewdriver(player, stack);
            }
            return true;
        }
        return false;
    }

    public static boolean isWrench(final ItemStack stack) {
        return stack.getItem() instanceof IScrewdriver;
    }
}
