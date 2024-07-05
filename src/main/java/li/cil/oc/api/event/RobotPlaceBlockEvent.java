package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Cancelable;

public abstract class RobotPlaceBlockEvent extends RobotEvent {
    /**
     * The item that is used to place the block.
     */
    public final ItemStack stack;

    /**
     * The world in which the block will be placed.
     */
    public final Level world;

    /**
     * The coordinates at which the block will be placed.
     */
    public final BlockPos pos;

    protected RobotPlaceBlockEvent(Agent agent, ItemStack stack, Level world, BlockPos pos) {
        super(agent);
        this.stack = stack;
        this.world = world;
        this.pos = pos;
    }

    /**
     * Fired when a robot is about to place a block.
     * <br>
     * Canceling this event will prevent the block from being placed.
     */
    @Cancelable
    public static class Pre extends RobotPlaceBlockEvent {
        public Pre(Agent agent, ItemStack stack, Level world, BlockPos pos) {
            super(agent, stack, world, pos);
        }
    }

    /**
     * Fired after a robot placed a block.
     */
    public static class Post extends RobotPlaceBlockEvent {
        public Post(Agent agent, ItemStack stack, Level world, BlockPos pos) {
            super(agent, stack, world, pos);
        }
    }
}
