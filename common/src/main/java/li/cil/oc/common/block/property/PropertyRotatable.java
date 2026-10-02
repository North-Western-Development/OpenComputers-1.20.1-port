package li.cil.oc.common.block.property;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public final class PropertyRotatable {
    public static final DirectionProperty Facing = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final DirectionProperty Pitch = DirectionProperty.create("pitch", d -> d.getAxis() == Direction.Axis.Y || d == Direction.NORTH);
    public static final DirectionProperty Yaw = DirectionProperty.create("yaw", Direction.Plane.HORIZONTAL);

    private PropertyRotatable() {
    }
}
