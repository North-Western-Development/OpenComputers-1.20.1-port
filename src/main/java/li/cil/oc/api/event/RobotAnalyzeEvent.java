package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.world.entity.player.Player;

/**
 * Fired when an analyzer is used on a robot.
 * <br>
 * Use this to echo additional information for custom components.
 */
public class RobotAnalyzeEvent extends RobotEvent {
    /**
     * The player that used the analyzer.
     */
    public final Player player;

    public RobotAnalyzeEvent(Agent agent, Player player) {
        super(agent);
        this.player = player;
    }
}
