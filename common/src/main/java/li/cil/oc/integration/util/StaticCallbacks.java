package li.cil.oc.integration.util;

import li.cil.oc.OpenComputers;

import java.lang.reflect.Method;

/**
 * Reflective invocation of IMC-registered static callbacks (formerly
 * {@code common.IMC.tryInvokeStatic} / {@code tryInvokeStaticVoid}).
 */
final class StaticCallbacks {
    private StaticCallbacks() {
    }

    @SuppressWarnings("unchecked")
    static <T> T tryInvokeStatic(Method method, T defaultValue, Object... args) {
        try {
            return (T) method.invoke(null, args);
        } catch (Throwable t) {
            OpenComputers.log.warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + ".", t);
            return defaultValue;
        }
    }

    static void tryInvokeStaticVoid(Method method, Object... args) {
        try {
            method.invoke(null, args);
        } catch (Throwable t) {
            OpenComputers.log.warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + ".", t);
        }
    }
}
