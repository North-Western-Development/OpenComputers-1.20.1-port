package li.cil.oc.server.machine.luac;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaGcMetamethodException;
import li.cil.repack.com.naef.jnlua.LuaMemoryAllocationException;
import li.cil.repack.com.naef.jnlua.LuaRuntimeException;
import li.cil.repack.com.naef.jnlua.LuaStackTraceElement;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.concurrent.Callable;

public abstract class NativeLuaArchitecture implements Architecture {
    public final li.cil.oc.api.machine.Machine machine;

    protected abstract LuaStateFactory factory();

    LuaState lua = null;

    int kernelMemory = 0;

    final double ramScale;

    private final PersistenceAPI persistence;

    private final NativeLuaAPI[] apis;

    protected NativeLuaArchitecture(li.cil.oc.api.machine.Machine machine) {
        this.machine = machine;
        this.ramScale = factory().is64Bit ? Settings.get().ramScaleFor64Bit : 1.0;
        this.persistence = new PersistenceAPI(this);
        this.apis = new NativeLuaAPI[]{
                new ComponentAPI(this),
                new ComputerAPI(this),
                new OSAPI(this),
                new SystemAPI(this),
                new UnicodeAPI(this),
                new UserdataAPI(this),
                // Persistence has to go last to ensure all other APIs can go into the permanent value table.
                persistence};
    }

