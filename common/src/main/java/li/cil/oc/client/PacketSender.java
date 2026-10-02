package li.cil.oc.client;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.PacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.container.Assembler;
import li.cil.oc.common.container.Case;
import li.cil.oc.common.container.Drone;
import li.cil.oc.common.container.Rack;
import li.cil.oc.common.container.Robot;
import li.cil.oc.common.container.Server;
import li.cil.oc.common.tileentity.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Optional;

/**
 * Client to server packets. Transport ({@link PacketBuilder#sendToServer()}) is
 * Architectury's {@code NetworkManager.sendToServer} on channel {@code opencomputers:main}.
 */
public final class PacketSender {
    private PacketSender() {
    }

    // Timestamp after which the next clipboard message may be sent. Used to
    // avoid spamming large packets on key repeat.
    private static long clipboardCooldown = 0L;

    @FunctionalInterface
    private interface Writer {
        void write() throws IOException;
    }

    private static void send(Writer writer) {
        try {
            writer.write();
        } catch (IOException e) {
            OpenComputers.log.warn("Failed sending packet to server.", e);
        }
    }

    public static void sendComputerPower(Case computer, boolean power) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ComputerPower);

            pb.writeInt(computer.containerId);
            pb.writeBoolean(power);

            pb.sendToServer();
        });
    }

    public static void sendRobotPower(Robot robot, boolean power) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ComputerPower);

            pb.writeInt(robot.containerId);
            pb.writeBoolean(power);

            pb.sendToServer();
        });
    }

    public static void sendDriveMode(boolean unmanaged) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DriveMode);

            pb.writeBoolean(unmanaged);

            pb.sendToServer();
        });
    }

    public static void sendDriveLock() {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DriveLock);

            pb.sendToServer();
        });
    }

    public static void sendDronePower(Drone drone, boolean power) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DronePower);

            pb.writeInt(drone.containerId);
            pb.writeBoolean(power);

            pb.sendToServer();
        });
    }

    public static void sendKeyDown(String address, char character, int code) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.KeyDown);

            pb.writeUTF(address);
            pb.writeChar(character);
            pb.writeInt(code);

            pb.sendToServer();
        });
    }

    public static void sendKeyUp(String address, char character, int code) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.KeyUp);

            pb.writeUTF(address);
            pb.writeChar(character);
            pb.writeInt(code);

            pb.sendToServer();
        });
    }

    public static void sendTextInput(String address, int codePt) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.TextInput);

            pb.writeUTF(address);
            pb.writeInt(codePt);

            pb.sendToServer();
        });
    }

    public static void sendClipboard(String address, String value) {
        if (value != null && !value.isEmpty()) {
            if (value.length() > 64 * 1024 || System.currentTimeMillis() < clipboardCooldown) {
                final SoundManager handler = Minecraft.getInstance().getSoundManager();
                handler.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(), 1, 1));
            } else {
                clipboardCooldown = System.currentTimeMillis() + value.length() / 10;
                for (int start = 0; start < value.length(); start += 16 * 1024) {
                    final String part = value.substring(start, Math.min(value.length(), start + 16 * 1024));
                    send(() -> {
                        final CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.Clipboard);

                        pb.writeUTF(address);
                        pb.writeUTF(part);

                        pb.sendToServer();
                    });
                }
            }
        }
    }

    public static void sendMachineItemStateRequest(ItemStack stack) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MachineItemStateRequest);

            pb.writeItemStack(stack);

            pb.sendToServer();
        });
    }

    public static void sendMouseClick(String address, double x, double y, boolean drag, int button) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MouseClickOrDrag);

            pb.writeUTF(address);
            pb.writeFloat((float) x);
            pb.writeFloat((float) y);
            pb.writeBoolean(drag);
            pb.writeByte((byte) button);

            pb.sendToServer();
        });
    }

    public static void sendMouseScroll(String address, double x, double y, int scroll) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MouseScroll);

            pb.writeUTF(address);
            pb.writeFloat((float) x);
            pb.writeFloat((float) y);
            pb.writeByte(scroll);

            pb.sendToServer();
        });
    }

    public static void sendMouseUp(String address, double x, double y, int button) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MouseUp);

            pb.writeUTF(address);
            pb.writeFloat((float) x);
            pb.writeFloat((float) y);
            pb.writeByte((byte) button);

            pb.sendToServer();
        });
    }

    public static void sendCopyToAnalyzer(String address, int line) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.CopyToAnalyzer);

            pb.writeUTF(address);
            pb.writeInt(line);

            pb.sendToServer();
        });
    }

    public static void sendMultiPlace() {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MultiPartPlace);
            pb.sendToServer();
        });
    }

    public static void sendPetVisibility() {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.PetVisibility);

            pb.writeBoolean(!Settings.get().hideOwnPet);

            pb.sendToServer();
        });
    }

    public static void sendRackMountableMapping(Rack rack, int mountableIndex, int nodeIndex, Optional<Direction> side) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RackMountableMapping);

            pb.writeInt(rack.containerId);
            pb.writeInt(mountableIndex);
            pb.writeInt(nodeIndex);
            pb.writeDirection(side);

            pb.sendToServer();
        });
    }

    public static void sendRackRelayState(Rack rack, boolean enabled) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RackRelayState);

            pb.writeInt(rack.containerId);
            pb.writeBoolean(enabled);

            pb.sendToServer();
        });
    }

    public static void sendRobotAssemblerStart(Assembler assembler) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotAssemblerStart);

            pb.writeInt(assembler.containerId);

            pb.sendToServer();
        });
    }

    public static void sendRobotStateRequest(ResourceLocation dimension, int x, int y, int z) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotStateRequest);

            pb.writeUTF(dimension.toString());
            pb.writeInt(x);
            pb.writeInt(y);
            pb.writeInt(z);

            pb.sendToServer();
        });
    }

    public static void sendServerPower(Server server, int mountableIndex, boolean power) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ServerPower);

            pb.writeInt(server.containerId);
            pb.writeInt(mountableIndex);
            pb.writeBoolean(power);

            pb.sendToServer();
        });
    }

    public static void sendTextBufferInit(String address) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.TextBufferInit);

            pb.writeUTF(address);

            pb.sendToServer();
        });
    }

    public static void sendWaypointLabel(Waypoint t) {
        send(() -> {
            final SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.WaypointLabel);

            pb.writeTileEntity(t);
            pb.writeUTF(t.label);

            pb.sendToServer();
        });
    }
}
