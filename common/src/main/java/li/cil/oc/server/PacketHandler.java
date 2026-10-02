package li.cil.oc.server;

import li.cil.oc.Localization;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Achievement;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.common.container.Assembler;
import li.cil.oc.common.entity.DroneInventory;
import li.cil.oc.common.item.Tablet;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.common.item.traits.FileSystemLike;
import li.cil.oc.common.tileentity.Rack;
import li.cil.oc.common.tileentity.RobotProxy;
import li.cil.oc.common.tileentity.Screen;
import li.cil.oc.common.tileentity.Waypoint;
import li.cil.oc.common.tileentity.traits.Computer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;

public final class PacketHandler extends li.cil.oc.common.PacketHandler {
    public static final PacketHandler INSTANCE = new PacketHandler();

    private PacketHandler() {
    }

    @Override
    protected Optional<Level> world(Player player, ResourceLocation dimension) {
        final MinecraftServer server = player != null && player.getServer() != null
                ? player.getServer()
                : dev.architectury.utils.GameInstance.getServer();
        if (server == null) return Optional.empty();
        return Optional.ofNullable(server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension)));
    }

    @Override
    public void dispatch(PacketParser p) {
        switch (p.packetType) {
            case ComputerPower -> onComputerPower(p);
            case CopyToAnalyzer -> onCopyToAnalyzer(p);
            case DriveLock -> onDriveLock(p);
            case DriveMode -> onDriveMode(p);
            case DronePower -> onDronePower(p);
            case KeyDown -> onKeyDown(p);
            case KeyUp -> onKeyUp(p);
            case TextInput -> onTextInput(p);
            case Clipboard -> onClipboard(p);
            case MachineItemStateRequest -> onMachineItemStateRequest(p);
            case MouseClickOrDrag -> onMouseClick(p);
            case MouseScroll -> onMouseScroll(p);
            case MouseUp -> onMouseUp(p);
            case PetVisibility -> onPetVisibility(p);
            case RackMountableMapping -> onRackMountableMapping(p);
            case RackRelayState -> onRackRelayState(p);
            case RobotAssemblerStart -> onRobotAssemblerStart(p);
            case RobotStateRequest -> onRobotStateRequest(p);
            case ServerPower -> onServerPower(p);
            case TextBufferInit -> onTextBufferInit(p);
            case WaypointLabel -> onWaypointLabel(p);
            default -> {
                // Invalid packet.
            }
        }
    }

    private static Optional<li.cil.oc.api.internal.TextBuffer> textBuffer(Player player, String address) {
        final Optional<ManagedEnvironment> env = ComponentTracker.INSTANCE.get(player.level(), address);
        if (env.isPresent() && env.get() instanceof li.cil.oc.api.internal.TextBuffer buffer) return Optional.of(buffer);
        return Optional.empty();
    }

    public void onComputerPower(PacketParser p) {
        final int containerId = p.readInt();
        final boolean setPower = p.readBoolean();
        final AbstractContainerMenu menu = p.player.containerMenu;
        if (menu instanceof li.cil.oc.common.container.Case computer && computer.containerId == containerId) {
            if (computer.otherInventory instanceof Computer te && p.player instanceof ServerPlayer player) {
                trySetComputerPower(te.machine(), setPower, player);
            }
        } else if (menu instanceof li.cil.oc.common.container.Robot robot && robot.containerId == containerId) {
            if (robot.otherInventory instanceof Computer te && p.player instanceof ServerPlayer player) {
                trySetComputerPower(te.machine(), setPower, player);
            }
        }
        // Else: invalid packet or container closed early.
    }

    public void onServerPower(PacketParser p) {
        final int containerId = p.readInt();
        final int index = p.readInt();
        final boolean setPower = p.readBoolean();
        if (p.player.containerMenu instanceof li.cil.oc.common.container.Server server && server.containerId == containerId) {
            if (server.otherInventory instanceof li.cil.oc.server.component.Server comp && p.player instanceof ServerPlayer player
                    && comp.rack.getMountable(index) == comp) {
                trySetComputerPower(comp.machine(), setPower, player);
            }
        }
        // Else: invalid packet or container closed early.
    }

    public void onCopyToAnalyzer(PacketParser p) {
        final String text = p.readUTF();
        final int line = p.readInt();
        final Optional<ManagedEnvironment> env = ComponentTracker.INSTANCE.get(p.player.level(), text);
        if (env.isPresent() && env.get() instanceof TextBuffer buffer) {
            buffer.copyToAnalyzer(line, p.player);
        }
    }

    public void onDriveLock(PacketParser p) {
        if (p.player instanceof ServerPlayer player) {
            final ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof FileSystemLike) {
                DriveData.lock(heldItem, player);
            }
        }
    }

    public void onDriveMode(PacketParser p) {
        final boolean unmanaged = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            final ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof FileSystemLike) {
                DriveData.setUnmanaged(heldItem, unmanaged);
            }
        }
    }

    public void onDronePower(PacketParser p) {
        final int containerId = p.readInt();
        final boolean power = p.readBoolean();
        if (p.player.containerMenu instanceof li.cil.oc.common.container.Drone drone && drone.containerId == containerId) {
            if (drone.otherInventory instanceof DroneInventory droneInv && p.player instanceof ServerPlayer player) {
                trySetComputerPower(droneInv.drone.machine, power, player);
            }
        }
    }

    private void trySetComputerPower(Machine computer, boolean value, ServerPlayer player) {
        if (computer.canInteract(player.getName().getString())) {
            if (value) {
                if (!computer.isPaused()) {
                    computer.start();
                    final String message = computer.lastError();
                    if (message != null) {
                        player.sendSystemMessage(Localization.Analyzer.LastError(message));
                    }
                }
            } else computer.stop();
        }
    }

    public void onKeyDown(PacketParser p) {
        final String address = p.readUTF();
        final char key = p.readChar();
        final int code = p.readInt();
        textBuffer(p.player, address).ifPresent(buffer -> buffer.keyDown(key, code, p.player));
    }

    public void onKeyUp(PacketParser p) {
        final String address = p.readUTF();
        final char key = p.readChar();
        final int code = p.readInt();
        textBuffer(p.player, address).ifPresent(buffer -> buffer.keyUp(key, code, p.player));
    }

    public void onTextInput(PacketParser p) {
        final String address = p.readUTF();
        final int codePt = p.readInt();
        if (codePt >= 0 && codePt <= Character.MAX_CODE_POINT) {
            textBuffer(p.player, address).ifPresent(buffer -> buffer.textInput(codePt, p.player));
        }
    }

    public void onClipboard(PacketParser p) {
        final String address = p.readUTF();
        final String copy = p.readUTF();
        textBuffer(p.player, address).ifPresent(buffer -> buffer.clipboard(copy, p.player));
    }

    public void onMouseClick(PacketParser p) {
        final String address = p.readUTF();
        final float x = p.readFloat();
        final float y = p.readFloat();
        final boolean dragging = p.readBoolean();
        final byte button = p.readByte();
        textBuffer(p.player, address).ifPresent(buffer -> {
            if (dragging) buffer.mouseDrag(x, y, button, p.player);
            else buffer.mouseDown(x, y, button, p.player);
        });
    }

    public void onMouseUp(PacketParser p) {
        final String address = p.readUTF();
        final float x = p.readFloat();
        final float y = p.readFloat();
        final byte button = p.readByte();
        textBuffer(p.player, address).ifPresent(buffer -> buffer.mouseUp(x, y, button, p.player));
    }

    public void onMouseScroll(PacketParser p) {
        final String address = p.readUTF();
        final float x = p.readFloat();
        final float y = p.readFloat();
        final byte button = p.readByte();
        textBuffer(p.player, address).ifPresent(buffer -> buffer.mouseScroll(x, y, button, p.player));
    }

    public void onPetVisibility(PacketParser p) {
        final boolean value = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            final String name = player.getName().getString();
            final boolean changed = value
                    ? PetVisibility.hidden.remove(name)
                    : PetVisibility.hidden.add(name);
            if (changed) {
                // Something changed.
                PacketSender.sendPetVisibility(Optional.of(name));
            }
        }
    }

    public void onRackMountableMapping(PacketParser p) {
        final int containerId = p.readInt();
        final int mountableIndex = p.readInt();
        final int nodeIndex = p.readInt();
        final Optional<Direction> side = p.readDirection();
        if (p.player.containerMenu instanceof li.cil.oc.common.container.Rack rack && rack.containerId == containerId) {
            if (rack.otherInventory instanceof Rack t && p.player instanceof ServerPlayer player && t.stillValid(player)) {
                t.connect(mountableIndex, nodeIndex, side);
            }
        }
    }

    public void onRackRelayState(PacketParser p) {
        final int containerId = p.readInt();
        final boolean enabled = p.readBoolean();
        if (p.player.containerMenu instanceof li.cil.oc.common.container.Rack rack && rack.containerId == containerId) {
            if (rack.otherInventory instanceof Rack t && p.player instanceof ServerPlayer player && t.stillValid(player)) {
                t.isRelayEnabled = enabled;
            }
        }
    }

    public void onRobotAssemblerStart(PacketParser p) {
        final int containerId = p.readInt();
        if (p.player.containerMenu instanceof Assembler assembler && assembler.containerId == containerId) {
            if (assembler.assembler instanceof li.cil.oc.common.tileentity.Assembler te) {
                final boolean creative = p.player instanceof ServerPlayer player && player.isCreative();
                if (te.start(creative) && !te.output.isEmpty()) {
                    Achievement.onAssemble(te.output, p.player);
                }
            }
        }
    }

    public void onRobotStateRequest(PacketParser p) {
        p.readBlockEntity(RobotProxy.class).ifPresent(proxy -> {
            final Level level = proxy.getLevel();
            if (level != null) {
                level.sendBlockUpdated(proxy.getBlockPos(), level.getBlockState(proxy.getBlockPos()), level.getBlockState(proxy.getBlockPos()), 3);
            }
        });
    }

    public void onMachineItemStateRequest(PacketParser p) {
        if (p.player instanceof ServerPlayer player) {
            final ItemStack stack = p.readItemStack();
            PacketSender.sendMachineItemState(player, stack, Tablet.get(stack, p.player).machine().isRunning());
        }
    }

    public void onTextBufferInit(PacketParser p) {
        final String address = p.readUTF();
        if (p.player instanceof ServerPlayer entity) {
            final Optional<ManagedEnvironment> env = ComponentTracker.INSTANCE.get(p.player.level(), address);
            if (env.isPresent() && env.get() instanceof TextBuffer buffer) {
                final boolean isOrigin = !(buffer.host instanceof Screen screen) || screen.isOrigin();
                if (isOrigin) {
                    final CompoundTag nbt = new CompoundTag();
                    buffer.data.saveData(nbt);
                    nbt.putInt("maxWidth", buffer.getMaximumWidth());
                    nbt.putInt("maxHeight", buffer.getMaximumHeight());
                    nbt.putInt("viewportWidth", buffer.getViewportWidth());
                    nbt.putInt("viewportHeight", buffer.getViewportHeight());
                    PacketSender.sendTextBufferInit(address, nbt, entity);
                }
            }
        }
    }

    public void onWaypointLabel(PacketParser p) {
        final Optional<Waypoint> entity = p.readBlockEntity(Waypoint.class);
        final String rawLabel = p.readUTF();
        final String label = rawLabel.length() > 32 ? rawLabel.substring(0, 32) : rawLabel;
        if (entity.isPresent() && p.player instanceof ServerPlayer player) {
            final Waypoint waypoint = entity.get();
            if (player.distanceToSqr(waypoint.getBlockPos().getX() + 0.5, waypoint.getBlockPos().getY() + 0.5, waypoint.getBlockPos().getZ() + 0.5) <= 64) {
                if (!Objects.equals(label, waypoint.label)) {
                    waypoint.label = label;
                    PacketSender.sendWaypointLabel(waypoint);
                }
            }
        }
    }
}
