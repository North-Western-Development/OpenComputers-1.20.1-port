package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.world.entity.Entity;

public class RobotAttackEntityEvent extends RobotEvent {
    /**
     * The entity that the robot will attack.
     */
    public final Entity target;

    protected RobotAttackEntityEvent(Agent agent, Entity target) {
        super(agent);
        this.target = target;
    }

    /**
     * Fired when a robot is about to attack an entity.
     * <p/>
     * Canceling this event will prevent the attack.
     */
    public static class Pre extends RobotAttackEntityEvent {
        @Override
        public boolean isCancelable() {
            return true;
        }

        public Pre(Agent agent, Entity target) {
            super(agent, target);
        }
    }

    /**
     * Fired after a robot has attacked an entity.
     */
    public static class Post extends RobotAttackEntityEvent {
        public Post(Agent agent, Entity target) {
            super(agent, target);
        }
    }
}
