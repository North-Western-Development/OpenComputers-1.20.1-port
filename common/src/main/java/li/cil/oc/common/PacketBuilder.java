package li.cil.oc.common;

import li.cil.oc.Settings;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.GameInstance;
import io.netty.buffer.Unpooled;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds a packet in OpenComputers' own byte format.
 * <p>
 * Payload layout (unchanged from 1.16.5): one byte compression flag (0 = raw,
 * 1 = deflate compressed remainder), followed by the {@link PacketType} id byte
 * and the packet type specific data written via {@link DataOutput} methods.
 * The payload is transported as the raw content of a custom payload packet on
 * channel {@link PacketHandler#CHANNEL} (both directions) via Architectury's
 * {@link NetworkManager}.
 * <p>
 * Port note: on 1.16.5 this extended {@link DataOutputStream}. Its write
 * methods are final and declare {@link IOException}, so this class instead
 * implements {@link DataOutput} with methods that do <em>not</em> throw checked
 * exceptions (any {@link IOException} is rethrown as {@link UncheckedIOException};
 * since everything is written to memory this never happens in practice). It can
 * still be passed anywhere a {@link DataOutput} is expected (e.g.
 * {@link NbtIo#write(CompoundTag, DataOutput)}).
 * <p>
 * Port note: {@link #writeRegistryEntry} now writes the registry key as UTF
 * string instead of a Forge numeric registry id.
 */
public abstract class PacketBuilder implements DataOutput {
    private final DataOutputStream out;

    protected PacketBuilder(OutputStream stream) {
        this.out = new DataOutputStream(stream);
    }

    // ----------------------------------------------------------------------- //
    // DataOutput without checked exceptions.

    @Override
    public void write(int b) {
        try {
            out.write(b);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(byte[] b) {
        try {
            out.write(b);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        try {
            out.write(b, off, len);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeBoolean(boolean v) {
        try {
            out.writeBoolean(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeByte(int v) {
        try {
            out.writeByte(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeShort(int v) {
        try {
            out.writeShort(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeChar(int v) {
        try {
            out.writeChar(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeInt(int v) {
        try {
            out.writeInt(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeLong(long v) {
        try {
            out.writeLong(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeFloat(float v) {
        try {
            out.writeFloat(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeDouble(double v) {
        try {
            out.writeDouble(v);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeBytes(String s) {
        try {
            out.writeBytes(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeChars(String s) {
        try {
            out.writeChars(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeUTF(String s) {
        try {
            out.writeUTF(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ----------------------------------------------------------------------- //

    public <T> void writeRegistryEntry(Registry<T> registry, T value) {
        final ResourceLocation key = registry.getKey(value);
        writeUTF(key != null ? key.toString() : "");
    }

    public void writeTileEntity(BlockEntity t) {
        writeUTF(t.getLevel().dimension().location().toString());
        writeInt(t.getBlockPos().getX());
        writeInt(t.getBlockPos().getY());
        writeInt(t.getBlockPos().getZ());
    }

    public void writeEntity(Entity e) {
        writeUTF(e.level().dimension().location().toString());
        writeInt(e.getId());
    }

    public void writeDirection(Optional<Direction> d) {
        if (d.isPresent()) writeByte((byte) d.get().ordinal());
        else writeByte((byte) -1);
    }

    public void writeItemStack(ItemStack stack) {
        final boolean haveStack = !stack.isEmpty() && stack.getCount() > 0;
        writeBoolean(haveStack);
        if (haveStack) {
            writeNBT(stack.save(new CompoundTag()));
        }
    }

    public void writeNBT(CompoundTag nbt) {
        final boolean haveNbt = nbt != null;
        writeBoolean(haveNbt);
        if (haveNbt) {
            try {
                NbtIo.write(nbt, this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void writePacketType(PacketType pt) {
        writeByte(pt.id());
    }

    // ----------------------------------------------------------------------- //

    private FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(packet()));
    }

    public void sendToAllPlayers() {
        final MinecraftServer server = GameInstance.getServer();
        if (server == null) return;
        NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), PacketHandler.CHANNEL, buffer());
    }

    public void sendToPlayersNearEntity(Entity e) {
        sendToPlayersNearEntity(e, Optional.empty());
    }

    public void sendToPlayersNearEntity(Entity e, Optional<Double> range) {
        sendToNearbyPlayers(e.level(), e.getX(), e.getY(), e.getZ(), range);
    }

    public void sendToPlayersNearTileEntity(BlockEntity t) {
        sendToPlayersNearTileEntity(t, Optional.empty());
    }

    public void sendToPlayersNearTileEntity(BlockEntity t, Optional<Double> range) {
        final BlockPos pos = t.getBlockPos();
        if (t.getLevel() instanceof ServerLevel serverLevel) {
            // Only players tracking the block entity's chunk can have it loaded.
            final double maxPacketRangeSq = maxPacketRangeSq(serverLevel.getServer(), range);
            final List<ServerPlayer> targets = new ArrayList<>();
            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= maxPacketRangeSq) {
                    targets.add(player);
                }
            }
            if (!targets.isEmpty()) {
                NetworkManager.sendToPlayers(targets, PacketHandler.CHANNEL, buffer());
            }
        } else {
            sendToNearbyPlayers(t.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range);
        }
    }

    public void sendToPlayersNearHost(EnvironmentHost host) {
        sendToPlayersNearHost(host, Optional.empty());
    }

    public void sendToPlayersNearHost(EnvironmentHost host, Optional<Double> range) {
        if (host instanceof BlockEntity t) sendToPlayersNearTileEntity(t, range);
        else sendToNearbyPlayers(host.world(), host.xPosition(), host.yPosition(), host.zPosition(), range);
    }

    /**
     * The given range (or the server's view distance if none is given), capped by
     * {@code misc.maxNetworkClientPacketDistance} if that is set.
     */
    private static double maxPacketRangeSq(MinecraftServer server, Optional<Double> range) {
        double maxPacketRange = range.orElseGet(() -> (server.getPlayerList().getViewDistance() + 1) * 16.0);
        final double maxPacketRangeConfig = Settings.get().maxNetworkClientPacketDistance;
        if (maxPacketRangeConfig > 0.0) {
            maxPacketRange = Math.min(maxPacketRange, maxPacketRangeConfig);
        }
        return maxPacketRange * maxPacketRange;
    }

    public void sendToNearbyPlayers(Level world, double x, double y, double z, Optional<Double> range) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        final double maxPacketRangeSq = maxPacketRangeSq(serverLevel.getServer(), range);
        final List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) <= maxPacketRangeSq) {
                targets.add(player);
            }
        }
        if (!targets.isEmpty()) {
            NetworkManager.sendToPlayers(targets, PacketHandler.CHANNEL, buffer());
        }
    }

    public void sendToPlayer(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, PacketHandler.CHANNEL, buffer());
    }

    /**
     * Client only.
     */
    public void sendToServer() {
        NetworkManager.sendToServer(PacketHandler.CHANNEL, buffer());
    }

    protected abstract byte[] packet();

    // ----------------------------------------------------------------------- //

    public static ByteArrayOutputStream newData(boolean compressed) {
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(compressed ? 1 : 0);
        return data;
    }

}
