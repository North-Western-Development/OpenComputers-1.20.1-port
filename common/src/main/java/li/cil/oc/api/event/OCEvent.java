package li.cil.oc.api.event;

/**
 * Base class for all events fired by OpenComputers.
 * <p/>
 * This replaces the Forge event base class used in earlier versions so that
 * the API is independent of the mod loader. Events are posted to and received
 * from {@link EventBus#INSTANCE}.
 * <p/>
 * Events that can be canceled override {@link #isCancelable()} to return
 * <tt>true</tt> (this replaces Forge's <tt>@Cancelable</tt> annotation).
 */
public abstract class OCEvent {
    private volatile boolean canceled;

    /**
     * Whether this event can be canceled via {@link #setCanceled(boolean)}.
     *
     * @return <tt>true</tt> if this event is cancelable.
     */
    public boolean isCancelable() {
        return false;
    }

    /**
     * Whether this event has been canceled by a listener.
     *
     * @return <tt>true</tt> if the event was canceled.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Cancel (or un-cancel) this event.
     *
     * @param cancel whether to cancel the event.
     * @throws UnsupportedOperationException if the event is not cancelable.
     */
    public void setCanceled(final boolean cancel) {
        if (!isCancelable()) {
            throw new UnsupportedOperationException("Attempted to call setCanceled() on a non-cancelable event of type: " + getClass().getName());
        }
        canceled = cancel;
    }
}
