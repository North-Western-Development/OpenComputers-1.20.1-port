package li.cil.oc.integration.computercraft;

import com.google.common.collect.Iterables;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.MethodResult;
import dev.architectury.utils.GameInstance;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class CallableHelper {
    private final List<String> _methods;

    public CallableHelper(final String[] methods) {
        _methods = Arrays.asList(methods);
    }

    public int methodIndex(final String method) throws NoSuchMethodException {
        final int index = _methods.indexOf(method);
        if (index < 0) {
            throw new NoSuchMethodException();
        }
        return index;
    }

    public static Object[] convertArguments(final Arguments args) {
        final Object[] argArray = Iterables.toArray(args, Object.class);
        for (int i = 0; i < argArray.length; ++i) {
            if (argArray[i] instanceof byte[] bytes) {
                argArray[i] = new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return argArray;
    }

    /**
     * Unwraps the result of a CC method call. Results that would need to yield the Lua coroutine
     * (e.g. waiting for an event) cannot be mapped to OC's call model.
     */
    public static Object[] unwrapResult(final MethodResult result) {
        final ILuaCallback callback = result.getCallback();
        if (callback != null) {
            // TODO(port): CC methods that yield (pullEvent etc.) are not supported from OC.
            throw new UnsupportedOperationException("ComputerCraft method requires yielding, which is not supported");
        }
        final Object[] values = result.getResult();
        return values == null ? new Object[0] : values;
    }

    /**
     * The Lua context passed to CC methods called from OC. Since we abstract away anything language
     * specific, we cannot support CC's coroutine based operations. "Main thread tasks" are run
     * right away when on the server thread; when called directly from the computer thread, we bail
     * out with a {@link LimitReachedException}, which makes OC repeat the call synchronously on the
     * server thread.
     */
    public static final class LuaContext implements ILuaContext {
        private static final AtomicLong nextTaskId = new AtomicLong(1);

        private final Context context;

        public LuaContext(final Context context) {
            this.context = context;
        }

        private static boolean isServerThread() {
            final MinecraftServer server = GameInstance.getServer();
            return server != null && server.isSameThread();
        }

        @Override
        public long issueMainThreadTask(final LuaTask task) throws LuaException {
            final long id = nextTaskId.getAndIncrement();
            final Runnable runner = () -> {
                Object[] results;
                boolean success = true;
                try {
                    results = task.execute();
                } catch (LuaException e) {
                    success = false;
                    results = new Object[]{e.getMessage()};
                }
                if (context != null) {
                    final Object[] event = new Object[2 + (results == null ? 0 : results.length)];
                    event[0] = (double) id;
                    event[1] = success;
                    if (results != null) System.arraycopy(results, 0, event, 2, results.length);
                    context.signal("task_complete", event);
                }
            };
            final MinecraftServer server = GameInstance.getServer();
            if (server == null) throw new LuaException("server not running");
            if (server.isSameThread()) runner.run();
            else server.execute(runner);
            return id;
        }

        @Override
        public MethodResult executeMainThreadTask(final LuaTask task) throws LuaException {
            if (!isServerThread()) {
                throw CallableHelper.<RuntimeException>sneakyThrow(new LimitReachedException());
            }
            return MethodResult.of(task.execute());
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> T sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }
}
