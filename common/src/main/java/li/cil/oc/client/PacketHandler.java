package li.cil.oc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.client.renderer.PetRenderer;
import li.cil.oc.common.Loot;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.component.ClientGpuTextBufferHandler;
import li.cil.oc.common.item.Tablet;
import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.common.nanomachines.ControllerImpl;
import li.cil.oc.common.tileentity.Adapter;
import li.cil.oc.common.tileentity.Assembler;
import li.cil.oc.common.tileentity.Charger;
import li.cil.oc.common.tileentity.Disassembler;
import li.cil.oc.common.tileentity.DiskDrive;
import li.cil.oc.common.tileentity.Hologram;
import li.cil.oc.common.tileentity.NetSplitter;
import li.cil.oc.common.tileentity.Printer;
import li.cil.oc.common.tileentity.Rack;
import li.cil.oc.common.tileentity.Raid;
import li.cil.oc.common.tileentity.Relay;
import li.cil.oc.common.tileentity.Robot;
import li.cil.oc.common.tileentity.RobotProxy;
import li.cil.oc.common.tileentity.Screen;
import li.cil.oc.common.tileentity.Transposer;
import li.cil.oc.common.tileentity.Waypoint;
import li.cil.oc.common.tileentity.traits.Computer;
import li.cil.oc.common.tileentity.traits.PowerInformation;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.util.Audio;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.zip.InflaterInputStream;

/**
 * Handles server to client packets.
 * <p>
 * Transport: Architectury {@link NetworkManager}, S2C receiver on channel
 * {@code opencomputers:main} (see {@link #registerClientReceiver()}). The
 * payload is OC's own packet format written as a single byte array
 * ({@code FriendlyByteBuf#writeByteArray}): first byte 0 = uncompressed, else
 * deflate compressed, followed by the packet type byte and the data.
 */
public final class PacketHandler extends li.cil.oc.common.PacketHandler {
    public static final PacketHandler INSTANCE = new PacketHandler();

    public static final ResourceLocation CHANNEL = new ResourceLocation(OpenComputers.ID, "main");

    private static boolean receiverRegistered = false;

    private PacketHandler() {
    }

    /**
     * Registers the S2C receiver for OC's packets. Client only; called by
     * {@link Proxy#initClient()}.
     */
    public static void registerClientReceiver() {
        if (receiverRegistered) return;
        receiverRegistered = true;
        NetworkManager.registerReceiver(NetworkManager.s2c(), CHANNEL, (buf, context) -> {
            final byte[] data = buf.readByteArray();
            context.queue(() -> INSTANCE.handleClientPacket(data, context.getPlayer()));
        });
    }

    /**
     * Client side equivalent of the common {@code handlePacket} for
     * {@code PLAY_TO_CLIENT}.
     */
    public void handleClientPacket(byte[] arr, Player player) {
        try {
            final ByteArrayInputStream stream = new ByteArrayInputStream(arr);
            if (stream.read() == 0) dispatch(createParser(stream, player));
            else dispatch(createParser(new InflaterInputStream(stream), player));
        } catch (Throwable e) {
            // Don't crash on badly formatted packets.
            OpenComputers.log.warn("Received a badly formatted packet.", e);
        }
    }

    @Override
    protected Optional<Level> world(Player player, ResourceLocation dimension) {
        final Level world = player.level();
        if (world.dimension().location().equals(dimension)) return Optional.of(world);
        return Optional.empty();
    }

