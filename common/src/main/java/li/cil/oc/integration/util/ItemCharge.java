package li.cil.oc.integration.util;

import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ItemCharge {
    private ItemCharge() {
    }

    private static final Set<Pair<Method, Method>> chargers = new LinkedHashSet<>();

    public static void add(Method canCharge, Method charge) {
        chargers.add(Pair.of(canCharge, charge));
    }

    public static boolean canCharge(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Pair<Method, Method> charger : chargers) {
            if (StaticCallbacks.tryInvokeStatic(charger.getLeft(), false, stack)) return true;
        }
        return false;
    }

    // Returns the amount of the delta that could not be applied.
    public static double charge(ItemStack stack, double amount) {
        if (stack.isEmpty()) return amount;
        for (Pair<Method, Method> charger : chargers) {
            if (StaticCallbacks.tryInvokeStatic(charger.getLeft(), false, stack)) {
                return StaticCallbacks.tryInvokeStatic(charger.getRight(), amount, stack, amount, Boolean.FALSE);
            }
        }
        return amount;
    }
}
