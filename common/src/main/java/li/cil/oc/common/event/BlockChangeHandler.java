package li.cil.oc.common.event;

import li.cil.oc.common.EventHandler;
import li.cil.oc.util.BlockPosition;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Vexatos
 */
@Deprecated
public final class BlockChangeHandler {
    private BlockChangeHandler() {
    }

    private static final Map<ChangeListener, BlockPosition> changeListeners = Collections.synchronizedMap(new WeakHashMap<>());

    public static void addListener(ChangeListener listener, BlockPosition coord) {
        EventHandler.scheduleServer(() -> changeListeners.put(listener, coord));
    }

    public static void removeListener(ChangeListener listener) {
        EventHandler.scheduleServer(() -> changeListeners.remove(listener));
    }

    public interface ChangeListener {
        void onBlockChanged();
    }
}
