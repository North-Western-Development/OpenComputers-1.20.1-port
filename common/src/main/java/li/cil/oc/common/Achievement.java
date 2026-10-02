package li.cil.oc.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Placeholder, as on 1.16.5. The 1.12 achievement tree (transistor, chip,
// assembler, robot, ...) was never ported to advancements.
// TODO(port): implement as data driven advancements (e.g. with custom criteria
//  triggers fired from onCraft / onAssemble).
public final class Achievement {
    public static void init() {
    }

    public static void onAssemble(ItemStack stack, Player player) {
    }

    public static void onCraft(ItemStack stack, Player player) {
    }

    private Achievement() {
    }
}
