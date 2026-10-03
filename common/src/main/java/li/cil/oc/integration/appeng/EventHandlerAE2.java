package li.cil.oc.integration.appeng;

import appeng.items.tools.quartz.QuartzWrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Wrench support for AE2's quartz wrenches. AE2 15 no longer has an {@code IAEWrench} interface
 * (its wrenches are plain items without durability), so holding any AE2 quartz wrench counts.
 */
public final class EventHandlerAE2 {
    private EventHandlerAE2() {
    }

    public static boolean useWrench(Player player, BlockPos pos, boolean changeDurability) {
        return isWrench(player.getItemInHand(InteractionHand.MAIN_HAND));
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof QuartzWrenchItem;
    }
}
