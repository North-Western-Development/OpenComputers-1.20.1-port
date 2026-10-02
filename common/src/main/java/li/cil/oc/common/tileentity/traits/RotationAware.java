package li.cil.oc.common.tileentity.traits;

import net.minecraft.core.Direction;

public interface RotationAware extends TileEntityTrait {
    default Direction toLocal(Direction value) {
        return value;
    }

    default Direction toGlobal(Direction value) {
        return value;
    }
}
