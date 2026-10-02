package li.cil.oc.common;

import dev.architectury.networking.NetworkManager;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.block.RobotAfterimage;
import li.cil.oc.common.tileentity.Robot;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.zip.InflaterInputStream;

/**
 * Base class of the side specific packet handlers ({@code li.cil.oc.client.PacketHandler},
 * {@code li.cil.oc.server.PacketHandler}).
 * <p>
 * Transport: all packets go over the single Architectury {@link NetworkManager}
 * channel {@link #CHANNEL} ({@code opencomputers:main}) in both directions. The
 * custom payload is exactly the byte array produced by {@link PacketBuilder}.
 * <ul>
 * <li>{@link #registerServerReceiver()} registers the C2S receiver (called from
 * {@code common.Proxy.preInit()}).</li>
 * <li>{@link #registerClientReceiver(PacketHandler)} registers the S2C receiver;
 * must only be called on the physical client (from client init), passing
 * {@code li.cil.oc.client.PacketHandler.INSTANCE}.</li>
 * </ul>
 */
public abstract class PacketHandler {
    public static final ResourceLocation CHANNEL = new ResourceLocation(OpenComputers.ID, "main");

    public static PacketHandler clientHandler;

    public static PacketHandler serverHandler;

    private static boolean serverReceiverRegistered;

    private static boolean clientReceiverRegistered;

