package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Port note: extends {@link Environment} (in Scala it only declared {@code isConnected}
 * abstractly; all implementers are environments) so that {@code isConnected()} has a single
 * default that {@link Hub} / tiles can override without default-method conflicts.
 */
public interface PowerBalancer extends PowerInformation, SidedEnvironment, Tickable, Environment {
    final class State {
        public volatile double globalBuffer = 0.0;
        public volatile double globalBufferSize = 0.0;
    }

    /** Provided by {@link TileEntity}. */
    State powerBalancerState();

    @Override
    default double globalBuffer() {
        return powerBalancerState().globalBuffer;
    }

    @Override
    default void setGlobalBuffer(double value) {
        powerBalancerState().globalBuffer = value;
    }

    @Override
    default double globalBufferSize() {
        return powerBalancerState().globalBufferSize;
    }

    @Override
    default void setGlobalBufferSize(double value) {
        powerBalancerState().globalBufferSize = value;
    }

    static void onUpdateEntity(PowerBalancer self) {
        if (self.isServer() && self.isConnected() && self.ocLevel().getGameTime() % Settings.get().tickFrequency == 0) {
            final Connector[] nodes = self.connectors();
            // Yeeeeah, so that just happened... it's not a beauty, but it works. This
            // is necessary because power in networks can be updated asynchronously,
            // i.e. in separate threads (e.g. to allow screens to consume energy when
            // they change, which usually happens in a computers executor thread).
            // This multi-lock only happens in the main server thread, though, so we
            // don't have to fear deadlocks. I think.
            synchronized (network(self, nodes[0])) {
                synchronized (network(self, nodes[1])) {
                    synchronized (network(self, nodes[2])) {
                        synchronized (network(self, nodes[3])) {
                            synchronized (network(self, nodes[4])) {
                                synchronized (network(self, nodes[5])) {
                                    final Pair<Double, Double> sums = self.distribute();
                                    final double sumBuffer = sums.getLeft();
                                    final double sumSize = sums.getRight();
                                    if (sumSize > 0) {
                                        final double ratio = sumBuffer / sumSize;
                                        for (Connector node : self.connectors()) {
                                            if (self.isPrimary(node)) {
                                                node.changeBuffer(node.globalBufferSize() * ratio - node.globalBuffer());
                                            }
                                        }
                                    }
                                    self.setGlobalBuffer(sumBuffer);
                                    self.setGlobalBufferSize(sumSize);
                                }
                            }
                        }
                    }
                }
            }
            self.updatePowerInformation();
        }
    }

    private static Object network(PowerBalancer self, Connector connector) {
        return connector != null && connector.network() != null ? connector.network() : self;
    }

    default Pair<Double, Double> distribute() {
        double sumBuffer = 0.0;
        double sumSize = 0.0;
        for (Connector node : connectors()) {
            if (isPrimary(node)) {
                sumBuffer += node.globalBuffer();
                sumSize += node.globalBufferSize();
            }
        }
        return Pair.of(sumBuffer, sumSize);
    }

    private Connector[] connectors() {
        final Direction[] sides = Direction.values();
        final Connector[] result = new Connector[sides.length];
        for (Direction side : sides) {
            final Node node = sidedNode(side);
            result[side.ordinal()] = node instanceof Connector connector ? connector : null;
        }
        return result;
    }

    private boolean isPrimary(Connector connector) {
        if (connector == null) return false;
        final Connector[] nodes = connectors();
        for (Connector node : nodes) {
            if (node != null && node.network() == connector.network()) {
                return node == connector;
            }
        }
        // Scala: nodes(indexWhere(...)) with index -1 would throw; cannot happen for a non-null
        // connector of this tile.
        return false;
    }
}
