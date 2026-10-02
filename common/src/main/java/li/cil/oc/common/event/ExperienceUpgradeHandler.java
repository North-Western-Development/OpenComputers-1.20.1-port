package li.cil.oc.common.event;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotAnalyzeEvent;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeExperience;

import java.util.function.Consumer;

public final class ExperienceUpgradeHandler {
    private ExperienceUpgradeHandler() {
    }

    public static void register() {
        EventBus.INSTANCE.register(RobotAnalyzeEvent.class, ExperienceUpgradeHandler::onRobotAnalyze);
        EventBus.INSTANCE.register(RobotUsedToolEvent.ComputeDamageRate.class, ExperienceUpgradeHandler::onRobotComputeDamageRate);
        EventBus.INSTANCE.register(RobotBreakBlockEvent.Pre.class, ExperienceUpgradeHandler::onRobotBreakBlockPre);
        EventBus.INSTANCE.register(RobotAttackEntityEvent.Post.class, ExperienceUpgradeHandler::onRobotAttackEntityPost);
        EventBus.INSTANCE.register(RobotBreakBlockEvent.Post.class, ExperienceUpgradeHandler::onRobotBreakBlockPost);
        EventBus.INSTANCE.register(RobotPlaceBlockEvent.Post.class, ExperienceUpgradeHandler::onRobotPlaceBlockPost);
        EventBus.INSTANCE.register(RobotMoveEvent.Post.class, ExperienceUpgradeHandler::onRobotMovePost);
        EventBus.INSTANCE.register(RobotExhaustionEvent.class, ExperienceUpgradeHandler::onRobotExhaustion);
        EventBus.INSTANCE.register(RobotRenderEvent.class, ExperienceUpgradeHandler::onRobotRender);
    }

    public static void onRobotAnalyze(RobotAnalyzeEvent e) {
        int[] level = {0};
        double[] experience = {0.0};
        foreachUpgrade(e.agent.machine().node(), upgrade -> {
            level[0] += upgrade.level;
            experience[0] += upgrade.experience;
        });
        // This is basically a 'does it have an experience upgrade' check.
        if (experience[0] != 0.0) {
            e.player.sendSystemMessage(Localization.Analyzer.RobotXp(experience[0], level[0]));
        }
    }

    public static void onRobotComputeDamageRate(RobotUsedToolEvent.ComputeDamageRate e) {
        e.setDamageRate(e.getDamageRate() * Math.max(0, 1 - getLevel(e.agent) * Settings.get().toolEfficiencyPerLevel));
    }

    public static void onRobotBreakBlockPre(RobotBreakBlockEvent.Pre e) {
        double boost = Math.max(0, 1 - getLevel(e.agent) * Settings.get().harvestSpeedBoostPerLevel);
        e.setBreakTime(e.getBreakTime() * boost);
    }

    public static void onRobotAttackEntityPost(RobotAttackEntityEvent.Post e) {
        if (e.agent instanceof Robot robot) {
            if (robot.equipmentInventory().getItem(0) != null && !e.target.isAlive()) {
                addExperience(robot, Settings.get().robotActionXp);
            }
        }
    }

    public static void onRobotBreakBlockPost(RobotBreakBlockEvent.Post e) {
        addExperience(e.agent, e.experience * Settings.get().robotOreXpRate + Settings.get().robotActionXp);
    }

    public static void onRobotPlaceBlockPost(RobotPlaceBlockEvent.Post e) {
        addExperience(e.agent, Settings.get().robotActionXp);
    }

    public static void onRobotMovePost(RobotMoveEvent.Post e) {
        addExperience(e.agent, Settings.get().robotExhaustionXpRate * 0.01);
    }

    public static void onRobotExhaustion(RobotExhaustionEvent e) {
        addExperience(e.agent, Settings.get().robotExhaustionXpRate * e.exhaustion);
    }

    public static void onRobotRender(RobotRenderEvent e) {
        int level = 0;
        if (e.agent instanceof Robot robot) {
            for (int index = 0; index < robot.getContainerSize(); index++) {
                if (robot.getComponentInSlot(index) instanceof UpgradeExperience upgrade) {
                    level += upgrade.level;
                }
            }
        }
        if (level > 19) {
            e.multiplyColors(0.4f, 1, 1);
        }
        else if (level > 9) {
            e.multiplyColors(1, 1, 0.4f);
        }
    }

    private static int getLevel(Agent agent) {
        int[] level = {0};
        foreachUpgrade(agent.machine().node(), upgrade -> level[0] += upgrade.level);
        return level[0];
    }

    private static void addExperience(Agent agent, double amount) {
        foreachUpgrade(agent.machine().node(), upgrade -> upgrade.addExperience(amount));
    }

    private static void foreachUpgrade(Node node, Consumer<UpgradeExperience> f) {
        for (Node n : node.reachableNodes()) {
            if (n.host() instanceof UpgradeExperience upgrade) f.accept(upgrade);
        }
    }
}
