package li.cil.oc.api.event;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events for handling network activity and representing it on the client.
 * <br>
 * This is used to render network activity
 * indicators on some containers (e.g. computer, server).
 * <br>
 * Use this to implement rendering of disk access indicators on you own
 * containers / computers / drive bays.
 * <br>
 * Canceling this event is provided to allow registering higher priority
 * event handlers that override default behavior.
 */
public class NetworkActivityEvent extends Event {
    protected Level world;

    protected double x;

    protected double y;

    protected double z;

    protected BlockEntity tileEntity;

    protected CompoundTag data;

    /**
     * Constructor for tile entity hosted network cards.
     *
     * @param tileEntity the tile entity hosting the network card.
     * @param data       the additional data.
     */
    protected NetworkActivityEvent(BlockEntity tileEntity, CompoundTag data) {
        this.world = tileEntity.getLevel();
        this.x = tileEntity.getBlockPos().getX() + 0.5;
        this.y = tileEntity.getBlockPos().getY() + 0.5;
        this.z = tileEntity.getBlockPos().getZ() + 0.5;
        this.tileEntity = tileEntity;
        this.data = data;
    }

    /**
     * Constructor for arbitrarily hosted network cards.
     *
     * @param world the world the network card lives in.
     * @param x     the x coordinate of the network card's container.
     * @param y     the y coordinate of the network card's container.
     * @param z     the z coordinate of the network card's container.
     * @param data  the additional data.
     */
    protected NetworkActivityEvent(Level world, double x, double y, double z, CompoundTag data) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tileEntity = null;
        this.data = data;
    }

    /**
     * The world the network card lives in.
     */
    public Level getWorld() {
        return world;
    }

    /**
     * The x coordinate of the network card's container.
     */
    public double getX() {
        return x;
    }

    /**
     * The y coordinate of the network card's container.
     */
    public double getY() {
        return y;
    }

    /**
     * The z coordinate of the network card's container.
     */
    public double getZ() {
        return z;
    }

    /**
     * The tile entity hosting the network card.
     * <br>
     * <em>Important</em>: this can be <tt>null</tt>, which is usually the
     * case when the container is an entity or item.
     */
    public BlockEntity getBlockEntity() {
        return tileEntity;
    }

    /**
     * Addition custom data, this is used to transmit the number of the server
     * in a server rack the network card lives in, for example.
     */
    public CompoundTag getData() {
        return data;
    }

    public static final class Server extends NetworkActivityEvent {
        private Node node;

        public Server(BlockEntity tileEntity, Node node) {
            super(tileEntity, new CompoundTag());
            this.node = node;
        }

        public Server(Level world, double x, double y, double z, Node node) {
            super(world, x, y, z, new CompoundTag());
            this.node = node;
        }

        /**
         * The node of the network card that signalled activity.
         */
        public Node getNode() {
            return node;
        }
    }

    public static final class Client extends NetworkActivityEvent {
        /**
         * Constructor for tile entity hosted network card.
         *
         * @param tileEntity the tile entity hosting the network card.
         * @param data       the additional data.
         */
        public Client(BlockEntity tileEntity, CompoundTag data) {
            super(tileEntity, data);
        }

        /**
         * Constructor for arbitrarily hosted network card.
         *
         * @param world the world the file system lives in.
         * @param x     the x coordinate of the network card's container.
         * @param y     the y coordinate of the network card's container.
         * @param z     the z coordinate of the network card's container.
         * @param data  the additional data.
         */
        public Client(Level world, double x, double y, double z, CompoundTag data) {
            super(world, x, y, z, data);
        }
    }
}