    /**
     * Registers the client to server receiver. Call once during common init
     * (both physical sides).
     */
    public static synchronized void registerServerReceiver() {
        serverHandler = li.cil.oc.server.PacketHandler.INSTANCE;
        if (serverReceiverRegistered) return;
        serverReceiverRegistered = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> receive(NetworkManager.Side.C2S, buf, context));
    }

    /**
     * Registers the server to client receiver. Call once during client init,
     * <em>only</em> on the physical client.
     */
    public static synchronized void registerClientReceiver(PacketHandler handler) {
        clientHandler = handler;
        if (clientReceiverRegistered) return;
        clientReceiverRegistered = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CHANNEL, (buf, context) -> receive(NetworkManager.Side.S2C, buf, context));
    }

    private static void receive(NetworkManager.Side side, FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        // Copy the payload, the buffer is released after this callback returns.
        final byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        context.queue(() -> handlePacket(side, data, context.getPlayer()));
    }

    public static void handlePacket(NetworkManager.Side side, byte[] arr, Player player) {
        try {
            final PacketHandler handler = side == NetworkManager.Side.S2C ? clientHandler : serverHandler;
            if (handler != null) {
                final ByteArrayInputStream stream = new ByteArrayInputStream(arr);
                if (stream.read() == 0) handler.dispatch(handler.createParser(stream, player));
                else handler.dispatch(handler.createParser(new InflaterInputStream(stream), player));
            }
        } catch (Throwable e) {
            // Don't crash on badly formatted packets (may have been altered by a
            // malicious client, in which case we don't want to allow it to kill the
            // server like this). Just spam the log a bit... ;)
            OpenComputers.log.warn("Received a badly formatted packet.", e);
        }

        // Avoid AFK kicks by marking players as non-idle when they send packets.
        // This will usually be stuff like typing while in screen GUIs.
        if (player instanceof ServerPlayer mp) {
            mp.resetLastActionTime();
        }
    }

    // ----------------------------------------------------------------------- //

    /**
     * Gets the world for the specified dimension.
     * <p>
     * For clients this returns the client's world if it is the specified
     * dimension; None otherwise. For the server it returns the world for the
     * specified dimension, if such a dimension exists; None otherwise.
     */
    protected abstract Optional<Level> world(Player player, ResourceLocation dimension);

    protected abstract void dispatch(PacketParser p);

    protected PacketParser createParser(InputStream stream, Player player) {
        return new PacketParser(stream, player);
    }

    /**
     * Reads a packet. Port note: implements {@link DataInput} with methods that
     * do not throw checked exceptions; {@link IOException}s (including
     * {@link java.io.EOFException} when reading past the end of the packet) are
     * rethrown as {@link UncheckedIOException} with the original as cause.
     */
    public class PacketParser implements DataInput {
        private final DataInputStream in;

        public final Player player;

        public final PacketType packetType;

        public PacketParser(InputStream stream, Player player) {
            this.in = new DataInputStream(stream);
            this.player = player;
            this.packetType = PacketType.byId(readByte());
        }

        public <T> T readRegistryEntry(Registry<T> registry) {
            return registry.get(new ResourceLocation(readUTF()));
        }

        public <T> Optional<T> getBlockEntity(Class<T> clazz, ResourceLocation dimension, int x, int y, int z) {
            final Optional<Level> maybeWorld = world(player, dimension);
            if (maybeWorld.isPresent() && ExtendedWorld.blockExists(maybeWorld.get(), BlockPosition.apply(x, y, z))) {
                final Level world = maybeWorld.get();
                final BlockEntity t = world.getBlockEntity(new BlockPos(x, y, z));
                if (t != null && clazz.isInstance(t)) {
                    return Optional.of(clazz.cast(t));
                }
                // In case a robot moved away before the packet arrived. This is
                // mostly used when the robot *starts* moving while the client sends
                // a request to the server.
                final ItemInfo info = li.cil.oc.api.Items.get(Constants.BlockName.RobotAfterimage);
                final Block block = info != null ? info.block() : null;
                if (block instanceof RobotAfterimage afterimage) {
                    final Optional<Robot> robot = afterimage.findMovingRobot(world, new BlockPos(x, y, z));
                    if (robot.isPresent() && clazz.isInstance(robot.get().proxy)) {
                        return Optional.of(clazz.cast(robot.get().proxy));
                    }
                }
            }
            return Optional.empty();
        }

        public <T> Optional<T> getEntity(Class<T> clazz, ResourceLocation dimension, int id) {
            final Optional<Level> world = world(player, dimension);
            if (world.isPresent()) {
                final Entity e = world.get().getEntity(id);
                if (e != null && clazz.isInstance(e)) {
                    return Optional.of(clazz.cast(e));
                }
            }
            return Optional.empty();
        }

        public <T> Optional<T> readBlockEntity(Class<T> clazz) {
            final ResourceLocation dimension = new ResourceLocation(readUTF());
            final int x = readInt();
            final int y = readInt();
            final int z = readInt();
            return getBlockEntity(clazz, dimension, x, y, z);
        }

        public <T> Optional<T> readEntity(Class<T> clazz) {
            final ResourceLocation dimension = new ResourceLocation(readUTF());
            final int id = readInt();
            return getEntity(clazz, dimension, id);
        }

        public Optional<Direction> readDirection() {
            final byte id = readByte();
            if (id < 0) return Optional.empty();
            return Optional.of(Direction.from3DDataValue(id));
        }

        public ItemStack readItemStack() {
            final boolean haveStack = readBoolean();
            if (haveStack) {
                return ItemStack.of(readNBT());
            } else return ItemStack.EMPTY;
        }

        public CompoundTag readNBT() {
            final boolean haveNbt = readBoolean();
            if (haveNbt) {
                try {
                    return NbtIo.read(this);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else return null;
        }

        public PacketType readPacketType() {
            return PacketType.byId(readByte());
        }

        // ------------------------------------------------------------------- //
        // DataInput without checked exceptions.

        public int read(byte[] b) {
            try {
                return in.read(b);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public int read(byte[] b, int off, int len) {
            try {
                return in.read(b, off, len);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public int available() {
            try {
                return in.available();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void readFully(byte[] b) {
            try {
                in.readFully(b);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void readFully(byte[] b, int off, int len) {
            try {
                in.readFully(b, off, len);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public int skipBytes(int n) {
            try {
                return in.skipBytes(n);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public boolean readBoolean() {
            try {
                return in.readBoolean();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public byte readByte() {
            try {
                return in.readByte();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public int readUnsignedByte() {
            try {
                return in.readUnsignedByte();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public short readShort() {
            try {
                return in.readShort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public int readUnsignedShort() {
            try {
                return in.readUnsignedShort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public char readChar() {
            try {
                return in.readChar();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public int readInt() {
            try {
                return in.readInt();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public long readLong() {
            try {
                return in.readLong();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public float readFloat() {
            try {
                return in.readFloat();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public double readDouble() {
            try {
                return in.readDouble();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        @Deprecated
        public String readLine() {
            try {
                //noinspection deprecation
                return in.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public String readUTF() {
            try {
                return in.readUTF();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
