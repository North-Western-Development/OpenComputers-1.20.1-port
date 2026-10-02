package li.cil.oc.common.capabilities;

import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedComponent;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Replacement for OC's own Forge capabilities ({@code CapabilityEnvironment},
 * {@code CapabilitySidedEnvironment}, {@code CapabilitySidedComponent}, {@code CapabilityColored}).
 * Those only wrapped block entities implementing the API interfaces, so plain {@code instanceof}
 * checks are equivalent; these helpers keep the special cases (sided components) in one place.
 */
public final class Capabilities {
    private Capabilities() {
    }

    /** Former {@code EnvironmentCapability}. */
    @Nullable
    public static Environment getEnvironment(@Nullable BlockEntity tileEntity) {
        return tileEntity instanceof Environment environment ? environment : null;
    }

    /**
     * Former {@code SidedEnvironmentCapability}: either the block entity itself, or, for an
     * {@link Environment} that is a {@link SidedComponent}, a view exposing its node only on
     * the sides it may connect to.
     */
    @Nullable
    public static SidedEnvironment getSidedEnvironment(@Nullable BlockEntity tileEntity) {
        if (tileEntity instanceof Environment environment && tileEntity instanceof SidedComponent component) {
            return new SidedComponentView(environment, component);
        }
        if (tileEntity instanceof SidedEnvironment sidedEnvironment) {
            return sidedEnvironment;
        }
        return null;
    }

    /** Former {@code ColoredCapability}. */
    @Nullable
    public static Colored getColored(@Nullable BlockEntity tileEntity) {
        return tileEntity instanceof Colored colored ? colored : null;
    }

    /** The node the block entity exposes on the given side, as {@code Network.getNetworkNode} used it. */
    @Nullable
    public static Node getNetworkNode(@Nullable BlockEntity tileEntity, Direction side) {
        final SidedEnvironment sided = getSidedEnvironment(tileEntity);
        if (sided != null) return sided.sidedNode(side);
        final Environment environment = getEnvironment(tileEntity);
        if (environment != null) return environment.node();
        return null;
    }

    private record SidedComponentView(Environment environment, SidedComponent component) implements SidedEnvironment {
        @Override
        public Node sidedNode(Direction side) {
            return component.canConnectNode(side) ? environment.node() : null;
        }

        @Override
        public boolean canConnect(Direction side) {
            return component.canConnectNode(side);
        }
    }
}
