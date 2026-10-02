package li.cil.oc.util;

import net.minecraft.world.item.ItemStack;

public final class ResultWrapper {
    private ResultWrapper() {
    }

    /**
     * Replacement for Scala's boxed unit {@code ()}: a sentinel that is
     * treated like {@code nil} / {@code null} when converting results to Lua
     * (compare by identity).
     */
    public static final Object unit = new Object() {
        @Override
        public String toString() {
            return "()";
        }
    };

    public static Object[] result(Object... args) {
        if (args == null) return new Object[]{null};
        final Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = unwrap(args[i]);
        }
        return result;
    }

    private static Object unwrap(Object arg) {
        if (arg instanceof ItemStack x && x.isEmpty()) return null;
        return arg;
    }
}
