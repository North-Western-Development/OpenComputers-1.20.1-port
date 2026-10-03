package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.methods.PeripheralMethod;
import dev.architectury.utils.GameInstance;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.BlacklistedPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.Reflection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Lets OC adapters use CC peripherals (CC's own blocks such as monitors, disk drives, speakers,
 * printers, and other mods' peripherals) as components.
 */
public final class DriverPeripheral implements li.cil.oc.api.driver.DriverBlock {
    private static Set<Class<?>> blacklist;

    private boolean isBlacklisted(final Object o) {
        // Check for our interface first, as that has priority.
        if (o instanceof BlacklistedPeripheral) {
            return ((BlacklistedPeripheral) o).isPeripheralBlacklisted();
        }

        // Delayed initialization of the resolved classes to allow registering
        // additional entries via IMC.
        if (blacklist == null) {
            final Set<Class<?>> classes = new HashSet<>();
            for (String name : Settings.get().peripheralBlacklist) {
                final Class<?> clazz = Reflection.getClass(name);
                if (clazz != null) {
                    classes.add(clazz);
                }
            }
            blacklist = classes;
        }
        for (Class<?> clazz : blacklist) {
            if (clazz.isInstance(o))
                return true;
        }
        return false;
    }

    private IPeripheral findPeripheral(final Level world, final BlockPos pos, final Direction side) {
        try {
            final IPeripheral p = ComputerCraftPlatform.getPeripheral(world, pos, side);
            if (p != null && !(p instanceof RelayPeripheral) && !isBlacklisted(p)) {
                return p;
            }
        } catch (Exception e) {
            OpenComputers.log.warn(String.format("Error accessing ComputerCraft peripheral @ (%d, %d, %d).", pos.getX(), pos.getY(), pos.getZ()), e);
        }
        return null;
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        if (world.isClientSide) return false;
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity != null
                // This ensures we don't get duplicate components, in case the
                // tile entity is natively compatible with OpenComputers.
                && !(tileEntity instanceof li.cil.oc.api.network.Environment)
                && !(tileEntity instanceof li.cil.oc.api.network.SidedEnvironment)
                // The black list is used to avoid peripherals that are known
                // to be incompatible with OpenComputers when used directly.
                && !isBlacklisted(tileEntity)
                // Actual check if it's a peripheral.
                && findPeripheral(world, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        final IPeripheral peripheral = findPeripheral(world, pos, side);
        return peripheral != null ? new Environment(peripheral) : null;
    }

    public static class Environment extends li.cil.oc.api.prefab.AbstractManagedEnvironment implements li.cil.oc.api.network.ManagedPeripheral, NamedBlock {
        protected final IPeripheral peripheral;

        protected final Map<String, PeripheralMethod> methods;
        protected final String[] methodNames;

        protected final Map<String, FakeComputerAccess> accesses = new HashMap<>();

        public Environment(final IPeripheral peripheral) {
            this.peripheral = peripheral;
            final MinecraftServer server = GameInstance.getServer();
            methods = server != null
                    ? ComputerCraftPlatform.peripheralMethods(server).getSelfMethods(peripheral)
                    : Collections.emptyMap();
            methodNames = methods.keySet().stream().sorted().toArray(String[]::new);
            setNode(Network.newNode(this, Visibility.Network).create());
        }

        @Override
        public String[] methods() {
            return methodNames;
        }

        @Override
        public Object[] invoke(final String name, final Context context, final Arguments args) throws Exception {
            final Object[] argArray = CallableHelper.convertArguments(args);
            final PeripheralMethod method = methods.get(name);
            if (method == null) throw new NoSuchMethodException();
            FakeComputerAccess access;
            synchronized (accesses) {
                access = accesses.get(context.node().address());
            }
            if (access == null) {
                // The calling contexts is not visible to us, meaning we never got
                // an onConnect for it. Create a temporary access.
                access = new FakeComputerAccess(this, context);
            }
            return CallableHelper.unwrapResult(method.apply(peripheral, new CallableHelper.LuaContext(context), access, new ObjectArguments(argArray)));
        }

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);
            if (node.host() instanceof Context) {
                final FakeComputerAccess access;
                synchronized (accesses) {
                    if (accesses.containsKey(node.address())) return;
                    access = new FakeComputerAccess(this, (Context) node.host());
                    accesses.put(node.address(), access);
                }
                peripheral.attach(access);
            }
        }

        @Override
        public void onDisconnect(final Node node) {
            super.onDisconnect(node);
            if (node.host() instanceof Context) {
                final FakeComputerAccess access;
                synchronized (accesses) {
                    access = accesses.remove(node.address());
                }
                if (access != null) {
                    peripheral.detach(access);
                    access.close();
                }
            } else if (node == this.node()) {
                final Map<String, FakeComputerAccess> copy;
                synchronized (accesses) {
                    copy = new HashMap<>(accesses);
                    accesses.clear();
                }
                for (FakeComputerAccess access : copy.values()) {
                    peripheral.detach(access);
                    access.close();
                }
            }
        }

        @Override
        public String preferredName() {
            return peripheral.getType();
        }

        @Override
        public int priority() {
            return -1; // Lower than 'real' OC components
        }

        /**
         * Map interaction with the computer to our format as good as we can.
         */
        public static class FakeComputerAccess implements IComputerAccess {
            protected final Environment owner;
            protected final Context context;
            protected final Map<String, ManagedEnvironment> fileSystems = new HashMap<>();

            public FakeComputerAccess(final Environment owner, final Context context) {
                this.owner = owner;
                this.context = context;
            }

            public synchronized void close() {
                for (ManagedEnvironment fileSystem : fileSystems.values()) {
                    fileSystem.node().remove();
                }
                fileSystems.clear();
            }

            @Override
            public synchronized String mount(final String desiredLocation, final Mount mount, final String driveName) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                final li.cil.oc.api.fs.FileSystem fs = DriverComputerCraftMedia.fromComputerCraft(mount);
                if (fs == null) return null;
                return mount(desiredLocation, FileSystem.asManagedEnvironment(fs, driveName));
            }

            @Override
            public synchronized String mountWritable(final String desiredLocation, final WritableMount mount, final String driveName) {
                return mount(desiredLocation, mount, driveName);
            }

            private String mount(final String path, final ManagedEnvironment fileSystem) {
                if (fileSystem == null) return null;
                fileSystems.put(path, fileSystem); // TODO This is per peripheral/Environment. It would be far better with per computer
                context.node().connect(fileSystem.node());
                return path;
            }

            @Override
            public synchronized void unmount(final String location) {
                final ManagedEnvironment fileSystem = fileSystems.remove(location);
                if (fileSystem != null) {
                    fileSystem.node().remove();
                }
            }

            @Override
            public int getID() {
                return context.node().address().hashCode();
            }

            @Override
            public void queueEvent(final String event, final Object... arguments) {
                context.signal(event, arguments == null ? new Object[0] : arguments);
            }

            @Override
            public String getAttachmentName() {
                return owner.node().address();
            }

            @Override
            public Map<String, IPeripheral> getAvailablePeripherals() {
                return Collections.emptyMap();
            }

            @Override
            public IPeripheral getAvailablePeripheral(final String name) {
                return null;
            }

            @Override
            public WorkMonitor getMainThreadMonitor() {
                return UnlimitedWorkMonitor.INSTANCE;
            }
        }

        /**
         * OC does not budget server thread work for peripherals the way CC does.
         */
        private static final class UnlimitedWorkMonitor implements WorkMonitor {
            static final UnlimitedWorkMonitor INSTANCE = new UnlimitedWorkMonitor();

            @Override
            public boolean canWork() {
                return true;
            }

            @Override
            public boolean shouldWork() {
                return true;
            }

            @Override
            public void trackWork(final long time, final TimeUnit unit) {
            }
        }
    }
}
