package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.EventHandler;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public interface Environment extends TileEntityTrait, li.cil.oc.api.network.Environment, EnvironmentHost {
    final class State {
        public volatile boolean isChangeScheduled = false;
    }

    String NodeTag = Settings.namespace + "node";

    /** Provided by {@link TileEntity}. */
    State environmentState();

    @Override
    default Level world() {
        return ocLevel();
    }

    @Override
    default double xPosition() {
        return x() + 0.5;
    }

    @Override
    default double yPosition() {
        return y() + 0.5;
    }

    @Override
    default double zPosition() {
        return z() + 0.5;
    }

    @Override
    default void markChanged() {
        if (this instanceof Tickable) environmentState().isChangeScheduled = true;
        else ocLevel().blockEntityChanged(ocBlockPos());
    }

    default boolean isConnected() {
        final Node node = node();
        return node != null && node.address() != null && node.network() != null;
    }

    // ----------------------------------------------------------------------- //

    static void onInitialize(Environment self) {
        if (self.isServer()) {
            EventHandler.scheduleServer((net.minecraft.world.level.block.entity.BlockEntity) self);
        }
    }

    static void onUpdateEntity(Environment self) {
        final State state = self.environmentState();
        if (state.isChangeScheduled) {
            self.ocLevel().blockEntityChanged(self.ocBlockPos());
            state.isChangeScheduled = false;
        }
    }

    static void onDispose(Environment self) {
        if (self.isServer()) {
            final Node node = self.node();
            if (node != null) node.remove();
            if (self instanceof SidedEnvironment sidedEnvironment) {
                for (Direction side : Direction.values()) {
                    final Node sidedNode = sidedEnvironment.sidedNode(side);
                    if (sidedNode != null) sidedNode.remove();
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(Environment self, CompoundTag nbt) {
        final Node node = self.node();
        if (node != null && node.host() == self) {
            node.loadData(nbt.getCompound(NodeTag));
        }
    }

    static void onSaveForServer(Environment self, CompoundTag nbt) {
        final Node node = self.node();
        if (node != null && node.host() == self) {
            ExtendedNBT.setNewCompoundTag(nbt, NodeTag, node::saveData);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    default void onMessage(Message message) {
    }

    @Override
    default void onConnect(Node node) {
    }

    @Override
    default void onDisconnect(Node node) {
        if (node == this.node() && node instanceof Connector connector) {
            // Set it to zero to push all energy into other nodes, to
            // avoid energy loss when removing nodes. Set it back to the
            // original value though, as there are cases where the node
            // is re-used afterwards, without re-adjusting its buffer size.
            final double bufferSize = connector.localBufferSize();
            connector.setLocalBufferSize(0);
            connector.setLocalBufferSize(bufferSize);
        }
    }

    // ----------------------------------------------------------------------- //

    default Object[] result(Object... args) {
        return ResultWrapper.result(args);
    }
}
