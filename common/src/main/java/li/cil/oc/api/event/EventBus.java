package li.cil.oc.api.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Minimal, loader-independent event bus used for all OpenComputers events
 * (replaces <tt>MinecraftForge.EVENT_BUS</tt> from earlier versions).
 * <p/>
 * Use the shared instance {@link #INSTANCE}:
 * <pre>
 * EventBus.INSTANCE.register(RobotMoveEvent.Pre.class, event -&gt; {
 *     if (shouldBlock(event)) event.setCanceled(true);
 * });
 * </pre>
 * A listener registered for a type receives events of that type and of all
 * its subclasses, e.g. a listener for {@link RobotEvent} receives every robot
 * event. Listeners are invoked in registration order (for each type, starting
 * with the most specific type of the posted event and walking up its
 * superclasses). Canceled events are still delivered to remaining listeners,
 * which may inspect {@link OCEvent#isCanceled()}.
 * <p/>
 * This class is thread-safe.
 */
public final class EventBus {
    /**
     * The event bus all OpenComputers events are posted to.
     */
    public static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    /**
     * Register a listener for events of the specified type (and subtypes).
     *
     * @param type     the type of event to listen for.
     * @param listener the listener to call when such an event is posted.
     * @param <T>      the type of event.
     */
    public <T extends OCEvent> void register(final Class<T> type, final Consumer<? super T> listener) {
        if (type == null || listener == null) {
            throw new IllegalArgumentException("type and listener must not be null");
        }
        listeners.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove a listener previously registered via {@link #register(Class, Consumer)}.
     *
     * @param type     the type the listener was registered for.
     * @param listener the listener to remove.
     */
    public void unregister(final Class<? extends OCEvent> type, final Consumer<?> listener) {
        final List<Consumer<?>> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Post an event to all listeners registered for its class or any of its
     * superclasses.
     *
     * @param event the event to post.
     * @return <tt>true</tt> if the event was canceled.
     */
    @SuppressWarnings("unchecked")
    public boolean post(final OCEvent event) {
        Class<?> type = event.getClass();
        while (type != null && OCEvent.class.isAssignableFrom(type)) {
            final List<Consumer<?>> list = listeners.get(type);
            if (list != null) {
                for (final Consumer<?> listener : list) {
                    ((Consumer<OCEvent>) listener).accept(event);
                }
            }
            type = type.getSuperclass();
        }
        return event.isCanceled();
    }
}