    @Override
    protected void dispatch(PacketParser p) {
        try {
            switch (p.packetType) {
                case AdapterState: onAdapterState(p); break;
                case Analyze: onAnalyze(p); break;
                case ChargerState: onChargerState(p); break;
                case ClientLog: onClientLog(p); break;
                case Clipboard: onClipboard(p); break;
                case ColorChange: onColorChange(p); break;
                case MachineItemStateResponse: onMachineItemStateResponse(p); break;
                case ComputerState: onComputerState(p); break;
                case ComputerUserList: onComputerUserList(p); break;
                case ContainerUpdate: onContainerUpdate(p); break;
                case DisassemblerActiveChange: onDisassemblerActiveChange(p); break;
                case FileSystemActivity: onFileSystemActivity(p); break;
                case FloppyChange: onFloppyChange(p); break;
                case HologramArea: onHologramArea(p); break;
                case HologramClear: onHologramClear(p); break;
                case HologramColor: onHologramColor(p); break;
                case HologramPowerChange: onHologramPowerChange(p); break;
                case HologramRotation: onHologramRotation(p); break;
                case HologramRotationSpeed: onHologramRotationSpeed(p); break;
                case HologramScale: onHologramScale(p); break;
                case HologramTranslation: onHologramPositionOffsetY(p); break;
                case HologramValues: onHologramValues(p); break;
                case LootDisk: onLootDisk(p); break;
                case CyclingDisk: onCyclingDisk(p); break;
                case NanomachinesConfiguration: onNanomachinesConfiguration(p); break;
                case NanomachinesInputs: onNanomachinesInputs(p); break;
                case NanomachinesPower: onNanomachinesPower(p); break;
                case NetSplitterState: onNetSplitterState(p); break;
                case NetworkActivity: onNetworkActivity(p); break;
                case ParticleEffect: onParticleEffect(p); break;
                case PetVisibility: onPetVisibility(p); break;
                case PowerState: onPowerState(p); break;
                case PrinterState: onPrinterState(p); break;
                case RackInventory: onRackInventory(p); break;
                case RackMountableData: onRackMountableData(p); break;
                case RaidStateChange: onRaidStateChange(p); break;
                case RedstoneState: onRedstoneState(p); break;
                case RobotAnimateSwing: onRobotAnimateSwing(p); break;
                case RobotAnimateTurn: onRobotAnimateTurn(p); break;
                case RobotAssemblingState: onRobotAssemblingState(p); break;
                case RobotInventoryChange: onRobotInventoryChange(p); break;
                case RobotLightChange: onRobotLightChange(p); break;
                case RobotMove: onRobotMove(p); break;
                case RobotNameChange: onRobotNameChange(p); break;
                case RobotSelectedSlotChange: onRobotSelectedSlotChange(p); break;
                case RotatableState: onRotatableState(p); break;
                case SwitchActivity: onSwitchActivity(p); break;
                case TextBufferInit: onTextBufferInit(p); break;
                case TextBufferPowerChange: onTextBufferPowerChange(p); break;
                case TextBufferMulti: onTextBufferMulti(p); break;
                case ScreenTouchMode: onScreenTouchMode(p); break;
                case SoundEffect: onSoundEffect(p); break;
                case Sound: onSound(p); break;
                case SoundPattern: onSoundPattern(p); break;
                case TransposerActivity: onTransposerActivity(p); break;
                case WaypointLabel: onWaypointLabel(p); break;
                default: // Invalid packet.
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected PacketParser createParser(InputStream stream, Player player) {
        try {
            return new PacketParser(stream, Minecraft.getInstance().player);
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) throw runtimeException;
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------------- //

    public void onAdapterState(PacketParser p) throws IOException {
        final Optional<Adapter> te = p.readBlockEntity(Adapter.class);
        if (te.isPresent()) {
            final Adapter t = te.get();
            t.setOpenSides(t.uncompressSides(p.readByte()));
            ExtendedWorld.notifyBlockUpdate(t.getLevel(), t.getBlockPos());
        }
    }

    public void onAnalyze(PacketParser p) throws IOException {
        final String address = p.readUTF();
        if (KeyBindings.isAnalyzeCopyingAddress()) {
            RenderSystem.recordRenderCall(() -> {
                final Minecraft mc = Minecraft.getInstance();
                mc.keyboardHandler.setClipboard(address);
                mc.gui.getChat().addMessage(Localization.Analyzer.AddressCopied());
            });
        }
    }

    public void onChargerState(PacketParser p) throws IOException {
        final Optional<Charger> te = p.readBlockEntity(Charger.class);
        if (te.isPresent()) {
            final Charger t = te.get();
            t.chargeSpeed = p.readDouble();
            t.hasPower = p.readBoolean();
            ExtendedWorld.notifyBlockUpdate(t.getLevel(), t.getBlockPos());
        }
    }

    public void onClientLog(PacketParser p) throws IOException {
        OpenComputers.log.info(p.readUTF());
    }

    public void onClipboard(PacketParser p) throws IOException {
        final String contents = p.readUTF();
        RenderSystem.recordRenderCall(() -> Minecraft.getInstance().keyboardHandler.setClipboard(contents));
    }

    public void onColorChange(PacketParser p) throws IOException {
        final Optional<li.cil.oc.api.internal.Colored> te = p.readBlockEntity(li.cil.oc.api.internal.Colored.class);
        if (te.isPresent()) {
            te.get().setColor(p.readInt());
            if (te.get() instanceof BlockEntity t) {
                ExtendedWorld.notifyBlockUpdate(t.getLevel(), t.getBlockPos());
            }
        }
    }

    public void onMachineItemStateResponse(PacketParser p) throws IOException {
        final ItemStack stack = p.readItemStack();
        final boolean running = p.readBoolean();
        final TabletWrapper wrapper = Tablet.get(stack, p.player);

        wrapper.data.isRunning = running;
        wrapper.isDirty = false;
    }

    public void onComputerState(PacketParser p) throws IOException {
        final Optional<Computer> te = p.readBlockEntity(Computer.class);
        if (te.isPresent()) {
            te.get().setRunning(p.readBoolean());
            te.get().setHasErrored(p.readBoolean());
        }
    }

    public void onComputerUserList(PacketParser p) throws IOException {
        final Optional<Computer> te = p.readBlockEntity(Computer.class);
        if (te.isPresent()) {
            final int count = p.readInt();
            final List<String> users = new ArrayList<>(count);
            for (int i = 0; i < count; i++) users.add(p.readUTF());
            te.get().setUsers(users);
        }
    }

    public void onContainerUpdate(PacketParser p) throws IOException {
        final int containerId = p.readInt();
        if (p.player.containerMenu != null && p.player.containerMenu.containerId == containerId) {
            if (p.player.containerMenu instanceof li.cil.oc.common.container.Player container) {
                container.updateCustomData(p.readNBT());
            }
        }
    }

    public void onDisassemblerActiveChange(PacketParser p) throws IOException {
        final Optional<Disassembler> te = p.readBlockEntity(Disassembler.class);
        if (te.isPresent()) te.get().isActive = p.readBoolean();
    }

    public void onFileSystemActivity(PacketParser p) throws IOException {
        final String sound = p.readUTF();
        final CompoundTag data = NbtIo.read(p);
        if (p.readBoolean()) {
            final Optional<BlockEntity> te = p.readBlockEntity(BlockEntity.class);
            te.ifPresent(t -> EventBus.INSTANCE.post(new FileSystemAccessEvent.Client(sound, t, data)));
        } else {
            final Optional<Level> world = world(p.player, new ResourceLocation(p.readUTF()));
            if (world.isPresent()) {
                final double x = p.readDouble();
                final double y = p.readDouble();
                final double z = p.readDouble();
                EventBus.INSTANCE.post(new FileSystemAccessEvent.Client(sound, world.get(), x, y, z, data));
            }
        }
    }

    public void onNetworkActivity(PacketParser p) throws IOException {
        final CompoundTag data = NbtIo.read(p);
        if (p.readBoolean()) {
            final Optional<BlockEntity> te = p.readBlockEntity(BlockEntity.class);
            te.ifPresent(t -> EventBus.INSTANCE.post(new NetworkActivityEvent.Client(t, data)));
        } else {
            final Optional<Level> world = world(p.player, new ResourceLocation(p.readUTF()));
            if (world.isPresent()) {
                final double x = p.readDouble();
                final double y = p.readDouble();
                final double z = p.readDouble();
                EventBus.INSTANCE.post(new NetworkActivityEvent.Client(world.get(), x, y, z, data));
            }
        }
    }

    public void onFloppyChange(PacketParser p) throws IOException {
        final Optional<DiskDrive> te = p.readBlockEntity(DiskDrive.class);
        if (te.isPresent()) te.get().setItem(0, p.readItemStack());
    }

    public void onHologramClear(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) {
            final Hologram t = te.get();
            java.util.Arrays.fill(t.volume, 0);
            t.needsRendering = true;
        }
    }

    public void onHologramColor(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) {
            final Hologram t = te.get();
            final int index = p.readInt();
            final int value = p.readInt();
            t.colors()[index] = value & 0xFFFFFF;
            t.needsRendering = true;
        }
    }

    public void onHologramPowerChange(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) te.get().hasPower = p.readBoolean();
    }

    public void onHologramScale(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) te.get().scale = p.readDouble();
    }

