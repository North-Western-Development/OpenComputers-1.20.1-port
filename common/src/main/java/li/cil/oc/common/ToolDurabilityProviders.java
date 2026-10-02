package li.cil.oc.common;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ToolDurabilityProviders {
    private static final List<Method> providers = new ArrayList<>();

    public static void add(Method provider) {
        providers.add(provider);
    }

    public static Optional<Double> getDurability(ItemStack stack) {
        for (Method provider : providers) {
            final Object durability = IMC.tryInvokeStatic(provider, (Object) Double.NaN, stack);
            if (durability instanceof Number number && !Double.isNaN(number.doubleValue()))
                return Optional.of(number.doubleValue());
        }
        // Fall back to vanilla damage values.
        if (stack.isDamageableItem())
            return Optional.of(1.0 - (double) stack.getDamageValue() / (double) stack.getMaxDamage());
        else return Optional.empty();
    }

    private ToolDurabilityProviders() {
    }
}
