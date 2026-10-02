package li.cil.oc.integration.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Wrench {
    private Wrench() {
    }

    private static final Set<Method> usages = new LinkedHashSet<>();
    private static final Set<Method> checks = new LinkedHashSet<>();

    public static void addUsage(Method wrench) {
        usages.add(wrench);
    }

    public static void addCheck(Method checker) {
        checks.add(checker);
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Method check : checks) {
            if (StaticCallbacks.tryInvokeStatic(check, false, stack)) return true;
        }
        return false;
    }

    public static boolean holdsApplicableWrench(Player player, BlockPos position) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return false;
        for (Method usage : usages) {
            if (StaticCallbacks.tryInvokeStatic(usage, false, player, position, Boolean.FALSE)) return true;
        }
        return false;
    }

    public static void wrenchUsed(Player player, BlockPos position) {
        if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            for (Method usage : usages) {
                StaticCallbacks.tryInvokeStaticVoid(usage, player, position, Boolean.TRUE);
            }
        }
    }
}
