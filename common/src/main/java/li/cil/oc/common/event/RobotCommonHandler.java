package li.cil.oc.common.event;

import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.common.item.UpgradeHover;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class RobotCommonHandler {
    private RobotCommonHandler() {
    }

    public static void register() {
        EventBus.INSTANCE.register(RobotUsedToolEvent.ApplyDamageRate.class, RobotCommonHandler::onRobotApplyDamageRate);
        EventBus.INSTANCE.register(RobotMoveEvent.Pre.class, RobotCommonHandler::onRobotMove);
    }

    public static void onRobotApplyDamageRate(RobotUsedToolEvent.ApplyDamageRate e) {
        if (e.agent instanceof Robot) {
            if (e.toolAfterUse.isDamageableItem()) {
                int damage = e.toolAfterUse.getDamageValue() - e.toolBeforeUse.getDamageValue();
                if (damage > 0) {
                    double actualDamage = damage * e.getDamageRate();
                    int repairedDamage = e.agent.player().getRandom().nextDouble() > 0.5
                        ? damage - (int) Math.floor(actualDamage)
                        : damage - (int) Math.ceil(actualDamage);
                    e.toolAfterUse.setDamageValue(e.toolAfterUse.getDamageValue() - repairedDamage);
                }
            }
        }
    }

    public static void onRobotMove(RobotMoveEvent.Pre e) {
        if (Settings.get().limitFlightHeight >= 0 && e.agent instanceof Robot robot) {
            Level world = robot.world();
            int maxFlyingHeight = Settings.get().limitFlightHeight;

            for (int i = 0; i < robot.equipmentInventory().getContainerSize(); i++) {
                Item item = robot.equipmentInventory().getItem(i).getItem();
                if (item instanceof UpgradeHover hover) {
                    maxFlyingHeight = Math.max(maxFlyingHeight, Settings.get().upgradeFlightHeight[hover.tier]);
                }
            }

            for (int i = 0; i < robot.componentCount(); i++) {
                int slot = i + robot.mainInventory().getContainerSize() + robot.equipmentInventory().getContainerSize();
                Item item = robot.getItem(slot).getItem();
                if (item instanceof UpgradeHover hover) {
                    maxFlyingHeight = Math.max(maxFlyingHeight, Settings.get().upgradeFlightHeight[hover.tier]);
                }
            }

            boolean isMovingDown = e.direction == Direction.DOWN;
            BlockPosition startPos = BlockPosition.apply(robot);
            BlockPosition targetPos = startPos.offset(e.direction);
            // New movement rules as of 1.5:
            // 1. Robots may only move if the start or target position is valid (e.g. to allow building bridges).
            // 2. The position below a robot is always valid (can always move down).
            // 3. Positions up to <flightHeight> above a block are valid (limited flight capabilities).
            // 4. Any position that has an adjacent block with a solid face towards the position is valid (robots can "climb").
            boolean validMove = isMovingDown ||
                hasAdjacentBlock(world, startPos) ||
                hasAdjacentBlock(world, targetPos) ||
                isWithinFlyingHeight(world, startPos, maxFlyingHeight);

            if (!validMove) {
                e.setCanceled(true);
            }
        }
    }

    private static boolean hasAdjacentBlock(Level world, BlockPosition pos) {
        for (Direction side : Direction.values()) {
            BlockPos sidePos = pos.offset(side).toBlockPos();
            if (world.getBlockState(sidePos).isFaceSturdy(world, sidePos, side.getOpposite())) return true;
        }
        return false;
    }

    private static boolean isWithinFlyingHeight(Level world, BlockPosition pos, int maxFlyingHeight) {
        if (maxFlyingHeight >= world.getHeight()) return true;
        for (int n = 1; n <= maxFlyingHeight; n++) {
            if (!ExtendedWorld.isAirBlock(world, pos.offset(Direction.DOWN, n))) return true;
        }
        return false;
    }
}
