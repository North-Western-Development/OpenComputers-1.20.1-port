package li.cil.oc.server.machine;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.server.driver.CompoundBlockEnvironment;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class Callbacks {
    private Callbacks() {
    }

    private static final Map<Class<?>, Map<String, Callback>> cache = new HashMap<>();

    public static Map<String, Callback> apply(Object host) {
        if (host instanceof CompoundBlockEnvironment || host instanceof ManagedPeripheral || host instanceof FilteredEnvironment) {
            return dynamicAnalyze(host);
        }
        synchronized (cache) {
            final Map<String, Callback> cached = cache.get(host.getClass());
            if (cached != null) return cached;
        }
        final Map<String, Callback> result = dynamicAnalyze(host);
        synchronized (cache) {
            cache.putIfAbsent(host.getClass(), result);
            return cache.get(host.getClass());
        }
    }

    // Clear the cache; used when world is unloaded, mostly to allow reacting to
    // stuff (aka configs) that may influence which @Callbacks are enabled.
    public static void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    public static Map<String, Callback> fromClass(Class<?> environment) {
        return staticAnalyze(environment, Optional.empty(), Optional.empty());
    }

    private static Map<String, Callback> dynamicAnalyze(Object host) {
        final List<Set<String>> whitelists = new ArrayList<>();
        final Map<String, Callback> callbacks = new HashMap<>();

        // Lazily computed to allow referencing it in closures before it's
        // actually initialized after the base whitelist has been compiled.
        final Set<String>[] whitelist = new Set[1];
        final Predicate<String> shouldAdd = name -> {
            if (whitelist[0] == null) {
                if (whitelists.isEmpty()) whitelist[0] = Collections.emptySet();
                else {
                    final Set<String> result = new HashSet<>(whitelists.get(0));
                    for (int i = 1; i < whitelists.size(); i++) result.retainAll(whitelists.get(i));
                    whitelist[0] = result;
                }
            }
            return !callbacks.containsKey(name) && (whitelist[0].isEmpty() || whitelist[0].contains(name));
        };

        final List<Pair<Integer, Runnable>> tasks = new ArrayList<>();
        if (host instanceof CompoundBlockEnvironment multi) {
            for (Pair<String, ManagedEnvironment> env : multi.environments) {
                tasks.add(process(env.getRight(), whitelists, callbacks, shouldAdd));
            }
        } else {
            tasks.add(process(host, whitelists, callbacks, shouldAdd));
        }

        // First collect whitelist and priority information, then sort and
        // fetch callbacks.
        tasks.sort(Comparator.comparingInt(p -> -p.getLeft()));
        for (Pair<Integer, Runnable> task : tasks) task.getRight().run();

        return Collections.unmodifiableMap(new HashMap<>(callbacks));
    }

    private static Pair<Integer, Runnable> process(Object environment, List<Set<String>> whitelists, Map<String, Callback> callbacks, Predicate<String> shouldAdd) {
        if (environment instanceof MethodWhitelist list) {
            final String[] methods = list.whitelistedMethods();
            whitelists.add(methods == null ? Collections.emptySet() : new HashSet<>(List.of(methods)));
        }
        final int priority = environment instanceof NamedBlock named ? named.priority() : 0;
        final Predicate<String> filter = environment instanceof FilteredEnvironment filtered
                ? s -> shouldAdd.test(s) && filtered.isCallbackEnabled(s)
                : shouldAdd;
        if (environment instanceof ManagedPeripheral peripheral) {
            return Pair.of(priority, () -> {
                for (String name : peripheral.methods()) {
                    if (filter.test(name)) {
                        callbacks.put(name, new PeripheralCallback(name));
                    }
                }
                staticAnalyze(environment.getClass(), Optional.of(filter), Optional.of(callbacks));
            });
        } else {
            return Pair.of(priority, () -> staticAnalyze(environment.getClass(), Optional.of(filter), Optional.of(callbacks)));
        }
    }

    private static Map<String, Callback> staticAnalyze(Class<?> seed, Optional<Predicate<String>> shouldAdd, Optional<Map<String, Callback>> optCallbacks) {
        final Map<String, Callback> callbacks = optCallbacks.orElseGet(HashMap::new);
        // Classes first (subclass to superclass, as before), then all interfaces
        // implemented anywhere in the hierarchy (Scala traits became Java
        // interfaces with @Callback default methods). Methods declared by a
        // class take precedence over interface defaults.
        final Set<Class<?>> interfaces = new java.util.LinkedHashSet<>();
        final java.util.ArrayDeque<Class<?>> pending = new java.util.ArrayDeque<>();
        Class<?> c = seed;
        while (c != null && c != Object.class) {
            analyzeDeclared(c, shouldAdd, callbacks, false);
            pending.addAll(List.of(c.getInterfaces()));
            c = c.getSuperclass();
        }
        while (!pending.isEmpty()) {
            final Class<?> iface = pending.poll();
            if (interfaces.add(iface)) {
                pending.addAll(List.of(iface.getInterfaces()));
            }
        }
        for (Class<?> iface : interfaces) {
            analyzeDeclared(iface, shouldAdd, callbacks, true);
        }
        return callbacks;
    }

    private static void analyzeDeclared(Class<?> c, Optional<Predicate<String>> shouldAdd, Map<String, Callback> callbacks, boolean isInterface) {
        for (Method m : c.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(li.cil.oc.api.machine.Callback.class)) continue;
            final Class<?>[] params = m.getParameterTypes();
            if (params.length != 2 || params[0] != Context.class || params[1] != Arguments.class) {
                OpenComputers.log.error("Invalid use of Callback annotation on " + m.getDeclaringClass().getName() + "." + m.getName() + ": invalid argument types or count.");
            } else if (m.getReturnType() != Object[].class) {
                OpenComputers.log.error("Invalid use of Callback annotation on " + m.getDeclaringClass().getName() + "." + m.getName() + ": invalid return type.");
            } else if (!Modifier.isPublic(m.getModifiers())) {
                OpenComputers.log.error("Invalid use of Callback annotation on " + m.getDeclaringClass().getName() + "." + m.getName() + ": method must be public.");
            } else if (Modifier.isStatic(m.getModifiers()) || (isInterface && Modifier.isAbstract(m.getModifiers()))) {
                OpenComputers.log.error("Invalid use of Callback annotation on " + m.getDeclaringClass().getName() + "." + m.getName() + ": method must be an instance method with an implementation.");
            } else {
                final li.cil.oc.api.machine.Callback a = m.getAnnotation(li.cil.oc.api.machine.Callback.class);
                final String name = a.value() != null && !a.value().trim().isEmpty() ? a.value() : m.getName();
                if (isInterface && callbacks.containsKey(name)) continue; // Class methods take precedence.
                if (shouldAdd.map(f -> f.test(name)).orElse(true)) {
                    callbacks.put(name, new ComponentCallback(m, a));
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    public abstract static class Callback {
        public final li.cil.oc.api.machine.Callback annotation;

        protected Callback(li.cil.oc.api.machine.Callback annotation) {
            this.annotation = annotation;
        }

        public abstract Object[] apply(Object instance, Context context, Arguments args) throws Exception;
    }

    public static class ComponentCallback extends Callback {
        public final Method method;
        public final CallbackCall callWrapper;

        public ComponentCallback(Method method, li.cil.oc.api.machine.Callback annotation) {
            super(annotation);
            this.method = method;
            this.callWrapper = CallbackWrapper.createCallbackWrapper(method);
        }

        @Override
        public Object[] apply(Object instance, Context context, Arguments args) {
            return callWrapper.call(instance, context, args);
        }
    }

    public static class PeripheralCallback extends Callback {
        private final String name;

        public PeripheralCallback(String name) {
            super(new PeripheralAnnotation(name));
            this.name = name;
        }

        @Override
        public Object[] apply(Object instance, Context context, Arguments args) throws Exception {
            if (instance instanceof ManagedPeripheral peripheral) return peripheral.invoke(name, context, args);
            throw new NoSuchMethodException();
        }
    }
}
