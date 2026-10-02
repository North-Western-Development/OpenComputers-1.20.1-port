package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.core.Direction;

public abstract class RobotMoveEvent extends RobotEvent {
    /**
     * The direction in which the robot will be moving.
     */
    public final Direction direction;

    protected RobotMoveEvent(Agent agent, Direction direction) {
        super(agent);
        this.direction = direction;
    }

    /**
     * Fired when a robot is about to move.
     * <p/>
     * Canceling the event will prevent the robot from moving.
     */
    public static class Pre extends RobotMoveEvent {
        @Override
        public boolean isCancelable() {
            return true;
        }

        public Pre(Agent agent, Direction direction) {
            super(agent, direction);
        }
    }

    /**
     * Fired after a robot moved.
     */
    public static class Post extends RobotMoveEvent {
        public Post(Agent agent, Direction direction) {
            super(agent, direction);
        }
    }
}
