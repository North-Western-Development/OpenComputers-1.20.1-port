package li.cil.oc.api.network;

import net.minecraft.core.Direction;

public interface SidedComponent {
    /**
     * Whether this component can connect to a node on the specified side.
     * <br>
     * The provided side is relative to the component, i.e. when the tile
     * entity sits at (0, 0, 0) and is asked for its southern node (positive
     * Z axis) it has to return the connectivity for the face between it and
     * the block at (0, 0, 1).
     *
     * @param side the side to check for.
     * @return whether the component may be connected to from the specified side.
     */
    boolean canConnectNode(Direction side);
}
