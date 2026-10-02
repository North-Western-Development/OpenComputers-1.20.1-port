package li.cil.oc.util;

import net.minecraft.core.Direction;

/**
 * Former implicit extension class for {@link Direction}; call as
 * {@code ExtendedEnumFacing.getRotation(facing, axis)}.
 */
public final class ExtendedEnumFacing {
    private ExtendedEnumFacing() {
    }

    // Copy-pasta from old Forge's ForgeDirection, because MC's equivalent in Direction is client side only \o/
    private static final int[][] ROTATION_MATRIX = {
            {0, 1, 4, 5, 3, 2, 6},
            {0, 1, 5, 4, 2, 3, 6},
            {5, 4, 2, 3, 0, 1, 6},
            {4, 5, 2, 3, 1, 0, 6},
            {2, 3, 1, 0, 4, 5, 6},
            {3, 2, 0, 1, 4, 5, 6},
            {0, 1, 2, 3, 4, 5, 6}};

    public static Direction getRotation(Direction facing, Direction axis) {
        return Direction.from3DDataValue(ROTATION_MATRIX[axis.ordinal()][facing.ordinal()]);
    }
}
