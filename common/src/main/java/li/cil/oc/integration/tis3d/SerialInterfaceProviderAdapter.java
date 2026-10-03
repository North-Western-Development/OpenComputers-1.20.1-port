package li.cil.oc.integration.tis3d;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.ResultWrapper;
import li.cil.tis3d.api.serial.SerialInterface;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import li.cil.tis3d.api.serial.SerialProtocolDocumentationReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Optional;

/**
 * Lets TIS-3D's serial port module talk to OpenComputers adapters: while a serial port module
 * faces an adapter, the adapter's network gains a {@code serial_port} component.
 */
public final class SerialInterfaceProviderAdapter implements SerialInterfaceProvider {
    public static final SerialInterfaceProviderAdapter INSTANCE = new SerialInterfaceProviderAdapter();

    private SerialInterfaceProviderAdapter() {
    }

    @Override
    public Optional<SerialProtocolDocumentationReference> getDocumentationReference() {
        // The page lives in assets/tis3d/doc/<lang>/protocols/ so that TIS-3D's manual finds it.
        return Optional.of(new SerialProtocolDocumentationReference(Component.literal("OpenComputers Adapter"), "protocols/opencomputersadapter.md"));
    }

    @Override
    public boolean matches(final Level world, final BlockPos pos, final Direction side) {
        return world.getBlockEntity(pos) instanceof Adapter;
    }

    @Override
    public Optional<SerialInterface> getInterface(final Level world, final BlockPos pos, final Direction side) {
        if (world.getBlockEntity(pos) instanceof Adapter adapter) {
            return Optional.of(new SerialInterfaceAdapter(adapter));
        }
        return Optional.empty();
    }

    @Override
    public boolean stillValid(final Level world, final BlockPos pos, final Direction side, final SerialInterface serialInterface) {
        return serialInterface instanceof SerialInterfaceAdapter adapter && adapter.tileEntity == world.getBlockEntity(pos);
    }

    public static final class SerialInterfaceAdapter implements Environment, SerialInterface {
        private static final int BufferCapacity = 128;

        final Adapter tileEntity;
        private final ArrayDeque<Short> readBuffer = new ArrayDeque<>();
        private final ArrayDeque<Short> writeBuffer = new ArrayDeque<>();
        private volatile boolean isReading = false;

        private final Node node = Network.newNode(this, Visibility.Network).withComponent("serial_port").create();

        public SerialInterfaceAdapter(final Adapter tileEntity) {
            this.tileEntity = tileEntity;
        }

        // ----------------------------------------------------------------------- //

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onMessage(final Message message) {
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        // ----------------------------------------------------------------------- //

        @Callback(doc = "function(enabled:boolean) -- Set whether values written by the serial port module are accepted.")
        public Object[] setReading(final Context context, final Arguments args) {
            isReading = args.checkBoolean(0);
            return null;
        }

        @Callback(doc = "function():number or nil -- Get the next value received from the serial port module, if any.")
        public Object[] read(final Context context, final Arguments args) {
            synchronized (readBuffer) {
                if (!readBuffer.isEmpty()) {
                    return ResultWrapper.result((int) readBuffer.poll());
                }
                return null;
            }
        }

        @Callback(doc = "function(value:number):boolean -- Queue a value for the serial port module to read.")
        public Object[] write(final Context context, final Arguments args) {
            final short value = (short) args.checkInteger(0);
            synchronized (writeBuffer) {
                if (writeBuffer.size() < BufferCapacity) {
                    writeBuffer.add(value);
                    return ResultWrapper.result(true);
                }
                return ResultWrapper.result(false, "buffer full");
            }
        }

        // ----------------------------------------------------------------------- //

        @Override
        public boolean canWrite() {
            synchronized (readBuffer) {
                return isReading && readBuffer.size() < BufferCapacity;
            }
        }

        @Override
        public void write(final short value) {
            synchronized (readBuffer) {
                readBuffer.add(value);
            }
        }

        @Override
        public boolean canRead() {
            ensureConnected();
            synchronized (writeBuffer) {
                return !writeBuffer.isEmpty();
            }
        }

        @Override
        public short peek() {
            synchronized (writeBuffer) {
                final Short value = writeBuffer.peek();
                return value != null ? value : 0;
            }
        }

        @Override
        public void skip() {
            synchronized (writeBuffer) {
                writeBuffer.poll();
            }
        }

        @Override
        public void reset() {
            synchronized (readBuffer) {
                synchronized (writeBuffer) {
                    readBuffer.clear();
                    writeBuffer.clear();
                    node.remove();
                }
            }
        }

        @Override
        public void load(final CompoundTag nbt) {
            node.loadData(nbt);

            synchronized (writeBuffer) {
                writeBuffer.clear();
                for (int value : nbt.getIntArray("writeBuffer")) writeBuffer.add((short) value);
            }
            synchronized (readBuffer) {
                readBuffer.clear();
                for (int value : nbt.getIntArray("readBuffer")) readBuffer.add((short) value);
            }
            isReading = nbt.getBoolean("isReading");
        }

        @Override
        public void save(final CompoundTag nbt) {
            node.saveData(nbt);

            synchronized (writeBuffer) {
                nbt.putIntArray("writeBuffer", writeBuffer.stream().mapToInt(Short::intValue).toArray());
            }
            synchronized (readBuffer) {
                nbt.putIntArray("readBuffer", readBuffer.stream().mapToInt(Short::intValue).toArray());
            }
            nbt.putBoolean("isReading", isReading);
        }

        private void ensureConnected() {
            final Node adapterNode = tileEntity.node();
            if (adapterNode != null && adapterNode.network() != null && adapterNode.network() != node.network()) {
                adapterNode.connect(node);
            }
        }
    }
}