    static String luaStackTrace(LuaRuntimeException e, String separator) {
        final LuaStackTraceElement[] trace = e.getLuaStackTrace();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trace.length; i++) {
            if (i > 0) sb.append(separator);
            sb.append(trace[i]);
        }
        return sb.toString();
    }

    int invoke(Callable<Object[]> f) {
        try {
            final Object[] results = f.call();
            if (results != null) {
                lua.pushBoolean(true);
                for (Object result : results) ExtendedLuaState.pushValue(lua, result);
                return 1 + results.length;
            }
            lua.pushBoolean(true);
            return 1;
        } catch (Throwable e) {
            if (Settings.get().logLuaCallbackErrors && !(e instanceof LimitReachedException)) {
                OpenComputers.log.warn("Exception in Lua callback.", e);
            }
            if (e instanceof LimitReachedException) {
                return 0;
            } else if (e instanceof IllegalArgumentException && e.getMessage() != null) {
                lua.pushBoolean(false);
                lua.pushString(e.getMessage());
                return 2;
            } else if (e.getMessage() != null) {
                lua.pushBoolean(true);
                lua.pushNil();
                lua.pushString(e.getMessage());
                if (Settings.get().logLuaCallbackErrors) {
                    final StringBuilder sb = new StringBuilder();
                    for (StackTraceElement element : e.getStackTrace()) sb.append(element).append("\n");
                    lua.pushString(sb.toString());
                    return 4;
                }
                return 3;
            } else if (e instanceof IndexOutOfBoundsException) {
                lua.pushBoolean(false);
                lua.pushString("index out of bounds");
                return 2;
            } else if (e instanceof IllegalArgumentException) {
                lua.pushBoolean(false);
                lua.pushString("bad argument");
                return 2;
            } else if (e instanceof NoSuchMethodException) {
                lua.pushBoolean(false);
                lua.pushString("no such method");
                return 2;
            } else if (e instanceof FileNotFoundException) {
                lua.pushBoolean(true);
                lua.pushNil();
                lua.pushString("file not found");
                return 3;
            } else if (e instanceof SecurityException) {
                lua.pushBoolean(true);
                lua.pushNil();
                lua.pushString("access denied");
                return 3;
            } else if (e instanceof IOException) {
                lua.pushBoolean(true);
                lua.pushNil();
                lua.pushString("i/o error");
                return 3;
            } else if (e instanceof UnsupportedOperationException) {
                lua.pushBoolean(false);
                lua.pushString("unsupported operation");
                return 2;
            } else {
                OpenComputers.log.warn("Unexpected error in Lua callback.", e);
                lua.pushBoolean(true);
                lua.pushNil();
                lua.pushString("unknown error");
                return 3;
            }
        }
    }

    int documentation(Callable<String> f) {
        try {
            final String doc = f.call();
            if (Strings.isNullOrEmpty(doc)) lua.pushNil();
            else lua.pushString(doc);
            return 1;
        } catch (NoSuchMethodException e) {
            lua.pushNil();
            lua.pushString("no such method");
            return 2;
        } catch (Throwable t) {
            lua.pushNil();
            lua.pushString(t.getMessage() != null ? t.getMessage() : t.toString());
            return 2;
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isInitialized() {
        return kernelMemory > 0;
    }

    @Override
    public boolean recomputeMemory(Iterable<ItemStack> components) {
        final int memory = (int) Math.ceil(memoryInBytes(components) * ramScale);
        final LuaState l = lua;
        if (l != null && Settings.get().limitMemory) {
            l.setTotalMemory(Integer.MAX_VALUE);
            if (kernelMemory > 0) {
                l.setTotalMemory(kernelMemory + memory);
            }
        }
        return memory > 0;
    }

    private int memoryInBytes(Iterable<ItemStack> components) {
        double acc = 0.0;
        for (ItemStack stack : components) {
            final DriverItem driver = Driver.driverFor(stack);
            if (driver instanceof Memory memoryDriver) acc += memoryDriver.amount(stack) * 1024;
        }
        return Math.min(Math.max((int) acc, 0), Settings.get().maxTotalRam);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void runSynchronized() {
        // These three asserts are all guaranteed by run().
        assert lua.getTop() == 2;
        assert lua.isThread(1);
        assert lua.isFunction(2);

        try {
            // Synchronized call protocol requires the called function to return
            // a table, which holds the results of the call, to be passed back
            // to the coroutine.yield() that triggered the call.
            lua.call(0, 1);
            lua.checkType(2, LuaType.TABLE);
        } catch (LuaMemoryAllocationException e) {
            // This can happen if we run out of memory while converting a Java
            // exception to a string (which we have to do to avoid keeping
            // userdata on the stack, which cannot be persisted).
            throw new OutOfMemoryError("not enough memory");
        }
    }

    @Override
    public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
        try {
            // The kernel thread will always be at stack index one.
            assert lua.isThread(1);

            // Resume the Lua state and remember the number of results we get.
            final int results;
            if (isSynchronizedReturn) {
                // If we were doing a synchronized call, continue where we left off.
                assert lua.getTop() == 2;
                assert lua.isTable(2);
                results = lua.resume(1, 1);
            } else {
                if (kernelMemory == 0) {
                    // We're doing the initialization run.
                    if (lua.resume(1, 0) > 0) {
                        // We expect to get nothing here, if we do we had an error.
                        results = 0;
                    } else {
                        // Run the garbage collector to get rid of stuff left behind after
                        // the initialization phase to get a good estimate of the base
                        // memory usage the kernel has (including libraries). We remember
                        // that size to grant user-space programs a fixed base amount of
                        // memory, regardless of the memory need of the underlying system
                        // (which may change across releases).
                        lua.gc(LuaState.GcAction.COLLECT, 0);
                        kernelMemory = Math.max(lua.getTotalMemory() - lua.getFreeMemory(), 1);
                        recomputeMemory(machine.host().internalComponents());

                        // Fake zero sleep to avoid stopping if there are no signals.
                        lua.pushInteger(0);
                        results = 1;
                    }
                } else {
                    final Signal signal = machine.popSignal();
                    if (signal != null) {
                        lua.pushString(signal.name());
                        for (Object arg : signal.args()) ExtendedLuaState.pushValue(lua, arg);
                        results = lua.resume(1, 1 + signal.args().length);
                    } else {
                        results = lua.resume(1, 0);
                    }
                }
            }

            // Check if the kernel is still alive.
            if (lua.status(1) == LuaState.YIELD) {
                // If we get one function it must be a wrapper for a synchronized
                // call. The protocol is that a closure is pushed that is then called
                // from the main server thread, and returns a table, which is in turn
                // passed to the originating coroutine.yield().
                if (results == 1 && lua.isFunction(2)) {
                    return new ExecutionResult.SynchronizedCall();
                }
                // Check if we are shutting down, and if so if we're rebooting. This
                // is signalled by boolean values, where `false` means shut down,
                // `true` means reboot (i.e shutdown then start again).
                else if (results == 1 && lua.isBoolean(2)) {
                    return new ExecutionResult.Shutdown(lua.toBoolean(2));
                } else {
                    // If we have a single number, that's how long we may wait before
                    // resuming the state again. Note that the sleep may be interrupted
                    // early if a signal arrives in the meantime. If we have something
                    // else we just process the next signal or wait for one.
                    final int ticks = results == 1 && lua.isNumber(2) ? (int) (lua.toNumber(2) * 20) : Integer.MAX_VALUE;
                    lua.pop(results);
                    return new ExecutionResult.Sleep(ticks);
                }
            }
            // The kernel thread returned. If it threw we'd be in the catch below.
            else {
                assert lua.isThread(1);
                // We're expecting the result of a pcall, if anything, so boolean + (result | string).
                if (!lua.isBoolean(2) || !(lua.isString(3) || lua.isNoneOrNil(3))) {
                    OpenComputers.log.warn("Kernel returned unexpected results.");
                }
                // The pcall *should* never return normally... but check for it nonetheless.
                if (lua.toBoolean(2)) {
                    OpenComputers.log.warn("Kernel stopped unexpectedly.");
                    return new ExecutionResult.Shutdown(false);
                } else {
                    if (Settings.get().limitMemory) {
                        lua.setTotalMemory(Integer.MAX_VALUE);
                    }
                    final String error = lua.isJavaObjectRaw(3) ? lua.toJavaObjectRaw(3).toString() : lua.toString(3);
                    if (error != null) return new ExecutionResult.Error(error);
                    else return new ExecutionResult.Error("unknown error");
                }
            }
        } catch (LuaRuntimeException e) {
            OpenComputers.log.warn("Kernel crashed. This is a bug!\n" + e + "\tat " + luaStackTrace(e, "\n\tat "));
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        } catch (LuaGcMetamethodException e) {
            if (e.getMessage() != null) return new ExecutionResult.Error("kernel panic:\n" + e.getMessage());
            else return new ExecutionResult.Error("kernel panic:\nerror in garbage collection metamethod");
        } catch (LuaMemoryAllocationException e) {
            return new ExecutionResult.Error("not enough memory");
        } catch (Error e) {
            if ("not enough memory".equals(e.getMessage())) return new ExecutionResult.Error("not enough memory");
            throw e;
        }
    }

    @Override
    public void onSignal() {
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean initialize() {
        // Creates a new state with all base libraries and the persistence library
        // loaded into it. This means the state has much more power than it
        // rightfully should have, so we sandbox it a bit in the following.
        final Optional<LuaState> state = factory().createState();
        if (state.isEmpty()) {
            lua = null;
            machine.crash("native libraries not available");
            return false;
        }
        lua = state.get();

        for (NativeLuaAPI api : apis) api.initialize();

        try {
            lua.load(Machine.class.getResourceAsStream(Settings.scriptPath + "machine.lua"), "=machine", "t");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        lua.newThread(); // Left as the first value on the stack.

        return true;
    }

    @Override
    public void onConnect() {
    }

    @Override
    public void close() {
        if (lua != null) {
            if (Settings.get().limitMemory) {
                lua.setTotalMemory(Integer.MAX_VALUE);
            }
            lua.close();
        }
        lua = null;
        kernelMemory = 0;
    }

    // ----------------------------------------------------------------------- //

    // Transition to storing the 'are we in or returning from a sync call' in here
    // so we don't need to check the state. Will need a period where saves are
    // loaded using the old *and* new method and saved using the new.
    @Deprecated
    private ArrayDeque<Machine.State> state() {
        return ((Machine) machine).state;
    }

    private boolean stateContains(Machine.State value) {
        final ArrayDeque<Machine.State> state = state();
        synchronized (state) {
            return state.contains(value);
        }
    }

    @Override
    public void loadData(CompoundTag nbt) {
        if (!machine.isRunning()) return;

        // Unlimit memory use while unpersisting.
        if (Settings.get().limitMemory) {
            lua.setTotalMemory(Integer.MAX_VALUE);
        }

        try {
            // Try unpersisting Lua, because that's what all of the rest depends
            // on. First, clear the stack, meaning the current kernel.
            lua.setTop(0);

            persistence.unpersist(SaveHandler.load(nbt, machine.node().address() + "_kernel"));
            if (!lua.isThread(1)) {
                // This shouldn't really happen, but there's a chance it does if
                // the save was corrupt (maybe someone modified the Lua files).
                throw new LuaRuntimeException("Invalid kernel.");
            }
            if (stateContains(Machine.State.SynchronizedCall) || stateContains(Machine.State.SynchronizedReturn)) {
                persistence.unpersist(SaveHandler.load(nbt, machine.node().address() + "_stack"));
                if (!(stateContains(Machine.State.SynchronizedCall) ? lua.isFunction(2) : lua.isTable(2))) {
                    // Same as with the above, should not really happen normally, but
                    // could for the same reasons.
                    throw new LuaRuntimeException("Invalid stack.");
                }
            }

            kernelMemory = (int) (nbt.getInt("kernelMemory") * ramScale);

            for (NativeLuaAPI api : apis) {
                api.loadData(nbt);
            }

            try {
                lua.gc(LuaState.GcAction.COLLECT, 0);
            } catch (Throwable t) {
                OpenComputers.log.warn("Error cleaning up loaded computer @ (" + machine.host().xPosition() + ", " + machine.host().yPosition() + ", " + machine.host().zPosition() + "). This either means the server is badly overloaded or a user created an evil __gc method, accidentally or not.");
                machine.crash("error in garbage collector, most likely __gc method timed out");
            }
        } catch (LuaRuntimeException e) {
            throw NativeLuaAPI.<RuntimeException>sneakyThrow(new Exception(e + (e.getLuaStackTrace().length == 0 ? "" : "\tat " + luaStackTrace(e, "\n\tat ")), e));
        }

        // Limit memory again.
        recomputeMemory(machine.host().internalComponents());
    }

    @Override
    public void saveData(CompoundTag nbt) {
        // Unlimit memory while persisting.
        if (Settings.get().limitMemory) {
            lua.setTotalMemory(Integer.MAX_VALUE);
        }

        try {
            // Try persisting Lua, because that's what all of the rest depends on.
            // Save the kernel state (which is always at stack index one).
            assert lua.isThread(1);

            SaveHandler.scheduleSave(machine.host(), nbt, machine.node().address() + "_kernel", persistence.persist(1));
            // While in a driver call we have one object on the global stack: either
            // the function to call the driver with, or the result of the call.
            if (stateContains(Machine.State.SynchronizedCall) || stateContains(Machine.State.SynchronizedReturn)) {
                assert stateContains(Machine.State.SynchronizedCall) ? lua.isFunction(2) : lua.isTable(2);
                SaveHandler.scheduleSave(machine.host(), nbt, machine.node().address() + "_stack", persistence.persist(2));
            }

            nbt.putInt("kernelMemory", (int) Math.ceil(kernelMemory / ramScale));

            for (NativeLuaAPI api : apis) {
                api.saveData(nbt);
            }

            try {
                lua.gc(LuaState.GcAction.COLLECT, 0);
            } catch (Throwable t) {
                OpenComputers.log.warn("Error cleaning up loaded computer @ (" + machine.host().xPosition() + ", " + machine.host().yPosition() + ", " + machine.host().zPosition() + "). This either means the server is badly overloaded or a user created an evil __gc method, accidentally or not.");
                machine.crash("error in garbage collector, most likely __gc method timed out");
            }
        } catch (LuaRuntimeException e) {
            OpenComputers.log.warn("Could not persist computer @ (" + machine.host().xPosition() + ", " + machine.host().yPosition() + ", " + machine.host().zPosition() + ").\n" + e + (e.getLuaStackTrace().length == 0 ? "" : "\tat " + luaStackTrace(e, "\n\tat ")));
            nbt.remove("state");
        } catch (LuaGcMetamethodException e) {
            OpenComputers.log.warn("Could not persist computer @ (" + machine.host().xPosition() + ", " + machine.host().yPosition() + ", " + machine.host().zPosition() + ").\n" + e);
            nbt.remove("state");
        }

        // Limit memory again.
        recomputeMemory(machine.host().internalComponents());
    }
}
