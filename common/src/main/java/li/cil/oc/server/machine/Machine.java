package li.cil.oc.server.machine;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.tileentity.traits.Computer;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.ThreadPoolFactory;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Machine extends AbstractManagedEnvironment implements li.cil.oc.api.machine.Machine, Runnable, DeviceInfo {
    public final MachineHost host;

    public final ComponentConnector node;

    public final Optional<ManagedEnvironment> tmp;

    public Architecture architecture;

    // Formerly private[machine]; also used by the native Lua architecture (persistence).
    // Lock on it before access.
    public final ArrayDeque<State> state = new ArrayDeque<>();

    private final Map<String, String> _components = new HashMap<>();

    private final Set<Component> addedComponents = new LinkedHashSet<>();

    private final Set<String> _users = new LinkedHashSet<>();

    private final ArrayDeque<Signal> signals = new ArrayDeque<>();

    public int maxComponents = 0;

    private double maxCallBudget = 1.0;

    private boolean hasMemory = false;

    private volatile double callBudget = 0.0;

    // We want to ignore the call limit in synchronized calls to avoid errors.
    private boolean inSynchronizedCall = false;

    // ----------------------------------------------------------------------- //

    public long worldTime = 0L; // Game-world time for os.time().

    private long uptime = 0L; // Game-world time [ticks] for os.uptime().

    private long cpuTotal = 0L; // Pseudo-real-world time [ns] for os.clock().

    private long cpuStart = 0L; // Pseudo-real-world time [ns] for os.clock().

    private int remainIdle = 0; // Ticks left to sleep before resuming.

    private int remainingPause = 0; // Ticks left to wait before resuming.

    private boolean usersChanged = false; // Send updated users list to clients?

    private Optional<String> message = Optional.empty(); // For error messages.

    private double cost = Settings.get().computerCost * Settings.get().tickFrequency;

    private final int maxSignalQueueSize = Settings.get().maxSignalQueueSize;

    public Machine(MachineHost host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network).
                withComponent("computer", Visibility.Neighbors).
                withConnector(Settings.get().bufferComputer).
                create();
        setNode(this.node);
        if (Settings.get().tmpSize > 0) {
            this.tmp = Optional.ofNullable(li.cil.oc.api.FileSystem.asManagedEnvironment(li.cil.oc.api.FileSystem.
                    fromMemory(Settings.get().tmpSize * 1024L), "tmpfs", null, null, 5));
        } else {
            this.tmp = Optional.empty();
        }
        state.push(State.Stopped);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    @Override
    public MachineHost host() {
        return host;
    }

    @Override
    public Architecture architecture() {
        return architecture;
    }

    @Override
    public int maxComponents() {
        return maxComponents;
    }

    @Override
    public long worldTime() {
        return worldTime;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onHostChanged() {
        final Iterable<ItemStack> components = host.internalComponents();
        int newMaxComponents = 0;
        double budgetSum = 0;
        int budgetCount = 0;
        for (ItemStack stack : components) {
            if (stack == null) continue;
            final DriverItem driver = Driver.driverFor(stack, host.getClass());
            if (driver instanceof Processor processor) newMaxComponents += processor.supportedComponents(stack);
            if (driver instanceof CallBudget budget) {
                budgetSum += budget.getCallBudget(stack);
                budgetCount += 1;
            }
        }
        maxComponents = newMaxComponents;
        maxCallBudget = budgetCount == 0 ? 1.0 : budgetSum / budgetCount;
        Architecture newArchitecture = null;
        for (ItemStack stack : components) {
            if (stack == null) continue;
            final DriverItem driver = Driver.driverFor(stack, host.getClass());
            if (driver instanceof Processor processor && li.cil.oc.api.driver.item.Slot.CPU.equals(processor.slot(stack))) {
                final Class<? extends Architecture> clazz = processor.architecture(stack);
                if (clazz != null) {
                    if (architecture == null || architecture.getClass() != clazz) {
                        try {
                            newArchitecture = clazz.getConstructor(li.cil.oc.api.machine.Machine.class).newInstance(this);
                        } catch (Throwable t) {
                            OpenComputers.log.warn("Failed instantiating a CPU architecture.", t);
                        }
                    } else {
                        newArchitecture = architecture;
                    }
                    break;
                }
            }
        }
        // This needs to operate synchronized against the worker thread, to avoid the
        // architecture changing while it is currently being executed.
        if (newArchitecture != architecture) {
            synchronized (this) {
                architecture = newArchitecture;
                if (architecture != null && node.network() != null) architecture.onConnect();
            }
        }
        hasMemory = architecture != null && architecture.recomputeMemory(components);
    }

    @Override
    public Map<String, String> components() {
        return _components;
    }

    @Override
    public int componentCount() {
        double count = 0;
        synchronized (_components) {
            for (String name : _components.values()) {
                count += !"filesystem".equals(name) ? 1.0 : 0.25;
            }
        }
        for (Component component : addedComponents) {
            count += !"filesystem".equals(component.name()) ? 1 : 0.25;
        }
        return (int) (count - 1); // -1 = this computer
    }

    @Override
    public String tmpAddress() {
        return tmp.map(t -> t.node().address()).orElse(null);
    }

    @Override
    public String lastError() {
        return message.orElse(null);
    }

    @Override
    public void setCostPerTick(double value) {
        cost = value * Settings.get().tickFrequency;
    }

    @Override
    public double getCostPerTick() {
        return cost / Settings.get().tickFrequency;
    }

    @Override
    public String[] users() {
        synchronized (_users) {
            return _users.toArray(new String[0]);
        }
    }

    @Override
    public double upTime() {
        // Convert from old saves (set to -timeStarted on load).
        if (uptime < 0) {
            uptime = worldTime + uptime;
        }
        // World time is in ticks, and each second has 20 ticks. Since we
        // want uptime() to return real seconds, though, we'll divide it
        // accordingly.
        return uptime / 20.0;
    }

    @Override
    public double cpuTime() {
        return (cpuTotal + (System.nanoTime() - cpuStart)) * 10e-10;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Map<String, String> getDeviceInfo() {
        if (host instanceof DeviceInfo deviceInfo) return deviceInfo.getDeviceInfo();
        return null;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canInteract(String player) {
        if (!Settings.get().canComputersBeOwned) return true;
        synchronized (_users) {
            if (_users.isEmpty() || _users.contains(player)) return true;
        }
        final MinecraftServer server = GameInstance.getServer();
        if (server == null || server.isSingleplayer()) return true;
        final PlayerList config = server.getPlayerList();
        final ServerPlayer entity = config.getPlayerByName(player);
        return entity != null && config.isOp(entity.getGameProfile());
    }

    @Override
    public boolean isRunning() {
        synchronized (state) {
            return state.peek() != State.Stopped && state.peek() != State.Stopping;
        }
    }

    @Override
    public boolean isPaused() {
        synchronized (state) {
            return state.peek() == State.Paused && remainingPause > 0;
        }
    }

    @Override
    public boolean start() {
        synchronized (state) {
            final State top = state.peek();
            if (top == State.Stopped && node.network() != null) {
                onHostChanged();
                processAddedComponents();
                verifyComponents();
                if (!Settings.get().ignorePower && node.globalBuffer() < cost) {
                    // No beep! We have no energy after all :P
                    crash("gui.Error.NoEnergy");
                    return false;
                } else if (architecture == null || maxComponents == 0) {
                    beep("-");
                    crash("gui.Error.NoCPU");
                    return false;
                } else if (componentCount() > maxComponents) {
                    beep("-..");
                    crash("gui.Error.ComponentOverflow");
                    return false;
                } else if (!hasMemory) {
                    beep("-.");
                    crash("gui.Error.NoRAM");
                    return false;
                } else if (!init()) {
                    beep("--");
                    return false;
                } else {
                    switchTo(State.Starting);
                    uptime = 0;
                    node.sendToReachable("computer.started");
                    return true;
                }
            } else if (top == State.Paused && remainingPause > 0) {
                remainingPause = 0;
                host.markChanged();
                return true;
            } else if (top == State.Stopping) {
                switchTo(State.Restarting);
                EventHandler.unscheduleClose(this);
                return true;
            }
            return false;
        }
    }

    private boolean shouldPause(State s, int ticksToPause) {
        if (s == State.Stopping || s == State.Stopped) return false;
        if (s == State.Paused && ticksToPause <= remainingPause) return false;
        return true;
    }

    @Override
    public boolean pause(double seconds) {
        final int ticksToPause = Math.max((int) (seconds * 20), 0);
        final State current;
        synchronized (state) {
            current = state.peek();
        }
        if (shouldPause(current, ticksToPause)) {
            // Check again when we get the lock, might have changed since.
            synchronized (this) {
                synchronized (state) {
                    if (shouldPause(state.peek(), ticksToPause)) {
                        if (state.peek() != State.Paused) {
                            assert !state.contains(State.Paused);
                            state.push(State.Paused);
                        }
                        remainingPause = ticksToPause;
                        host.markChanged();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean stop() {
        synchronized (state) {
            final State top = state.peek();
            if (top == State.Stopped || top == State.Stopping) {
                return false;
            }
            state.push(State.Stopping);
            EventHandler.scheduleClose(this);
            return true;
        }
    }

    @Override
    public void consumeCallBudget(double callCost) {
        if (architecture.isInitialized() && !inSynchronizedCall) {
            final double clampedCost = Math.max(0.0, callCost);
            if (clampedCost > callBudget) {
                throw Machine.<RuntimeException>sneakyThrow(new LimitReachedException());
            }
            callBudget -= clampedCost;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public void beep(short frequency, short duration) {
        PacketSender.sendSound(host.world(), host.xPosition(), host.yPosition(), host.zPosition(), frequency, duration);
    }

    @Override
    public void beep(String pattern) {
        PacketSender.sendSound(host.world(), host.xPosition(), host.yPosition(), host.zPosition(), pattern);
    }

    @Override
    public boolean crash(String message) {
        this.message = Optional.ofNullable(message);
        synchronized (state) {
            final boolean result = stop();
            if (state.peek() == State.Stopping) {
                // When crashing, make sure there's no "Running" left in the stack.
                state.clear();
                state.push(State.Stopping);
            }
            return result;
        }
    }

    public Object convertArg(Object param) {
        if (param instanceof Boolean) return param;
        if (param instanceof Character c) return (double) c;
        if (param instanceof Long) return param;
        if (param instanceof Number n) return n.doubleValue();
        if (param instanceof String) return param;
        if (param instanceof byte[]) return param;
        if (param instanceof CompoundTag) return param;
        OpenComputers.log.warn("Trying to push signal with an unsupported argument of type " + (param == null ? "null" : param.getClass().getName()));
        return null;
    }

    @Override
    public boolean signal(String name, Object... args) {
        synchronized (state) {
            final State top = state.peek();
            if (top == State.Stopped || top == State.Stopping) return false;
            synchronized (signals) {
                if (signals.size() >= maxSignalQueueSize) return false;
                else if (args == null) {
                    signals.add(new Signal(name, new Object[0]));
                } else {
                    final Object[] converted = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        final Object arg = args[i];
                        if (arg == null || arg == ResultWrapper.unit || (arg instanceof Optional<?> o && o.isEmpty())) {
                            converted[i] = null;
                        } else if (arg instanceof Map<?, ?> map) {
                            final Map<Object, Object> convertedMap = new HashMap<>();
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                final Object convertedKey = convertArg(entry.getKey());
                                if (convertedKey != null) {
                                    final Object convertedValue = convertArg(entry.getValue());
                                    if (convertedValue != null) {
                                        convertedMap.put(convertedKey, convertedValue);
                                    }
                                }
                            }
                            converted[i] = convertedMap;
                        } else {
                            converted[i] = convertArg(arg);
                        }
                    }
                    signals.add(new Signal(name, converted));
                }
            }
        }

        if (architecture != null) architecture.onSignal();
        return true;
    }

    @Override
    public Signal popSignal() {
        synchronized (signals) {
            final Signal signal = signals.poll();
            return signal == null ? null : signal.convert();
        }
    }

    @Override
    public Map<String, Callback> methods(Object value) {
        final Map<String, Callback> result = new HashMap<>();
        for (Map.Entry<String, Callbacks.Callback> entry : Callbacks.apply(value).entrySet()) {
            result.put(entry.getKey(), entry.getValue().annotation);
        }
        return result;
    }

    @Override
    public Object[] invoke(String address, String method, Object[] args) throws Exception {
        if (node != null && node.network() != null) {
            final Node target = node.network().node(address);
            if (target instanceof Component component && (component.canBeSeenFrom(node) || component == node)) {
                final Callback annotation = component.annotation(method);
                if (annotation.direct()) {
                    consumeCallBudget(1.0 / annotation.limit());
                }
                return component.invoke(method, this, args);
            }
            throw new IllegalArgumentException("no such component");
        } else {
            // Not really, but makes the VM stop, which is what we want in this case,
            // because it means we've been disconnected / disposed already.
            throw new LimitReachedException();
        }
    }

    @Override
    public Object[] invoke(Value value, String method, Object[] args) throws Exception {
        final Callbacks.Callback callback = Callbacks.apply(value).get(method);
        if (callback != null) {
            final Callback annotation = callback.annotation;
            if (annotation.direct()) {
                consumeCallBudget(1.0 / annotation.limit());
            }
            final ArgumentsImpl arguments = new ArgumentsImpl(Arrays.asList(args));
            return Registry.INSTANCE.convert(callback.apply(value, this, arguments));
        }
        throw new NoSuchMethodException();
    }

    @Override
    public void addUser(String name) throws Exception {
        if (_users.size() >= Settings.get().maxUsers)
            throw new Exception("too many users");

        if (_users.contains(name))
            throw new Exception("user exists");
        if (name.length() > Settings.get().maxUsernameLength)
            throw new Exception("username too long");
        final MinecraftServer server = GameInstance.getServer();
        if (server == null || !Arrays.asList(server.getPlayerNames()).contains(name))
            throw new Exception("player must be online");

        synchronized (_users) {
            _users.add(name);
            usersChanged = true;
        }
    }

    @Override
    public boolean removeUser(String name) {
        synchronized (_users) {
            final boolean success = _users.remove(name);
            if (success) {
                usersChanged = true;
            }
            return success;
        }
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():boolean -- Starts the computer. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) {
        return ResultWrapper.result(!isPaused() && start());
    }

    @Callback(doc = "function():boolean -- Stops the computer. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) {
        return ResultWrapper.result(stop());
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the computer is running.")
    public Object[] isRunning(Context context, Arguments args) {
        return ResultWrapper.result(isRunning());
    }

    @Callback(doc = "function([frequency:string or number[, duration:number]]) -- Plays a tone, useful to alert users via audible feedback.")
    public Object[] beep(Context context, Arguments args) {
        if (args.count() == 1 && args.isString(0)) {
            beep(args.checkString(0));
        } else {
            final int frequency = args.optInteger(0, 440);
            if (frequency < 20 || frequency > 2000) {
                throw new IllegalArgumentException("invalid frequency, must be in [20, 2000]");
            }
            final double duration = args.optDouble(1, 0.1);
            final int durationInMilliseconds = Math.max(50, Math.min(5000, (int) (duration * 1000)));
            context.pause(durationInMilliseconds / 1000.0);
            beep((short) frequency, (short) durationInMilliseconds);
        }
        return null;
    }

    @Callback(direct = true, doc = "function():table -- Collect information on all connected devices.")
    public Object[] getDeviceInfo(Context context, Arguments args) {
        context.pause(1); // Iterating all nodes is potentially expensive, and I see no practical reason for having to call this frequently.
        final Map<String, Map<String, String>> result = new HashMap<>();
        for (Node n : node.network().nodes()) {
            if (n.host() instanceof DeviceInfo deviceInfo) {
                final boolean visible = n instanceof Component c
                        ? c.canBeSeenFrom(node) || c == node
                        : n.canBeReachedFrom(node);
                if (visible) {
                    final Map<String, String> info = deviceInfo.getDeviceInfo();
                    if (info != null) result.put(n.address(), info);
                }
            }
        }
        return new Object[]{result};
    }

    @Callback(doc = "function():table -- Returns a map of program name to disk label for known programs.")
    public Object[] getProgramLocations(Context context, Arguments args) {
        return ResultWrapper.result(ProgramLocations.getMappings(Machine.getArchitectureName(architecture.getClass())));
    }

    // ----------------------------------------------------------------------- //

    public boolean isExecuting() {
        synchronized (state) {
            return state.contains(State.Running);
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    private State top() {
        synchronized (state) {
            return state.peek();
        }
    }

    @Override
    public void update() {
        if (top() == State.Stopped) return;

        // Add components that were added since the last update to the actual list
        // of components if we can see them. We use this delayed approach to avoid
        // issues with components that have a visibility lower than their
        // reachability, because in that case if they get connected in the wrong
        // order we wouldn't add them (since they'd be invisible in their connect
        // message, and only become visible with a later node-to-node connection,
        // but that wouldn't trigger a connect message anymore due to the higher
        // reachability).
        processAddedComponents();

        // Component overflow check, crash if too many components are connected, to
        // avoid confusion on the user's side due to components not showing up.
        if (componentCount() > maxComponents) {
            beep("-..");
            crash("gui.Error.ComponentOverflow");
        }

        // Update world time for time() and uptime().
        worldTime = host.world().getDayTime();
        uptime += 1;

        if (remainIdle > 0) {
            remainIdle -= 1;
        }

        // Reset direct call budget.
        callBudget = maxCallBudget;

        // Make sure we have enough power.
        if (host.world().getGameTime() % Settings.get().tickFrequency == 0) {
            synchronized (state) {
                final State top = state.peek();
                if (top == State.Paused || top == State.Restarting || top == State.Stopping || top == State.Stopped) {
                    // No power consumption.
                } else if (top == State.Sleeping && remainIdle > 0 && signals.isEmpty()) {
                    if (!node.tryChangeBuffer(-cost * Settings.get().sleepCostFactor)) {
                        crash("gui.Error.NoEnergy");
                    }
                } else {
                    if (!node.tryChangeBuffer(-cost)) {
                        crash("gui.Error.NoEnergy");
                    }
                }
            }
        }

        // Avoid spamming user list across the network.
        if (host.world().getGameTime() % 20 == 0 && usersChanged) {
            final String[] list;
            synchronized (_users) {
                usersChanged = false;
                list = users();
            }
            if (host instanceof Computer computer) {
                PacketSender.sendComputerUserList(computer, list);
            }
        }

        // Check if we should switch states. These are all the states in which we're
        // guaranteed that the executor thread isn't running anymore.
        final State current = top();
        if (current == State.Starting) {
            // Booting up.
            verifyComponents();
            switchTo(State.Yielded);
        } else if (current == State.Restarting) {
            // Computer is rebooting.
            close();
            if (Settings.get().eraseTmpOnReboot) {
                tmp.ifPresent(t -> t.node().remove()); // To force deleting contents.
                tmp.ifPresent(t -> node.connect(t.node()));
            }
            node.sendToReachable("computer.stopped");
            start();
        } else if (current == State.Sleeping && (remainIdle <= 0 || !signals.isEmpty())) {
            // Resume from pauses based on sleep or signal underflow.
            switchTo(State.Yielded);
        } else if (current == State.Paused) {
            // Resume in case we paused  because the game was paused.
            if (remainingPause > 0) {
                remainingPause -= 1;
            } else {
                verifyComponents(); // In case we're resuming after loading.
                state.pop();
                switchTo(state.peek()); // Trigger execution if necessary.
            }
        } else if (current == State.SynchronizedCall) {
            // Perform a synchronized call (message sending).
            // We switch into running state, since we'll behave as though the call
            // were performed from our executor thread.
            switchTo(State.Running);
            try {
                inSynchronizedCall = true;
                architecture.runSynchronized();
                inSynchronizedCall = false;
                // Check if the callback called pause() or stop().
                final State after = state.peek();
                if (after == State.Running) {
                    switchTo(State.SynchronizedReturn);
                } else if (after == State.Paused) {
                    state.pop(); // Paused
                    state.pop(); // Running, no switchTo to avoid new future.
                    state.push(State.SynchronizedReturn);
                    state.push(State.Paused);
                } else if (after == State.Stopping) {
                    state.clear();
                    state.push(State.Stopping);
                } else {
                    throw new AssertionError();
                }
            } catch (Error e) {
                if ("not enough memory".equals(e.getMessage())) {
                    crash("gui.Error.OutOfMemory");
                } else {
                    OpenComputers.log.warn("Faulty architecture implementation for synchronized calls.", e);
                    crash("gui.Error.InternalError");
                }
            } catch (Throwable e) {
                OpenComputers.log.warn("Faulty architecture implementation for synchronized calls.", e);
                crash("gui.Error.InternalError");
            } finally {
                inSynchronizedCall = false;
            }
        }

        // Finally check if we should stop the computer. We cannot lock the state
        // because we may have to wait for the executor thread to finish, which
        // might turn into a deadlock depending on where it currently is.
        if (top() == State.Stopping) {
            // Computer is shutting down.
            synchronized (this) {
                synchronized (state) {
                    tryClose();
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onMessage(Message message) {
        final Object[] data = message.data();
        if ("computer.signal".equals(message.name()) && data.length >= 1 && data[0] instanceof String name) {
            signal(name, prependAddress(message.source().address(), data, 1));
        } else if ("computer.checked_signal".equals(message.name()) && data.length >= 2 && data[0] instanceof Player player && data[1] instanceof String name) {
            if (canInteract(player.getName().getString()))
                signal(name, prependAddress(message.source().address(), data, 2));
        } else {
            if ("computer.start".equals(message.name()) && !isPaused()) start();
            else if ("computer.stop".equals(message.name())) stop();
        }
    }

    private static Object[] prependAddress(String address, Object[] data, int offset) {
        final Object[] result = new Object[data.length - offset + 1];
        result[0] = address;
        System.arraycopy(data, offset, result, 1, data.length - offset);
        return result;
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            synchronized (_components) {
                _components.put(this.node.address(), this.node.name());
            }
            tmp.ifPresent(fs -> node.connect(fs.node()));
            if (architecture != null) architecture.onConnect();
        } else {
            if (node instanceof Component component) addComponent(component);
        }
        // For computers, to generate the components in their inventory.
        host.onMachineConnect(node);
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == this.node) {
            close();
            tmp.ifPresent(fs -> fs.node().remove());
        } else {
            if (node instanceof Component component) removeComponent(component);
        }
        // For computers, to save the components in their inventory.
        host.onMachineDisconnect(node);
    }

    // ----------------------------------------------------------------------- //

    public void addComponent(Component component) {
        if (!_components.containsKey(component.address())) {
            addedComponents.add(component);
        }
    }

    public void removeComponent(Component component) {
        if (_components.containsKey(component.address())) {
            synchronized (_components) {
                _components.remove(component.address());
            }
            signal("component_removed", component.address(), component.name());
        }
        addedComponents.remove(component);
    }

    private void processAddedComponents() {
        if (!addedComponents.isEmpty()) {
            for (Component component : new ArrayList<>(addedComponents)) {
                if (component.canBeSeenFrom(node)) {
                    synchronized (_components) {
                        _components.put(component.address(), component.name());
                    }
                    // Skip the signal if we're not initialized yet, since we'd generate a
                    // duplicate in the startup script otherwise.
                    if (architecture != null && architecture.isInitialized()) {
                        signal("component_added", component.address(), component.name());
                    }
                }
            }
            addedComponents.clear();
        }
    }

    private void verifyComponents() {
        final Set<String> invalid = new HashSet<>();
        for (Map.Entry<String, String> entry : new ArrayList<>(_components.entrySet())) {
            final String address = entry.getKey();
            final String name = entry.getValue();
            final Node target = node.network().node(address);
            if (target instanceof Component component && name.equals(component.name())) {
                // All is well.
                continue;
            }
            if ("filesystem".equals(name)) {
                OpenComputers.log.trace("A component of type '" + name + "' disappeared (" + address + ")! This usually means that it didn't save its node.");
                OpenComputers.log.trace("If this was a file system provided by a ComputerCraft peripheral, this is normal.");
            } else
                OpenComputers.log.warn("A component of type '" + name + "' disappeared (" + address + ")! This usually means that it didn't save its node.");
            signal("component_removed", address, name);
            invalid.add(address);
        }
        synchronized (_components) {
            for (String address : invalid) {
                _components.remove(address);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private String tmpPath() {
        return node.address() + "_tmp";
    }

    private static final String StateTag = "state";
    private static final String UsersTag = "users";
    private static final String MessageTag = "message";
    private static final String ComponentsTag = "components";
    private static final String AddressTag = "address";
    private static final String NameTag = "name";
    private static final String TmpTag = "tmp";
    private static final String SignalsTag = "signals";
    private static final String ArgsTag = "args";
    private static final String LengthTag = "length";
    private static final String ArgPrefixTag = "arg";
    private static final String UptimeTag = "uptime";
    private static final String CPUTimeTag = "cpuTime";
    private static final String RemainingPauseTag = "remainingPause";

    @Override
    public void loadData(CompoundTag nbt) {
        synchronized (this) {
            synchronized (state) {
                assert state.peek() == State.Stopped || state.peek() == State.Paused;
                close();
                state.clear();

                super.loadData(nbt);

                final int[] states = nbt.getIntArray(StateTag);
                for (int i = states.length - 1; i >= 0; i--) {
                    state.push(State.values()[states[i]]);
                }
                final ListTag usersNbt = nbt.getList(UsersTag, Tag.TAG_STRING);
                for (int i = 0; i < usersNbt.size(); i++) {
                    _users.add(usersNbt.getString(i));
                }
                if (nbt.contains(MessageTag)) {
                    message = Optional.of(nbt.getString(MessageTag));
                }

                final ListTag componentsNbt = nbt.getList(ComponentsTag, Tag.TAG_COMPOUND);
                synchronized (_components) {
                    for (int i = 0; i < componentsNbt.size(); i++) {
                        final CompoundTag tag = componentsNbt.getCompound(i);
                        _components.put(tag.getString(AddressTag), tag.getString(NameTag));
                    }
                }

                tmp.ifPresent(fs -> {
                    if (nbt.contains(TmpTag)) fs.loadData(nbt.getCompound(TmpTag));
                    else fs.loadData(SaveHandler.loadNBT(nbt, tmpPath()));
                });

                if (!state.isEmpty() && isRunning() && init()) {
                    try {
                        architecture.loadData(nbt);

                        final ListTag signalsNbt = nbt.getList(SignalsTag, Tag.TAG_COMPOUND);
                        for (int s = 0; s < signalsNbt.size(); s++) {
                            final CompoundTag signalNbt = signalsNbt.getCompound(s);
                            final CompoundTag argsNbt = signalNbt.getCompound(ArgsTag);
                            final int argsLength = argsNbt.getInt(LengthTag);
                            final Object[] args = new Object[argsLength];
                            for (int i = 0; i < argsLength; i++) {
                                final Tag tag = argsNbt.get(ArgPrefixTag + i);
                                if (tag instanceof ByteTag b && b.getAsByte() == -1) args[i] = null;
                                else if (tag instanceof ByteTag b) args[i] = b.getAsByte() == 1;
                                else if (tag instanceof LongTag l) args[i] = l.getAsLong();
                                else if (tag instanceof DoubleTag d) args[i] = d.getAsDouble();
                                else if (tag instanceof StringTag str) args[i] = str.getAsString();
                                else if (tag instanceof ByteArrayTag ba) args[i] = ba.getAsByteArray();
                                else if (tag instanceof ListTag list) {
                                    final Map<String, String> data = new HashMap<>();
                                    for (int j = 0; j + 1 < list.size(); j += 2) {
                                        data.put(list.getString(j), list.getString(j + 1));
                                    }
                                    args[i] = data;
                                } else if (tag instanceof CompoundTag compound) args[i] = compound;
                                else args[i] = null;
                            }
                            signals.add(new Signal(signalNbt.getString(NameTag), args));
                        }

                        uptime = nbt.getLong(UptimeTag);
                        cpuTotal = nbt.getLong(CPUTimeTag);
                        remainingPause = nbt.getInt(RemainingPauseTag);

                        // Delay execution for a second to allow the world around us to settle.
                        if (state.peek() != State.Restarting) {
                            pause(Settings.get().startupDelay);
                        }
                    } catch (Throwable t) {
                        OpenComputers.log.error(
                                "Unexpected error loading a state of computer at (" + host.xPosition() + ", " + host.yPosition() + ", " + host.zPosition() + "). " +
                                        "State: " + (state.isEmpty() ? "no state" : state.peek().toString()) + ". Unless you're upgrading/downgrading across a major version, please report this! Thank you.", t);
                        close();
                    }
                } else {
                    // Clean up in case we got a weird state stack.
                    onHostChanged();
                    close();
                }
            }
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        synchronized (this) {
            synchronized (state) {
                // The lock on 'this' should guarantee that this never happens regularly.
                // If something other than regular saving tries to save while we are executing code,
                // e.g. SpongeForge saving during robot.move due to block changes being captured,
                // just don't save this at all. What could possibly go wrong?
                if (isExecuting()) return;

                if (SaveHandler.savingForClients) {
                    return;
                }

                // Make sure we don't continue running until everything has saved.
                pause(0.05);

                super.saveData(nbt);

                // Make sure the component list is up-to-date.
                processAddedComponents();

                final int[] states = new int[state.size()];
                int index = 0;
                for (State s : state) states[index++] = s.ordinal();
                nbt.putIntArray(StateTag, states);
                synchronized (_users) {
                    ExtendedNBT.setNewTagList(nbt, UsersTag, ExtendedNBT.stringIterableToNbt(_users));
                }
                message.ifPresent(m -> nbt.putString(MessageTag, m));

                final ListTag componentsNbt = new ListTag();
                synchronized (_components) {
                    for (Map.Entry<String, String> entry : _components.entrySet()) {
                        final CompoundTag componentNbt = new CompoundTag();
                        componentNbt.putString(AddressTag, entry.getKey());
                        componentNbt.putString(NameTag, entry.getValue());
                        componentsNbt.add(componentNbt);
                    }
                }
                nbt.put(ComponentsTag, componentsNbt);

                tmp.ifPresent(fs -> SaveHandler.scheduleSave(host, nbt, tmpPath(), fs::saveData));

                if (state.peek() != State.Stopped) {
                    try {
                        architecture.saveData(nbt);

                        final ListTag signalsNbt = new ListTag();
                        for (Signal s : signals) {
                            final CompoundTag signalNbt = new CompoundTag();
                            signalNbt.putString(NameTag, s.name);
                            ExtendedNBT.setNewCompoundTag(signalNbt, ArgsTag, args -> {
                                args.putInt(LengthTag, s.args.length);
                                for (int i = 0; i < s.args.length; i++) {
                                    final Object arg = s.args[i];
                                    final String key = ArgPrefixTag + i;
                                    if (arg == null) args.putByte(key, (byte) -1);
                                    else if (arg instanceof Boolean b) args.putByte(key, (byte) (b ? 1 : 0));
                                    else if (arg instanceof Long l) args.putLong(key, l);
                                    else if (arg instanceof Double d) args.putDouble(key, d);
                                    else if (arg instanceof String str) args.putString(key, str);
                                    else if (arg instanceof byte[] ba) args.putByteArray(key, ba);
                                    else if (arg instanceof Map<?, ?> map) {
                                        final ListTag list = new ListTag();
                                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                                            list.add(StringTag.valueOf(String.valueOf(entry.getKey())));
                                            list.add(StringTag.valueOf(String.valueOf(entry.getValue())));
                                        }
                                        args.put(key, list);
                                    } else if (arg instanceof CompoundTag compound) args.put(key, compound);
                                    else args.putByte(key, (byte) -1);
                                }
                            });
                            signalsNbt.add(signalNbt);
                        }
                        nbt.put(SignalsTag, signalsNbt);

                        nbt.putLong(UptimeTag, uptime);
                        nbt.putLong(CPUTimeTag, cpuTotal);
                        nbt.putInt(RemainingPauseTag, remainingPause);
                    } catch (Throwable t) {
                        OpenComputers.log.error(
                                "Unexpected error saving a state of computer at (" + host.xPosition() + ", " + host.yPosition() + ", " + host.zPosition() + "). " +
                                        "State: " + (state.isEmpty() ? "no state" : state.peek().toString()) + ". Unless you're upgrading/downgrading across a major version, please report this! Thank you.", t);
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private boolean init() {
        onHostChanged();
        if (architecture == null) return false;

        // Reset error state.
        message = Optional.empty();

        // Clear any left-over signals from a previous run.
        synchronized (signals) {
            signals.clear();
        }

        // Connect the `/tmp` node to our owner. We're not in a network in
        // case we're loading, which is why we have to check it here.
        if (node.network() != null) {
            tmp.ifPresent(fs -> node.connect(fs.node()));
        }

        try {
            return architecture.initialize();
        } catch (Throwable ex) {
            OpenComputers.log.warn("Failed initializing computer.", ex);
            close();
        }
        return false;
    }

    public boolean tryClose() {
        if (isExecuting()) return false;
        close();
        tmp.ifPresent(t -> t.node().remove()); // To force deleting contents.
        if (node.network() != null) {
            tmp.ifPresent(t -> node.connect(t.node()));
        }
        node.sendToReachable("computer.stopped");
        return true;
    }

    private void close() {
        final boolean shouldClose;
        synchronized (state) {
            shouldClose = state.isEmpty() || state.peek() != State.Stopped;
        }
        if (shouldClose) {
            // Give up the state lock, then get the more generic lock on this instance first
            // before locking on state again. Always must be in that order to avoid deadlocks.
            synchronized (this) {
                synchronized (state) {
                    state.clear();
                    state.push(State.Stopped);
                    if (architecture != null) architecture.close();
                    synchronized (signals) {
                        signals.clear();
                    }
                    uptime = 0;
                    cpuTotal = 0;
                    cpuStart = 0;
                    remainIdle = 0;
                }
            }

            // Mark state change in owner, to send it to clients.
            host.markChanged();
        }
    }

    // ----------------------------------------------------------------------- //

    private State switchTo(State value) {
        synchronized (state) {
            final State result = state.pop();
            if (value == State.Stopping || value == State.Restarting) {
                state.clear();
            }
            state.push(value);
            if (value == State.Yielded || value == State.SynchronizedReturn) {
                remainIdle = 0;
                threadPool.schedule(this, Settings.get().executionDelay, TimeUnit.MILLISECONDS);
            }

            // Mark state change in owner, to send it to clients.
            host.markChanged();

            return result;
        }
    }

    private boolean isGamePaused() {
        final MinecraftServer server = GameInstance.getServer();
        return server != null && !server.isDedicatedServer() &&
                Platform.getEnvironment() == Env.CLIENT &&
                li.cil.oc.client.GamePause.isPaused();
    }

    // This is a really high level lock that we only use for saving and loading.
    @Override
    public void run() {
        synchronized (this) {
            final boolean isSynchronizedReturn;
            synchronized (state) {
                if (state.peek() != State.Yielded &&
                        state.peek() != State.SynchronizedReturn) {
                    return;
                }
                // See if the game appears to be paused, in which case we also pause.
                if (isGamePaused()) {
                    state.push(State.Paused);
                    return;
                }
                isSynchronizedReturn = switchTo(State.Running) == State.SynchronizedReturn;
            }

            cpuStart = System.nanoTime();

            try {
                final ExecutionResult result = architecture.runThreaded(isSynchronizedReturn);

                // Check if someone called pause() or stop() in the meantime.
                synchronized (state) {
                    final State top = state.peek();
                    if (top == State.Running) {
                        if (result instanceof ExecutionResult.Sleep sleep) {
                            synchronized (signals) {
                                // Immediately check for signals to allow processing more than one
                                // signal per game tick.
                                if (signals.isEmpty() && sleep.ticks > 0) {
                                    switchTo(State.Sleeping);
                                    remainIdle = sleep.ticks;
                                } else {
                                    switchTo(State.Yielded);
                                }
                            }
                        } else if (result instanceof ExecutionResult.SynchronizedCall) {
                            switchTo(State.SynchronizedCall);
                        } else if (result instanceof ExecutionResult.Shutdown shutdown) {
                            if (shutdown.reboot) {
                                switchTo(State.Restarting);
                            } else {
                                switchTo(State.Stopping);
                            }
                        } else if (result instanceof ExecutionResult.Error error) {
                            beep("--");
                            crash(error.message != null ? error.message : "unknown error");
                        } else {
                            throw new AssertionError("Unknown execution result.");
                        }
                    } else if (top == State.Paused) {
                        state.pop(); // Paused
                        state.pop(); // Running, no switchTo to avoid new future.
                        if (result instanceof ExecutionResult.Sleep sleep) {
                            remainIdle = sleep.ticks;
                            state.push(State.Sleeping);
                        } else if (result instanceof ExecutionResult.SynchronizedCall) {
                            state.push(State.SynchronizedCall);
                        } else if (result instanceof ExecutionResult.Shutdown shutdown) {
                            if (shutdown.reboot) {
                                state.push(State.Restarting);
                            } else {
                                state.push(State.Stopping);
                            }
                        } else if (result instanceof ExecutionResult.Error error) {
                            crash(error.message != null ? error.message : "unknown error");
                        }
                        state.push(State.Paused);
                    } else if (top == State.Stopping) {
                        state.clear();
                        state.push(State.Stopping);
                    } else if (top == State.Restarting) {
                        // Nothing to do!
                    } else {
                        throw new AssertionError("Invalid state in executor post-processing.");
                    }
                    assert !isExecuting();
                }
            } catch (Throwable e) {
                OpenComputers.log.warn("Architecture's runThreaded threw an error. This should never happen!", e);
                crash("gui.Error.InternalError");
            }

            // Keep track of time spent executing the computer.
            cpuTotal += System.nanoTime() - cpuStart;
        }
    }

    // ----------------------------------------------------------------------- //
    // Former companion object (implements MachineAPI via INSTANCE).

    // Keep registration order, to allow deterministic iteration of the architectures.
    public static final Set<Class<? extends Architecture>> checked = Collections.synchronizedSet(new LinkedHashSet<>());

    public static void add(Class<? extends Architecture> architecture) {
        if (!checked.contains(architecture)) {
            try {
                architecture.getConstructor(li.cil.oc.api.machine.Machine.class);
            } catch (Throwable t) {
                throw new IllegalArgumentException("Architecture does not have required constructor.", t);
            }
            checked.add(architecture);
        }
    }

    public static List<Class<? extends Architecture>> architectures() {
        synchronized (checked) {
            return new ArrayList<>(checked);
        }
    }

    public static String getArchitectureName(Class<? extends Architecture> architecture) {
        final Architecture.Name annotation = architecture.getAnnotation(Architecture.Name.class);
        if (annotation != null) return annotation.value();
        return architecture.getSimpleName();
    }

    public static Machine create(MachineHost host) {
        return new Machine(host);
    }

    /**
     * The {@link MachineAPI} implementation, to be assigned to {@code api.API.machine}.
     */
    public static final MachineAPI INSTANCE = new MachineAPI() {
        @Override
        public void add(Class<? extends Architecture> architecture) {
            Machine.add(architecture);
        }

        @Override
        public Collection<Class<? extends Architecture>> architectures() {
            return Machine.architectures();
        }

        @Override
        public String getArchitectureName(Class<? extends Architecture> architecture) {
            return Machine.getArchitectureName(architecture);
        }

        @Override
        public li.cil.oc.api.machine.Machine create(MachineHost host) {
            return Machine.create(host);
        }
    };

    /**
     * Possible states of the computer, and in particular its executor.
     * The ordinal is persisted, so never reorder these.
     */
    public enum State {
        /**
         * The computer is not running right now and there is no Lua state.
         */
        Stopped,

        /**
         * Booting up, doing the first run to initialize the kernel and libs.
         */
        Starting,

        /**
         * Computer is currently rebooting.
         */
        Restarting,

        /**
         * The computer is currently shutting down.
         */
        Stopping,

        /**
         * The computer is paused and waiting for the game to resume.
         */
        Paused,

        /**
         * The computer executor is waiting for a synchronized call to be made.
         */
        SynchronizedCall,

        /**
         * The computer should resume with the result of a synchronized call.
         */
        SynchronizedReturn,

        /**
         * The computer will resume as soon as possible.
         */
        Yielded,

        /**
         * The computer is yielding for a longer amount of time.
         */
        Sleeping,

        /**
         * The computer is up and running, executing Lua code.
         */
        Running
    }

    /**
     * Signals are messages sent to the Lua state from Java asynchronously.
     */
    public static class Signal implements li.cil.oc.api.machine.Signal {
        public final String name;
        public final Object[] args;

        Signal(String name, Object[] args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Object[] args() {
            return args;
        }

        Signal convert() {
            return new Signal(name, Registry.INSTANCE.convert(args));
        }
    }

    private static final ScheduledExecutorService threadPool = ThreadPoolFactory.create("Computer", Settings.get().threads);
}
