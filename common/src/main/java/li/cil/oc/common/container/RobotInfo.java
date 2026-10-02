package li.cil.oc.common.container;

import li.cil.oc.api.Driver;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.integration.opencomputers.DriverKeyboard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Robot information sent to the client when opening the robot GUI (was declared in container/Robot.scala).
 */
public class RobotInfo {
    public final int mainInvSize;
    public final String slot1;
    public final int tier1;
    public final String slot2;
    public final int tier2;
    public final String slot3;
    public final int tier3;
    public final Optional<String> screenBuffer;
    public final boolean hasKeyboard;

    public RobotInfo(int mainInvSize, String slot1, int tier1, String slot2, int tier2, String slot3, int tier3,
                     Optional<String> screenBuffer, boolean hasKeyboard) {
        this.mainInvSize = mainInvSize;
        this.slot1 = slot1;
        this.tier1 = tier1;
        this.slot2 = slot2;
        this.tier2 = tier2;
        this.slot3 = slot3;
        this.tier3 = tier3;
        this.screenBuffer = screenBuffer;
        this.hasKeyboard = hasKeyboard;
    }

    public RobotInfo(li.cil.oc.common.tileentity.Robot robot) {
        this(robot.mainInventory().getContainerSize(), robot.containerSlotType(1), robot.containerSlotTier(1),
            robot.containerSlotType(2), robot.containerSlotTier(2), robot.containerSlotType(3), robot.containerSlotTier(3),
            getScreenBuffer(robot), hasKeyboard(robot));
    }

    public static Optional<String> getScreenBuffer(li.cil.oc.common.tileentity.Robot robot) {
        for (Optional<ManagedEnvironment> component : robot.components()) {
            if (component.isPresent() && component.get() instanceof li.cil.oc.api.internal.TextBuffer buffer && buffer.node() != null) {
                return Optional.of(buffer.node().address());
            }
        }
        return Optional.empty();
    }

    public static boolean hasKeyboard(li.cil.oc.common.tileentity.Robot robot) {
        for (ItemStack stack : robot.info.components) {
            if (Driver.driverFor(stack, robot.getClass()) == DriverKeyboard.INSTANCE) return true;
        }
        return false;
    }

    public static RobotInfo readRobotInfo(FriendlyByteBuf buff) {
        int mainInvSize = buff.readVarInt();
        String slot1 = buff.readUtf(32);
        int tier1 = buff.readVarInt();
        String slot2 = buff.readUtf(32);
        int tier2 = buff.readVarInt();
        String slot3 = buff.readUtf(32);
        int tier3 = buff.readVarInt();
        Optional<String> screenBuffer = buff.readBoolean() ? Optional.of(buff.readUtf()) : Optional.empty();
        boolean hasKeyboard = buff.readBoolean();
        return new RobotInfo(mainInvSize, slot1, tier1, slot2, tier2, slot3, tier3, screenBuffer, hasKeyboard);
    }

    public static void writeRobotInfo(FriendlyByteBuf buff, RobotInfo info) {
        buff.writeVarInt(info.mainInvSize);
        buff.writeUtf(info.slot1, 32);
        buff.writeVarInt(info.tier1);
        buff.writeUtf(info.slot2, 32);
        buff.writeVarInt(info.tier2);
        buff.writeUtf(info.slot3, 32);
        buff.writeVarInt(info.tier3);
        if (info.screenBuffer.isPresent()) {
            buff.writeBoolean(true);
            buff.writeUtf(info.screenBuffer.get());
        }
        else buff.writeBoolean(false);
        buff.writeBoolean(info.hasKeyboard);
    }
}
