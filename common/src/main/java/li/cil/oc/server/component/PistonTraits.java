package li.cil.oc.server.component;

import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import net.minecraft.core.Direction;

/**
 * Was a {@code protected object} in UpgradePiston.scala.
 */
final class PistonTraits {
    private PistonTraits() {
    }

    interface ExtendAware {
        EnvironmentHost host();

        default BlockPosition pushOrigin(Direction side) {
            return BlockPosition.apply(host());
        }

        Direction pushDirection(Arguments args, int index);
    }

    interface DroneLike extends ExtendAware {
        @Override
        default Direction pushDirection(Arguments args, int index) {
            return ExtendedArguments.optSideAny(args, index, Direction.SOUTH);
        }
    }

    interface RotatableLike extends ExtendAware {
        Rotatable rotatable();

        @Override
        default Direction pushDirection(Arguments args, int index) {
            return rotatable().toGlobal(ExtendedArguments.optSideForAction(args, index, Direction.SOUTH));
        }
    }

    interface TabletLike extends ExtendAware {
        Tablet tablet();

        @Override
        default BlockPosition pushOrigin(Direction side) {
            if (side == Direction.DOWN && tablet().player().getEyeHeight() > 1) return ExtendAware.super.pushOrigin(side).offset(Direction.DOWN);
            else return ExtendAware.super.pushOrigin(side);
        }
    }
}
