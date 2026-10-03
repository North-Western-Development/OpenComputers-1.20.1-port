package li.cil.oc.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.Loot;
import li.cil.oc.common.PacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.nanomachines.ControllerImpl;
import li.cil.oc.common.tileentity.Waypoint;
import li.cil.oc.common.tileentity.traits.Colored;
import li.cil.oc.common.tileentity.traits.Computer;
import li.cil.oc.common.tileentity.traits.PowerInformation;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.PackedColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.WeakHashMap;

/**
 * Server to client packets. Port notes: trait typed tile entities are passed to
 * {@link PacketBuilder#writeTileEntity(BlockEntity)} via a cast to
 * {@link BlockEntity}; Scala default arguments became overloads.
 */
public final class PacketSender {
    private PacketSender() {
    }

    private static BlockEntity be(Object t) {
        return (BlockEntity) t;
    }

    public static void sendAdapterState(li.cil.oc.common.tileentity.Adapter t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.AdapterState);

        pb.writeTileEntity(t);
        pb.writeByte(t.compressSides());

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendAnalyze(String address, ServerPlayer player) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.Analyze);

        pb.writeUTF(address);

        pb.sendToPlayer(player);
    }

    public static void sendChargerState(li.cil.oc.common.tileentity.Charger t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ChargerState);

        pb.writeTileEntity(t);
        pb.writeDouble(t.chargeSpeed);
        pb.writeBoolean(t.hasPower);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendClientLog(String line, ServerPlayer player) {
        final CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.ClientLog);

        pb.writeUTF(line);

        pb.sendToPlayer(player);
    }

    public static void sendClipboard(ServerPlayer player, String text) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.Clipboard);

        pb.writeUTF(text);

        pb.sendToPlayer(player);
    }

    public static void sendColorChange(Colored t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ColorChange);

        pb.writeTileEntity(be(t));
        pb.writeInt(t.getColor());

        pb.sendToPlayersNearTileEntity(be(t));
    }

    public static void sendComputerState(Computer t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ComputerState);

        pb.writeTileEntity(be(t));
        pb.writeBoolean(t.isRunning());
        pb.writeBoolean(t.hasErrored());

        pb.sendToPlayersNearTileEntity(be(t));
    }

    public static void sendMachineItemState(ServerPlayer player, ItemStack stack, boolean isRunning) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MachineItemStateResponse);

        pb.writeItemStack(stack);
        pb.writeBoolean(isRunning);

        pb.sendToPlayer(player);
    }

    public static void sendComputerUserList(Computer t, String[] list) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ComputerUserList);

        pb.writeTileEntity(be(t));
        pb.writeInt(list.length);
        for (String user : list) pb.writeUTF(user);

        pb.sendToPlayersNearTileEntity(be(t));
    }

    public static void sendContainerUpdate(AbstractContainerMenu c, CompoundTag nbt, ServerPlayer player) {
        if (!nbt.isEmpty()) {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ContainerUpdate);

            pb.writeInt(c.containerId);
            pb.writeNBT(nbt);

            pb.sendToPlayer(player);
        }
    }

    public static void sendDisassemblerActive(li.cil.oc.common.tileentity.Disassembler t, boolean active) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DisassemblerActiveChange);

        pb.writeTileEntity(t);
        pb.writeBoolean(active);

        pb.sendToPlayersNearTileEntity(t);
    }

    // Avoid spamming the network with disk activity notices. Entries expire, so names that are not
    // accessed again do not stay around forever.
    private static final Map<Node, Cache<String, Long>> fileSystemAccessTimeouts = new WeakHashMap<>();

    public static void sendFileSystemActivity(Node node, EnvironmentHost host, String name) {
        final int diskActivityPacketDelay = Settings.get().diskActivitySoundDelay;
        if (diskActivityPacketDelay < 0) return;

        final Cache<String, Long> hostTimeouts;
        synchronized (fileSystemAccessTimeouts) {
            hostTimeouts = fileSystemAccessTimeouts.computeIfAbsent(node, k -> CacheBuilder.newBuilder()
                    .concurrencyLevel(Settings.get().threads)
                    .maximumSize(250)
                    .expireAfterWrite(diskActivityPacketDelay, TimeUnit.MILLISECONDS)
                    .build());
        }
        final Long lastHostTimeout = hostTimeouts.getIfPresent(name);
        if (lastHostTimeout != null && lastHostTimeout > System.currentTimeMillis()) {
            return; // Cooldown.
        }
        final FileSystemAccessEvent.Server event = host instanceof BlockEntity t
                ? new FileSystemAccessEvent.Server(name, t, node)
                : new FileSystemAccessEvent.Server(name, host.world(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        final boolean canceled = EventBus.INSTANCE.post(event);
        if (!canceled) {
            hostTimeouts.put(name, System.currentTimeMillis() + diskActivityPacketDelay);

            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.FileSystemActivity);

            pb.writeUTF(event.getSound());
            writeRawNbt(pb, event.getData());
            writeEventLocation(pb, event.getBlockEntity(), event.getWorld(), event.getX(), event.getY(), event.getZ());

            pb.sendToPlayersNearHost(host, Optional.of(Settings.get().maxNetworkClientSoundPacketDistance));
        }
    }

    public static void sendNetworkActivity(Node node, EnvironmentHost host) {
        final NetworkActivityEvent.Server event = host instanceof BlockEntity t
                ? new NetworkActivityEvent.Server(t, node)
                : new NetworkActivityEvent.Server(host.world(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        final boolean canceled = EventBus.INSTANCE.post(event);
        if (!canceled) {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.NetworkActivity);

            writeRawNbt(pb, event.getData());
            writeEventLocation(pb, event.getBlockEntity(), event.getWorld(), event.getX(), event.getY(), event.getZ());

            pb.sendToPlayersNearHost(host, Optional.of(Settings.get().maxNetworkClientEffectPacketDistance));
        }
    }

    private static void writeRawNbt(PacketBuilder pb, CompoundTag nbt) {
        try {
            NbtIo.write(nbt, pb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeEventLocation(PacketBuilder pb, BlockEntity blockEntity, Level world, double x, double y, double z) {
        if (blockEntity != null) {
            pb.writeBoolean(true);
            pb.writeTileEntity(blockEntity);
        } else {
            pb.writeBoolean(false);
            pb.writeUTF(world.dimension().location().toString());
            pb.writeDouble(x);
            pb.writeDouble(y);
            pb.writeDouble(z);
        }
    }

    public static void sendFloppyChange(li.cil.oc.common.tileentity.DiskDrive t) {
        sendFloppyChange(t, ItemStack.EMPTY);
    }

    public static void sendFloppyChange(li.cil.oc.common.tileentity.DiskDrive t, ItemStack stack) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.FloppyChange);

        pb.writeTileEntity(t);
        pb.writeItemStack(stack);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramClear(li.cil.oc.common.tileentity.Hologram t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.HologramClear);

        pb.writeTileEntity(t);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramColor(li.cil.oc.common.tileentity.Hologram t, int index, int value) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.HologramColor);

        pb.writeTileEntity(t);
        pb.writeInt(index);
        pb.writeInt(value);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramPowerChange(li.cil.oc.common.tileentity.Hologram t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.HologramPowerChange);

        pb.writeTileEntity(t);
        pb.writeBoolean(t.hasPower);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramScale(li.cil.oc.common.tileentity.Hologram t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.HologramScale);

        pb.writeTileEntity(t);
        pb.writeDouble(t.scale);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramArea(li.cil.oc.common.tileentity.Hologram t) {
        final CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.HologramArea);

        pb.writeTileEntity(t);
        pb.writeByte(t.dirtyFromX);
        pb.writeByte(t.dirtyUntilX);
        pb.writeByte(t.dirtyFromZ);
        pb.writeByte(t.dirtyUntilZ);
        for (int x = t.dirtyFromX; x < t.dirtyUntilX; x++) {
            for (int z = t.dirtyFromZ; z < t.dirtyUntilZ; z++) {
                pb.writeInt(t.volume[x + z * t.width]);
                pb.writeInt(t.volume[x + z * t.width + t.width * t.width]);
            }
        }

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramValues(li.cil.oc.common.tileentity.Hologram t) {
        final CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.HologramValues);

        pb.writeTileEntity(t);
        pb.writeInt(t.dirty.size());
        for (short xz : t.dirty) {
            final byte x = (byte) (xz >> 8);
            final byte z = (byte) xz;
            pb.writeShort(xz);
            final int rangeStart = x + z * t.width;
            final int rangeFinal = x + z * t.width + t.width * t.width;
            pb.writeInt(t.volume[Math.min(Math.max(rangeStart, 0), t.volume.length - 1)]);
            pb.writeInt(t.volume[Math.min(Math.max(rangeFinal, 0), t.volume.length - 1)]);
        }

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramOffset(li.cil.oc.common.tileentity.Hologram t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.HologramTranslation);

        pb.writeTileEntity(t);
        pb.writeDouble(t.translation.x);
        pb.writeDouble(t.translation.y);
        pb.writeDouble(t.translation.z);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramRotation(li.cil.oc.common.tileentity.Hologram t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.HologramRotation);

        pb.writeTileEntity(t);
        pb.writeFloat(t.rotationAngle);
        pb.writeFloat(t.rotationX);
        pb.writeFloat(t.rotationY);
        pb.writeFloat(t.rotationZ);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendHologramRotationSpeed(li.cil.oc.common.tileentity.Hologram t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.HologramRotationSpeed);

        pb.writeTileEntity(t);
        pb.writeFloat(t.rotationSpeed);
        pb.writeFloat(t.rotationSpeedX);
        pb.writeFloat(t.rotationSpeedY);
        pb.writeFloat(t.rotationSpeedZ);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendLootDisks(ServerPlayer p) {
        // Sending as separate packets, because CompressedStreamTools hiccups otherwise...
        for (Pair<ItemStack, Integer> entry : Loot.worldDisks) {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.LootDisk);

            pb.writeItemStack(entry.getLeft());

            pb.sendToPlayer(p);
        }
        for (ItemStack stack : Loot.disksForCyclingServer) {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.CyclingDisk);

            pb.writeItemStack(stack);

            pb.sendToPlayer(p);
        }
    }

    public static void sendNanomachineConfiguration(Player player) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.NanomachinesConfiguration);

        pb.writeEntity(player);
        final Controller controller = li.cil.oc.api.Nanomachines.getController(player);
        if (controller instanceof ControllerImpl impl) {
            pb.writeBoolean(true);
            final CompoundTag nbt = new CompoundTag();
            impl.saveData(nbt);
            pb.writeNBT(nbt);
        } else {
            pb.writeBoolean(false);
        }

        pb.sendToPlayersNearEntity(player);
    }

    public static void sendNanomachineInputs(Player player) {
        final Controller controller = li.cil.oc.api.Nanomachines.getController(player);
        if (controller instanceof ControllerImpl) {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.NanomachinesInputs);

            pb.writeEntity(player);
            final byte[] inputs = new byte[controller.getTotalInputCount()];
            for (int i = 0; i < inputs.length; i++) {
                inputs[i] = controller.getInput(i) ? (byte) 1 : (byte) 0;
            }
            pb.writeInt(inputs.length);
            pb.write(inputs);

            pb.sendToPlayersNearEntity(player);
        }
        // Else: wat.
    }

    public static void sendNanomachinePower(Player player) {
        final Controller controller = li.cil.oc.api.Nanomachines.getController(player);
        if (controller instanceof ControllerImpl) {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.NanomachinesPower);

            pb.writeEntity(player);
            pb.writeDouble(controller.getLocalBuffer());

            pb.sendToPlayersNearEntity(player);
        }
        // Else: wat.
    }

    public static void sendNetSplitterState(li.cil.oc.common.tileentity.NetSplitter t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.NetSplitterState);

        pb.writeTileEntity(t);
        pb.writeBoolean(t.isInverted);
        pb.writeByte(t.compressSides());

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendParticleEffect(BlockPosition position, ParticleOptions particleType, int count, double velocity) {
        sendParticleEffect(position, particleType, count, velocity, Optional.empty());
    }

    public static void sendParticleEffect(BlockPosition position, ParticleOptions particleType, int count, double velocity, Optional<Direction> direction) {
        if (count > 0) {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ParticleEffect);

            final Level world = position.world.get();
            pb.writeUTF(world.dimension().location().toString());
            pb.writeInt(position.x);
            pb.writeInt(position.y);
            pb.writeInt(position.z);
            pb.writeDouble(velocity);
            pb.writeDirection(direction);
            pb.writeRegistryEntry(BuiltInRegistries.PARTICLE_TYPE, particleType.getType());
            pb.writeByte((byte) count);

            pb.sendToNearbyPlayers(world, position.x, position.y, position.z, Optional.of(Settings.get().maxNetworkClientEffectPacketDistance / 2.0));
        }
    }

    public static void sendPetVisibility() {
        sendPetVisibility(Optional.empty(), Optional.empty());
    }

    public static void sendPetVisibility(Optional<String> name) {
        sendPetVisibility(name, Optional.empty());
    }

    public static void sendPetVisibility(Optional<String> name, Optional<ServerPlayer> player) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.PetVisibility);

        if (name.isPresent()) {
            final String n = name.get();
            pb.writeInt(1);
            pb.writeUTF(n);
            pb.writeBoolean(!PetVisibility.hidden.contains(n));
        } else {
            synchronized (PetVisibility.hidden) {
                pb.writeInt(PetVisibility.hidden.size());
                for (String n : PetVisibility.hidden) {
                    pb.writeUTF(n);
                    pb.writeBoolean(false);
                }
            }
        }

        if (player.isPresent()) pb.sendToPlayer(player.get());
        else pb.sendToAllPlayers();
    }

    public static void sendPowerState(PowerInformation t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.PowerState);

        pb.writeTileEntity(be(t));
        pb.writeDouble(Math.round(t.globalBuffer()));
        pb.writeDouble(t.globalBufferSize());

        pb.sendToPlayersNearTileEntity(be(t));
    }

    public static void sendPrinting(li.cil.oc.common.tileentity.Printer t, boolean printing) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.PrinterState);

        pb.writeTileEntity(t);
        pb.writeBoolean(printing);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRackInventory(li.cil.oc.common.tileentity.Rack t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RackInventory);

        pb.writeTileEntity(t);
        pb.writeInt(t.getContainerSize());
        for (int slot = 0; slot < t.getContainerSize(); slot++) {
            pb.writeInt(slot);
            pb.writeItemStack(t.getItem(slot));
        }

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRackInventory(li.cil.oc.common.tileentity.Rack t, int slot) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RackInventory);

        pb.writeTileEntity(t);
        pb.writeInt(1);
        pb.writeInt(slot);
        pb.writeItemStack(t.getItem(slot));

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRackMountableData(li.cil.oc.common.tileentity.Rack t, int mountable) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RackMountableData);

        pb.writeTileEntity(t);
        pb.writeInt(mountable);
        pb.writeNBT(t.lastData[mountable]);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRaidChange(li.cil.oc.common.tileentity.Raid t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RaidStateChange);

        pb.writeTileEntity(t);
        for (int slot = 0; slot < t.getContainerSize(); slot++) {
            pb.writeBoolean(!t.getItem(slot).isEmpty());
        }

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRedstoneState(RedstoneAware t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RedstoneState);

        pb.writeTileEntity(be(t));
        pb.writeBoolean(t.isOutputEnabled());
        for (Direction d : Direction.values()) {
            pb.writeByte(t.getOutput(d));
        }

        pb.sendToPlayersNearTileEntity(be(t));
    }

    public static void sendRobotAssembling(li.cil.oc.common.tileentity.Assembler t, boolean assembling) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotAssemblingState);

        pb.writeTileEntity(t);
        pb.writeBoolean(assembling);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRobotMove(li.cil.oc.common.tileentity.Robot t, BlockPos position, Direction direction) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotMove);

        // Custom pb.writeTileEntity() with fake coordinates (valid for the client).
        pb.writeUTF(t.getLevel().dimension().location().toString());
        pb.writeInt(position.getX());
        pb.writeInt(position.getY());
        pb.writeInt(position.getZ());
        pb.writeDirection(Optional.ofNullable(direction));

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRobotAnimateSwing(li.cil.oc.common.tileentity.Robot t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotAnimateSwing);

        pb.writeTileEntity(t.proxy);
        pb.writeInt(t.animationTicksTotal);

        pb.sendToPlayersNearTileEntity(t, Optional.of(Settings.get().maxNetworkClientEffectPacketDistance));
    }

    public static void sendRobotAnimateTurn(li.cil.oc.common.tileentity.Robot t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotAnimateTurn);

        pb.writeTileEntity(t.proxy);
        pb.writeByte(t.turnAxis);
        pb.writeInt(t.animationTicksTotal);

        pb.sendToPlayersNearTileEntity(t, Optional.of(Settings.get().maxNetworkClientEffectPacketDistance));
    }

    public static void sendRobotInventory(li.cil.oc.common.tileentity.Robot t, int slot, ItemStack stack) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotInventoryChange);

        pb.writeTileEntity(t.proxy);
        pb.writeInt(slot);
        pb.writeItemStack(stack);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRobotLightChange(li.cil.oc.common.tileentity.Robot t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotLightChange);

        pb.writeTileEntity(t.proxy);
        pb.writeInt(t.info.lightColor);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRobotNameChange(li.cil.oc.common.tileentity.Robot t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotNameChange);

        pb.writeTileEntity(t.proxy);
        final String name = t.name();
        final short len = (short) name.length();
        pb.writeShort(len);
        for (int x = 0; x < len; x++) {
            pb.writeChar(name.charAt(x));
        }

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendRobotSelectedSlotChange(li.cil.oc.common.tileentity.Robot t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotSelectedSlotChange);

        pb.writeTileEntity(t.proxy);
        pb.writeInt(t.selectedSlot());

        pb.sendToPlayersNearTileEntity(t, Optional.of(Settings.get().maxNetworkClientEffectPacketDistance / 4.0));
    }

    public static void sendRotatableState(Rotatable t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RotatableState);

        pb.writeTileEntity(be(t));
        pb.writeDirection(Optional.ofNullable(t.pitch()));
        pb.writeDirection(Optional.ofNullable(t.yaw()));

        pb.sendToPlayersNearTileEntity(be(t));
    }

    public static void sendSwitchActivity(li.cil.oc.common.tileentity.Relay t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.SwitchActivity);

        pb.writeTileEntity(t);

        pb.sendToPlayersNearTileEntity(t, Optional.of(Settings.get().maxNetworkClientEffectPacketDistance));
    }

    public static void appendTextBufferColorChange(PacketBuilder pb, PackedColor.Color foreground, PackedColor.Color background) {
        pb.writePacketType(PacketType.TextBufferMultiColorChange);

        pb.writeInt(foreground.value);
        pb.writeBoolean(foreground.isPalette);
        pb.writeInt(background.value);
        pb.writeBoolean(background.isPalette);
    }

    public static void appendTextBufferCopy(PacketBuilder pb, int col, int row, int w, int h, int tx, int ty) {
        pb.writePacketType(PacketType.TextBufferMultiCopy);

        pb.writeInt(col);
        pb.writeInt(row);
        pb.writeInt(w);
        pb.writeInt(h);
        pb.writeInt(tx);
        pb.writeInt(ty);
    }

    public static void appendTextBufferDepthChange(PacketBuilder pb, li.cil.oc.api.internal.TextBuffer.ColorDepth value) {
        pb.writePacketType(PacketType.TextBufferMultiDepthChange);

        pb.writeInt(value.ordinal());
    }

    public static void appendTextBufferFill(PacketBuilder pb, int col, int row, int w, int h, char c) {
        pb.writePacketType(PacketType.TextBufferMultiFill);

        pb.writeInt(col);
        pb.writeInt(row);
        pb.writeInt(w);
        pb.writeInt(h);
        pb.writeChar(c);
    }

    public static void appendTextBufferPaletteChange(PacketBuilder pb, int index, int color) {
        pb.writePacketType(PacketType.TextBufferMultiPaletteChange);

        pb.writeInt(index);
        pb.writeInt(color);
    }

    public static void appendTextBufferResolutionChange(PacketBuilder pb, int w, int h) {
        pb.writePacketType(PacketType.TextBufferMultiResolutionChange);

        pb.writeInt(w);
        pb.writeInt(h);
    }

    public static void appendTextBufferViewportResolutionChange(PacketBuilder pb, int w, int h) {
        pb.writePacketType(PacketType.TextBufferMultiViewportResolutionChange);

        pb.writeInt(w);
        pb.writeInt(h);
    }

    public static void appendTextBufferMaxResolutionChange(PacketBuilder pb, int w, int h) {
        pb.writePacketType(PacketType.TextBufferMultiMaxResolutionChange);

        pb.writeInt(w);
        pb.writeInt(h);
    }

    public static void appendTextBufferSet(PacketBuilder pb, int col, int row, String s, boolean vertical) {
        pb.writePacketType(PacketType.TextBufferMultiSet);

        pb.writeInt(col);
        pb.writeInt(row);
        pb.writeUTF(s);
        pb.writeBoolean(vertical);
    }

    public static void appendTextBufferBitBlt(PacketBuilder pb, int col, int row, int w, int h, String owner, int id, int fromCol, int fromRow) {
        pb.writePacketType(PacketType.TextBufferBitBlt);

        pb.writeInt(col);
        pb.writeInt(row);
        pb.writeInt(w);
        pb.writeInt(h);
        pb.writeUTF(owner);
        pb.writeInt(id);
        pb.writeInt(fromCol);
        pb.writeInt(fromRow);
    }

    public static void appendTextBufferRamInit(PacketBuilder pb, String address, int id, CompoundTag nbt) {
        pb.writePacketType(PacketType.TextBufferRamInit);

        pb.writeUTF(address);
        pb.writeInt(id);
        pb.writeNBT(nbt);
    }

    public static void appendTextBufferRamDestroy(PacketBuilder pb, String owner, int id) {
        pb.writePacketType(PacketType.TextBufferRamDestroy);
        pb.writeUTF(owner);
        pb.writeInt(id);
    }

    public static void appendTextBufferRawSetText(PacketBuilder pb, int col, int row, char[][] text) {
        pb.writePacketType(PacketType.TextBufferMultiRawSetText);

        pb.writeInt(col);
        pb.writeInt(row);
        final short height = (short) text.length;
        pb.writeShort(height);
        for (int y = 0; y < height; y++) {
            final char[] line = text[y];
            final short width = (short) line.length;
            pb.writeShort(width);
            for (int x = 0; x < width; x++) {
                pb.writeChar(line[x]);
            }
        }
    }

    public static void appendTextBufferRawSetBackground(PacketBuilder pb, int col, int row, int[][] color) {
        pb.writePacketType(PacketType.TextBufferMultiRawSetBackground);
        writeColorMatrix(pb, col, row, color);
    }

    public static void appendTextBufferRawSetForeground(PacketBuilder pb, int col, int row, int[][] color) {
        pb.writePacketType(PacketType.TextBufferMultiRawSetForeground);
        writeColorMatrix(pb, col, row, color);
    }

    private static void writeColorMatrix(PacketBuilder pb, int col, int row, int[][] color) {
        pb.writeInt(col);
        pb.writeInt(row);
        final short height = (short) color.length;
        pb.writeShort(height);
        for (int y = 0; y < height; y++) {
            final int[] line = color[y];
            final short width = (short) line.length;
            pb.writeShort(width);
            for (int x = 0; x < width; x++) {
                pb.writeInt(line[x]);
            }
        }
    }

    public static void sendTextBufferInit(String address, CompoundTag value, ServerPlayer player) {
        final CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.TextBufferInit);

        pb.writeUTF(address);
        pb.writeNBT(value);

        pb.sendToPlayer(player);
    }

    public static void sendTextBufferPowerChange(String address, boolean hasPower, EnvironmentHost host) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.TextBufferPowerChange);

        pb.writeUTF(address);
        pb.writeBoolean(hasPower);

        pb.sendToPlayersNearHost(host);
    }

    public static void sendScreenTouchMode(li.cil.oc.common.tileentity.Screen t, boolean value) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ScreenTouchMode);

        pb.writeTileEntity(t);
        pb.writeBoolean(value);

        pb.sendToPlayersNearTileEntity(t);
    }

    public static void sendSound(Level world, double x, double y, double z, ResourceLocation sound, SoundSource category, double range) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.SoundEffect);

        pb.writeUTF(world.dimension().location().toString());
        pb.writeDouble(x);
        pb.writeDouble(y);
        pb.writeDouble(z);
        pb.writeUTF(sound.toString());
        pb.writeByte(category.ordinal());
        pb.writeFloat((float) range);

        pb.sendToNearbyPlayers(world, x, y, z, Optional.of(range));
    }

    public static void sendSound(Level world, double x, double y, double z, int frequency, int duration) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.Sound);

        final BlockPosition blockPos = BlockPosition.apply(x, y, z);
        pb.writeUTF(world.dimension().location().toString());
        pb.writeInt(blockPos.x);
        pb.writeInt(blockPos.y);
        pb.writeInt(blockPos.z);
        pb.writeShort((short) frequency);
        pb.writeShort((short) duration);

        pb.sendToNearbyPlayers(world, x, y, z, Optional.of(Settings.get().maxNetworkClientSoundPacketDistance));
    }

    public static void sendSound(Level world, double x, double y, double z, String pattern) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.SoundPattern);

        final BlockPosition blockPos = BlockPosition.apply(x, y, z);
        pb.writeUTF(world.dimension().location().toString());
        pb.writeInt(blockPos.x);
        pb.writeInt(blockPos.y);
        pb.writeInt(blockPos.z);
        pb.writeUTF(pattern);

        pb.sendToNearbyPlayers(world, x, y, z, Optional.of(Settings.get().maxNetworkClientSoundPacketDistance));
    }

    public static void sendTransposerActivity(li.cil.oc.common.tileentity.Transposer t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.TransposerActivity);

        pb.writeTileEntity(t);

        pb.sendToPlayersNearTileEntity(t, Optional.of(Settings.get().maxNetworkClientEffectPacketDistance / 2.0));
    }

    public static void sendWaypointLabel(Waypoint t) {
        final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.WaypointLabel);

        pb.writeTileEntity(t);
        pb.writeUTF(t.label);

        pb.sendToPlayersNearTileEntity(t);
    }
}