    public void onHologramArea(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) {
            final Hologram t = te.get();
            final int fromX = p.readByte();
            final int untilX = p.readByte();
            final int fromZ = p.readByte();
            final int untilZ = p.readByte();
            for (int x = fromX; x < untilX; x++) {
                for (int z = fromZ; z < untilZ; z++) {
                    t.volume[x + z * t.width] = p.readInt();
                    t.volume[x + z * t.width + t.width * t.width] = p.readInt();
                }
            }
            t.needsRendering = true;
        }
    }

    public void onHologramValues(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) {
            final Hologram t = te.get();
            final int count = p.readInt();
            for (int i = 0; i < count; i++) {
                final short xz = p.readShort();
                final byte x = (byte) (xz >> 8);
                final byte z = (byte) xz;
                t.volume[x + z * t.width] = p.readInt();
                t.volume[x + z * t.width + t.width * t.width] = p.readInt();
            }
            t.needsRendering = true;
        }
    }

    public void onHologramPositionOffsetY(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) {
            final double x = p.readDouble();
            final double y = p.readDouble();
            final double z = p.readDouble();
            te.get().translation = new Vec3(x, y, z);
        }
    }

    public void onHologramRotation(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) {
            final Hologram t = te.get();
            t.rotationAngle = p.readFloat();
            t.rotationX = p.readFloat();
            t.rotationY = p.readFloat();
            t.rotationZ = p.readFloat();
        }
    }

    public void onHologramRotationSpeed(PacketParser p) throws IOException {
        final Optional<Hologram> te = p.readBlockEntity(Hologram.class);
        if (te.isPresent()) {
            final Hologram t = te.get();
            t.rotationSpeed = p.readFloat();
            t.rotationSpeedX = p.readFloat();
            t.rotationSpeedY = p.readFloat();
            t.rotationSpeedZ = p.readFloat();
        }
    }

    public void onLootDisk(PacketParser p) throws IOException {
        final ItemStack stack = p.readItemStack();
        if (!stack.isEmpty()) {
            Loot.disksForClient.add(stack);
        }
        // TODO(port): integration (JEI: ModJEI.addDiskAtRuntime(stack)).
    }

    public void onCyclingDisk(PacketParser p) throws IOException {
        final ItemStack stack = p.readItemStack();
        if (!stack.isEmpty()) {
            Loot.disksForCyclingClient.add(stack);
        }
    }

    public void onNanomachinesConfiguration(PacketParser p) throws IOException {
        final Optional<Player> entity = p.readEntity(Player.class);
        if (entity.isPresent()) {
            final Player player = entity.get();
            final boolean hasController = p.readBoolean();
            if (hasController) {
                final Controller controller = li.cil.oc.api.Nanomachines.installController(player);
                if (controller instanceof ControllerImpl impl) impl.loadData(p.readNBT());
            } else {
                li.cil.oc.api.Nanomachines.uninstallController(player);
            }
        }
    }

    public void onNanomachinesInputs(PacketParser p) throws IOException {
        final Optional<Player> entity = p.readEntity(Player.class);
        if (entity.isPresent()) {
            final Controller c = li.cil.oc.api.Nanomachines.getController(entity.get());
            if (c instanceof ControllerImpl controller) {
                final byte[] inputs = new byte[p.readInt()];
                p.readFully(inputs);
                synchronized (controller.configuration) {
                    for (int index = 0; index < inputs.length; index++) {
                        if (index < controller.configuration.triggers.size()) {
                            controller.configuration.triggers.get(index).isActive = inputs[index] == 1;
                        }
                    }
                    controller.activeBehaviorsDirty = true;
                }
            }
        }
    }

    public void onNanomachinesPower(PacketParser p) throws IOException {
        final Optional<Player> entity = p.readEntity(Player.class);
        if (entity.isPresent()) {
            final Controller c = li.cil.oc.api.Nanomachines.getController(entity.get());
            if (c instanceof ControllerImpl controller) controller.storedEnergy = p.readDouble();
        }
    }

    public void onNetSplitterState(PacketParser p) throws IOException {
        final Optional<NetSplitter> te = p.readBlockEntity(NetSplitter.class);
        if (te.isPresent()) {
            final NetSplitter t = te.get();
            t.isInverted = p.readBoolean();
            t.setOpenSides(t.uncompressSides(p.readByte()));
            ExtendedWorld.notifyBlockUpdate(t.getLevel(), t.getBlockPos());
        }
    }

    public void onParticleEffect(PacketParser p) throws IOException {
        final Optional<Level> w = world(p.player, new ResourceLocation(p.readUTF()));
        if (w.isEmpty()) return;
        final Level world = w.get();
        final int x = p.readInt();
        final int y = p.readInt();
        final int z = p.readInt();
        final double velocity = p.readDouble();
        final Optional<Direction> direction = p.readDirection();
        final Object particleType = p.readRegistryEntry(BuiltInRegistries.PARTICLE_TYPE);
        if (particleType instanceof ParticleOptions particle) {
            final int count = p.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                final double vx = rv(world, direction, Direction::getStepX);
                final double vy = rv(world, direction, Direction::getStepY);
                final double vz = rv(world, direction, Direction::getStepZ);
                if (vx * vx + vy * vy + vz * vz < 1) {
                    final double px = rp(x, vx, velocity, direction, Direction::getStepX);
                    final double py = rp(y, vy, velocity, direction, Direction::getStepY);
                    final double pz = rp(z, vz, velocity, direction, Direction::getStepZ);
                    world.addParticle(particle, px, py, pz, vx, vy + velocity * 0.25, vz);
                }
            }
        }
    }

    private static double rv(Level world, Optional<Direction> direction, ToIntFunction<Direction> f) {
        if (direction.isPresent()) return world.random.nextFloat() - 0.5 + f.applyAsInt(direction.get()) * 0.5;
        return world.random.nextFloat() * 2.0 - 1;
    }

    private static double rp(int x, double v, double velocity, Optional<Direction> direction, ToIntFunction<Direction> f) {
        if (direction.isPresent()) return x + 0.5 + v * velocity * 0.5 + f.applyAsInt(direction.get()) * velocity;
        return x + 0.5 + v * velocity;
    }

    public void onPetVisibility(PacketParser p) throws IOException {
        if (!PetRenderer.isInitialized) {
            PetRenderer.isInitialized = true;
            if (Settings.get().hideOwnPet) {
                PetRenderer.hidden.add(Minecraft.getInstance().player.getName().getString());
            }
            PacketSender.sendPetVisibility();
        }

        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            final String name = p.readUTF();
            if (p.readBoolean()) {
                PetRenderer.hidden.remove(name);
            } else {
                PetRenderer.hidden.add(name);
            }
        }
    }

    public void onPowerState(PacketParser p) throws IOException {
        final Optional<PowerInformation> te = p.readBlockEntity(PowerInformation.class);
        if (te.isPresent()) {
            te.get().setGlobalBuffer(p.readDouble());
            te.get().setGlobalBufferSize(p.readDouble());
        }
    }

    public void onPrinterState(PacketParser p) throws IOException {
        final Optional<Printer> te = p.readBlockEntity(Printer.class);
        if (te.isPresent()) {
            if (p.readBoolean()) te.get().requiredEnergy = 9001;
            else te.get().requiredEnergy = 0;
        }
    }

    public void onRackInventory(PacketParser p) throws IOException {
        final Optional<Rack> te = p.readBlockEntity(Rack.class);
        if (te.isPresent()) {
            final int count = p.readInt();
            for (int i = 0; i < count; i++) {
                final int slot = p.readInt();
                te.get().setItem(slot, p.readItemStack());
            }
        }
    }

    public void onRackMountableData(PacketParser p) throws IOException {
        final Optional<Rack> te = p.readBlockEntity(Rack.class);
        if (te.isPresent()) {
            final Rack t = te.get();
            final int mountableIndex = p.readInt();
            t.lastData[mountableIndex] = p.readNBT();
            ExtendedWorld.notifyBlockUpdate(t.getLevel(), t.getBlockPos());
        }
    }

    public void onRaidStateChange(PacketParser p) throws IOException {
        final Optional<Raid> te = p.readBlockEntity(Raid.class);
        if (te.isPresent()) {
            final Raid t = te.get();
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                t.presence[slot] = p.readBoolean();
            }
        }
    }

    public void onRedstoneState(PacketParser p) throws IOException {
        final Optional<RedstoneAware> te = p.readBlockEntity(RedstoneAware.class);
        if (te.isPresent()) {
            final RedstoneAware t = te.get();
            t.setOutputEnabled(p.readBoolean());
            for (Direction d : Direction.values()) {
                t.setOutput(d, p.readByte());
            }
        }
    }

    public void onRobotAnimateSwing(PacketParser p) throws IOException {
        final Optional<RobotProxy> te = p.readBlockEntity(RobotProxy.class);
        if (te.isPresent()) te.get().robot.setAnimateSwing(p.readInt());
    }

    public void onRobotAnimateTurn(PacketParser p) throws IOException {
        final Optional<RobotProxy> te = p.readBlockEntity(RobotProxy.class);
        if (te.isPresent()) {
            final int axis = p.readByte();
            final int ticks = p.readInt();
            te.get().robot.setAnimateTurn(axis, ticks);
        }
    }

    public void onRobotAssemblingState(PacketParser p) throws IOException {
        final Optional<Assembler> te = p.readBlockEntity(Assembler.class);
        if (te.isPresent()) {
            if (p.readBoolean()) te.get().requiredEnergy = 9001;
            else te.get().requiredEnergy = 0;
        }
    }

    public void onRobotInventoryChange(PacketParser p) throws IOException {
        final Optional<RobotProxy> te = p.readBlockEntity(RobotProxy.class);
        if (te.isPresent()) {
            final Robot robot = te.get().robot;
            final int slot = p.readInt();
            final ItemStack stack = p.readItemStack();
            if (slot >= robot.getContainerSize() - robot.componentCount()) {
                robot.info.components[slot - (robot.getContainerSize() - robot.componentCount())] = stack;
            } else robot.setItem(slot, stack);
        }
    }

    public void onRobotLightChange(PacketParser p) throws IOException {
        final Optional<RobotProxy> te = p.readBlockEntity(RobotProxy.class);
        if (te.isPresent()) te.get().robot.info.lightColor = p.readInt();
    }

    public void onRobotNameChange(PacketParser p) throws IOException {
        final Optional<RobotProxy> te = p.readBlockEntity(RobotProxy.class);
        if (te.isPresent()) {
            final short len = p.readShort();
            final char[] name = new char[len];
            for (int x = 0; x < len; x++) {
                name[x] = p.readChar();
            }
            te.get().robot.setName(new String(name));
        }
    }

    public void onRobotMove(PacketParser p) throws IOException {
        final ResourceLocation dimension = new ResourceLocation(p.readUTF());
        final int x = p.readInt();
        final int y = p.readInt();
        final int z = p.readInt();
        final Optional<Direction> direction = p.readDirection();
        final Optional<RobotProxy> te = p.getBlockEntity(RobotProxy.class, dimension, x, y, z);
        if (te.isPresent() && direction.isPresent()) {
            te.get().robot.move(direction.get());
        } else if (direction.isPresent()) {
            final Direction d = direction.get();
            // Invalid packet, robot may be coming from outside our loaded area.
            PacketSender.sendRobotStateRequest(dimension, x + d.getStepX(), y + d.getStepY(), z + d.getStepZ());
        }
    }

    public void onRobotSelectedSlotChange(PacketParser p) throws IOException {
        final Optional<RobotProxy> te = p.readBlockEntity(RobotProxy.class);
        if (te.isPresent()) te.get().robot.selectedSlot = p.readInt();
    }

    public void onRotatableState(PacketParser p) throws IOException {
        final Optional<Rotatable> te = p.readBlockEntity(Rotatable.class);
        if (te.isPresent()) {
            te.get().setPitch(p.readDirection().get());
            te.get().setYaw(p.readDirection().get());
        }
    }

    public void onSwitchActivity(PacketParser p) throws IOException {
        final Optional<Relay> te = p.readBlockEntity(Relay.class);
        if (te.isPresent()) te.get().lastMessage = System.currentTimeMillis();
    }

    public void onTextBufferPowerChange(PacketParser p) throws IOException {
        final Optional<ManagedEnvironment> env = ComponentTracker.INSTANCE.get(p.player.level(), p.readUTF());
        if (env.isPresent() && env.get() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.setRenderingEnabled(p.readBoolean());
        }
    }

    public void onTextBufferInit(PacketParser p) throws IOException {
        final Optional<ManagedEnvironment> env = ComponentTracker.INSTANCE.get(p.player.level(), p.readUTF());
        if (env.isPresent() && env.get() instanceof li.cil.oc.common.component.TextBuffer buffer) {
            final CompoundTag nbt = p.readNBT();
            if (nbt.contains("maxWidth")) {
                final int maxWidth = nbt.getInt("maxWidth");
                final int maxHeight = nbt.getInt("maxHeight");
                buffer.setMaximumResolution(maxWidth, maxHeight);
            }
            buffer.data.loadData(nbt);
            if (nbt.contains("viewportWidth")) {
                final int viewportWidth = nbt.getInt("viewportWidth");
                final int viewportHeight = nbt.getInt("viewportHeight");
                buffer.setViewport(viewportWidth, viewportHeight);
            }
            buffer.proxy.setChanged();
            buffer.markInitialized();
        }
    }

    public void onTextBufferMulti(PacketParser p) throws IOException {
        if (p.player == null) return;
        final Optional<ManagedEnvironment> env = ComponentTracker.INSTANCE.get(p.player.level(), p.readUTF());
        if (env.isPresent() && env.get() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            try {
                while (true) {
                    switch (p.readPacketType()) {
                        case TextBufferMultiColorChange: onTextBufferMultiColorChange(p, buffer); break;
                        case TextBufferMultiCopy: onTextBufferMultiCopy(p, buffer); break;
                        case TextBufferMultiDepthChange: onTextBufferMultiDepthChange(p, buffer); break;
                        case TextBufferMultiFill: onTextBufferMultiFill(p, buffer); break;
                        case TextBufferMultiPaletteChange: onTextBufferMultiPaletteChange(p, buffer); break;
                        case TextBufferMultiResolutionChange: onTextBufferMultiResolutionChange(p, buffer); break;
                        case TextBufferMultiViewportResolutionChange: onTextBufferMultiViewportResolutionChange(p, buffer); break;
                        case TextBufferMultiMaxResolutionChange: onTextBufferMultiMaxResolutionChange(p, buffer); break;
                        case TextBufferMultiSet: onTextBufferMultiSet(p, buffer); break;
                        case TextBufferRamInit: onTextBufferRamInit(p, buffer); break;
                        case TextBufferBitBlt: onTextBufferBitBlt(p, buffer); break;
                        case TextBufferRamDestroy: onTextBufferRamDestroy(p, buffer); break;
                        case TextBufferMultiRawSetText: onTextBufferMultiRawSetText(p, buffer); break;
                        case TextBufferMultiRawSetBackground: onTextBufferMultiRawSetBackground(p, buffer); break;
                        case TextBufferMultiRawSetForeground: onTextBufferMultiRawSetForeground(p, buffer); break;
                        default: // Invalid packet.
                    }
                }
            } catch (EOFException ignored) {
                // No more commands.
            }
        }
    }

    public void onTextBufferMultiColorChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int foreground = p.readInt();
        final boolean foregroundIsPalette = p.readBoolean();
        buffer.setForegroundColor(foreground, foregroundIsPalette);
        final int background = p.readInt();
        final boolean backgroundIsPalette = p.readBoolean();
        buffer.setBackgroundColor(background, backgroundIsPalette);
    }

    public void onTextBufferMultiCopy(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int col = p.readInt();
        final int row = p.readInt();
        final int w = p.readInt();
        final int h = p.readInt();
        final int tx = p.readInt();
        final int ty = p.readInt();
        buffer.copy(col, row, w, h, tx, ty);
    }

    public void onTextBufferMultiDepthChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        buffer.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[p.readInt()]);
    }

    public void onTextBufferMultiFill(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int col = p.readInt();
        final int row = p.readInt();
        final int w = p.readInt();
        final int h = p.readInt();
        final char c = p.readChar();
        buffer.fill(col, row, w, h, c);
    }

    public void onTextBufferMultiPaletteChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int index = p.readInt();
        final int color = p.readInt();
        buffer.setPaletteColor(index, color);
    }

    public void onTextBufferMultiResolutionChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int w = p.readInt();
        final int h = p.readInt();
        buffer.setResolution(w, h);
    }

    public void onTextBufferMultiViewportResolutionChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int w = p.readInt();
        final int h = p.readInt();
        buffer.setViewport(w, h);
    }

    public void onTextBufferMultiMaxResolutionChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int w = p.readInt();
        final int h = p.readInt();
        buffer.setMaximumResolution(w, h);
    }

    public void onTextBufferMultiSet(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int col = p.readInt();
        final int row = p.readInt();
        final String s = p.readUTF();
        final boolean vertical = p.readBoolean();
        buffer.set(col, row, s, vertical);
    }

    public void onTextBufferRamInit(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final String owner = p.readUTF();
        final int id = p.readInt();
        final CompoundTag nbt = p.readNBT();

        ClientGpuTextBufferHandler.loadBuffer(buffer, owner, id, nbt);
    }

    public void onTextBufferBitBlt(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int col = p.readInt();
        final int row = p.readInt();
        final int w = p.readInt();
        final int h = p.readInt();
        final String owner = p.readUTF();
        final int id = p.readInt();
        final int fromCol = p.readInt();
        final int fromRow = p.readInt();

        ClientGpuTextBufferHandler.bitblt(buffer, col, row, w, h, owner, id, fromCol, fromRow);
    }

    public void onTextBufferRamDestroy(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final String owner = p.readUTF();
        final int id = p.readInt();

        ClientGpuTextBufferHandler.removeBuffer(buffer, owner, id);
    }

    public void onTextBufferMultiRawSetText(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int col = p.readInt();
        final int row = p.readInt();

        final short rows = p.readShort();
        final char[][] text = new char[rows][];
        for (int y = 0; y < rows; y++) {
            final short cols = p.readShort();
            final char[] line = new char[cols];
            for (int x = 0; x < cols; x++) {
                line[x] = p.readChar();
            }
            text[y] = line;
        }

        buffer.rawSetText(col, row, text);
    }

    public void onTextBufferMultiRawSetBackground(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int col = p.readInt();
        final int row = p.readInt();
        buffer.rawSetBackground(col, row, readColorMatrix(p));
    }

    public void onTextBufferMultiRawSetForeground(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        final int col = p.readInt();
        final int row = p.readInt();
        buffer.rawSetForeground(col, row, readColorMatrix(p));
    }

    private static int[][] readColorMatrix(PacketParser p) throws IOException {
        final short rows = p.readShort();
        final int[][] color = new int[rows][];
        for (int y = 0; y < rows; y++) {
            final short cols = p.readShort();
            final int[] line = new int[cols];
            for (int x = 0; x < cols; x++) {
                line[x] = p.readInt();
            }
            color[y] = line;
        }
        return color;
    }

    public void onScreenTouchMode(PacketParser p) throws IOException {
        final Optional<Screen> te = p.readBlockEntity(Screen.class);
        if (te.isPresent()) te.get().invertTouchMode = p.readBoolean();
    }

    public void onSoundEffect(PacketParser p) throws IOException {
        final Optional<Level> world = world(p.player, new ResourceLocation(p.readUTF()));
        if (world.isPresent()) {
            final double x = p.readDouble();
            final double y = p.readDouble();
            final double z = p.readDouble();
            final String sound = p.readUTF();
            final SoundSource category = SoundSource.values()[p.readByte()];
            final float range = p.readFloat();
            world.get().playSound(p.player, x, y, z, SoundEvent.createVariableRangeEvent(new ResourceLocation(sound)), category, range / 15 + 0.5F, 1.0F);
        }
    }

    public void onSound(PacketParser p) throws IOException {
        if (world(p.player, new ResourceLocation(p.readUTF())).isPresent()) {
            final int x = p.readInt();
            final int y = p.readInt();
            final int z = p.readInt();
            final short frequency = p.readShort();
            final short duration = p.readShort();
            Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, frequency, duration);
        }
    }

    public void onSoundPattern(PacketParser p) throws IOException {
        if (world(p.player, new ResourceLocation(p.readUTF())).isPresent()) {
            final int x = p.readInt();
            final int y = p.readInt();
            final int z = p.readInt();
            final String pattern = p.readUTF();
            Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, pattern);
        }
    }

    public void onTransposerActivity(PacketParser p) throws IOException {
        final Optional<Transposer> te = p.readBlockEntity(Transposer.class);
        if (te.isPresent()) te.get().lastOperation = System.currentTimeMillis();
    }

    public void onWaypointLabel(PacketParser p) throws IOException {
        final Optional<Waypoint> te = p.readBlockEntity(Waypoint.class);
        if (te.isPresent()) te.get().label = p.readUTF();
    }
}
