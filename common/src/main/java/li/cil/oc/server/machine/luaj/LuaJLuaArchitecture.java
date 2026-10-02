package li.cil.oc.server.machine.luaj;

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
import li.cil.oc.server.machine.Machine;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.Globals;
import li.cil.repack.org.luaj.vm2.LuaError;
import li.cil.repack.org.luaj.vm2.LuaFunction;
import li.cil.repack.org.luaj.vm2.LuaThread;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.jse.JsePlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

@Architecture.Name("LuaJ")
public class LuaJLuaArchitecture implements Architecture {
    public final li.cil.oc.api.machine.Machine machine;

    Globals lua;

    private LuaThread thread;

    private LuaFunction synchronizedCall;

    private LuaValue synchronizedResult;

    private boolean doneWithInitRun = false;

    int memory = 0;

    private final LuaJAPI[] apis;

    public LuaJLuaArchitecture(li.cil.oc.api.machine.Machine machine) {
        this.machine = machine;
        this.apis = new LuaJAPI[]{
                new ComponentAPI(this),
                new ComputerAPI(this),
                new OSAPI(this),
                new SystemAPI(this),
                new UnicodeAPI(this),
                new UserdataAPI(this)};
    }

    Varargs invoke(Callable<Object[]> f) {
        try {
            final Object[] results = f.call();
            if (results != null) {
                final LuaValue[] values = new LuaValue[results.length + 1];
                values[0] = LuaValue.TRUE;
                for (int i = 0; i < results.length; i++) values[i + 1] = ScalaClosure.toLuaValue(results[i]);
                return LuaValue.varargsOf(values);
            }
            return LuaValue.TRUE;
        } catch (Throwable e) {
            if (Settings.get().logLuaCallbackErrors && !(e instanceof LimitReachedException)) {
                OpenComputers.log.warn("Exception in Lua callback.", e);
            }
            if (e instanceof LimitReachedException) {
                return LuaValue.NONE;
            } else if (e instanceof IllegalArgumentException && e.getMessage() != null) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(e.getMessage()));
            } else if (e.getMessage() != null) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf(e.getMessage()));
            } else if (e instanceof IndexOutOfBoundsException) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("index out of bounds"));
            } else if (e instanceof IllegalArgumentException) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("bad argument"));
            } else if (e instanceof NoSuchMethodException) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("no such method"));
            } else if (e instanceof FileNotFoundException) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("file not found"));
            } else if (e instanceof SecurityException) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("access denied"));
            } else if (e instanceof IOException) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("i/o error"));
            } else {
                OpenComputers.log.warn("Unexpected error in Lua callback.", e);
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("unknown error"));
            }
        }
    }

    Varargs documentation(Callable<String> f) {
        try {
            final String doc = f.call();
            if (Strings.isNullOrEmpty(doc)) return LuaValue.NIL;
            return LuaValue.valueOf(doc);
        } catch (NoSuchMethodException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such method"));
        } catch (Throwable t) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(t.getMessage() != null ? t.getMessage() : t.toString()));
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isInitialized() {
        return doneWithInitRun;
    }

    @Override
    public boolean recomputeMemory(Iterable<ItemStack> components) {
        memory = memoryInBytes(components);
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
        synchronizedResult = synchronizedCall.call();
        synchronizedCall = null;
    }

    @Override
    public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
        try {
            // Resume the Lua state and remember the number of results we get.
            final Varargs results;
            if (isSynchronizedReturn) {
                // If we were doing a synchronized call, continue where we left off.
                results = thread.resume(synchronizedResult);
                synchronizedResult = null;
            } else {
                if (!doneWithInitRun) {
                    // We're doing the initialization run.
                    final Varargs result = thread.resume(LuaValue.NONE);
                    // Mark as done *after* we ran, to avoid switching to synchronized
                    // calls when we actually need direct ones in the init phase.
                    doneWithInitRun = true;
                    // We expect to get nothing here, if we do we had an error.
                    if (result.narg() != 1) {
                        results = result;
                    } else {
                        // Fake zero sleep to avoid stopping if there are no signals.
                        results = LuaValue.varargsOf(LuaValue.TRUE, LuaValue.valueOf(0));
                    }
                } else {
                    final Signal signal = machine.popSignal();
                    if (signal != null) {
                        final Object[] args = signal.args();
                        final LuaValue[] values = new LuaValue[args.length + 1];
                        values[0] = LuaValue.valueOf(signal.name());
                        for (int i = 0; i < args.length; i++) values[i + 1] = ScalaClosure.toLuaValue(args[i]);
                        results = thread.resume(LuaValue.varargsOf(values));
                    } else {
                        results = thread.resume(LuaValue.NONE);
                    }
                }
            }

            // Check if the kernel is still alive.
            if (thread.state.status == LuaThread.STATUS_SUSPENDED) {
                // If we get one function it must be a wrapper for a synchronized
                // call. The protocol is that a closure is pushed that is then called
                // from the main server thread, and returns a table, which is in turn
                // passed to the originating coroutine.yield().
                if (results.narg() == 2 && results.isfunction(2)) {
                    synchronizedCall = results.checkfunction(2);
                    return new ExecutionResult.SynchronizedCall();
                }
                // Check if we are shutting down, and if so if we're rebooting. This
                // is signalled by boolean values, where `false` means shut down,
                // `true` means reboot (i.e shutdown then start again).
                else if (results.narg() == 2 && results.type(2) == LuaValue.TBOOLEAN) {
                    return new ExecutionResult.Shutdown(results.toboolean(2));
                } else {
                    // If we have a single number, that's how long we may wait before
                    // resuming the state again. Note that the sleep may be interrupted
                    // early if a signal arrives in the meantime. If we have something
                    // else we just process the next signal or wait for one.
                    final int ticks = results.narg() == 2 && results.isnumber(2) ? (int) (results.todouble(2) * 20) : Integer.MAX_VALUE;
                    return new ExecutionResult.Sleep(ticks);
                }
            }
            // The kernel thread returned. If it threw we'd be in the catch below.
            else {
                // This is a little... messy because we run a pcall inside the kernel
                // to be able to catch errors before JNLua gets its claws on them. So
                // we can either have (boolean, string | error) if the main kernel
                // fails, or (boolean, boolean, string | error) if something inside
                // that pcall goes bad.
                final boolean isInnerError = results.type(2) == LuaValue.TBOOLEAN && (results.isstring(3) || results.isnoneornil(3));
                final boolean isOuterError = results.isstring(2) || results.isnoneornil(2);
                if (results.type(1) != LuaValue.TBOOLEAN || !isInnerError || !isOuterError) {
                    OpenComputers.log.warn("Kernel returned unexpected results.");
                }
                // The pcall *should* never return normally... but check for it nonetheless.
                if ((isOuterError && results.toboolean(1)) || (isInnerError && results.toboolean(2))) {
                    OpenComputers.log.warn("Kernel stopped unexpectedly.");
                    return new ExecutionResult.Shutdown(false);
                } else {
                    final String error;
                    if (isInnerError) {
                        if (results.isuserdata(3)) error = results.touserdata(3).toString();
                        else error = results.tojstring(3);
                    } else if (results.isuserdata(2)) error = results.touserdata(2).toString();
                    else error = results.tojstring(2);
                    if (error != null) return new ExecutionResult.Error(error);
                    else return new ExecutionResult.Error("unknown error");
                }
            }
        } catch (LuaError e) {
            OpenComputers.log.warn("Kernel crashed. This is a bug!", e);
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        } catch (Throwable e) {
            OpenComputers.log.warn("Unexpected error in kernel. This is a bug!", e);
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        }
    }

    @Override
    public void onSignal() {
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean initialize() {
        lua = JsePlatform.debugGlobals();
        lua.set("package", LuaValue.NIL);
        lua.set("require", LuaValue.NIL);
        lua.set("io", LuaValue.NIL);
        lua.set("os", LuaValue.NIL);
        lua.set("luajava", LuaValue.NIL);

        // Remove some other functions we don't need and are dangerous.
        lua.set("dofile", LuaValue.NIL);
        lua.set("loadfile", LuaValue.NIL);

        for (LuaJAPI api : apis) api.initialize();

        recomputeMemory(machine.host().internalComponents());

        final LuaValue kernel = lua.load(Machine.class.getResourceAsStream(Settings.scriptPath + "machine.lua"), "=machine", "t", lua);
        thread = new LuaThread(lua, kernel); // Left as the first value on the stack.

        return true;
    }

    @Override
    public void onConnect() {
    }

    @Override
    public void close() {
        lua = null;
        thread = null;
        synchronizedCall = null;
        synchronizedResult = null;
        doneWithInitRun = false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadData(CompoundTag nbt) {
        if (machine.isRunning()) {
            machine.stop();
            machine.start();
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
    }
}
