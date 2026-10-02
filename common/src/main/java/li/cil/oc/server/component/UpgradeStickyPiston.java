package li.cil.oc.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.Direction;

/**
 * Was declared in UpgradePiston.scala.
 */
public abstract class UpgradeStickyPiston extends UpgradePiston {
    public UpgradeStickyPiston(EnvironmentHost host) {
        super(host, true);
    }

    @Callback(doc = "function([side:number]):boolean -- Tries to reach out to the side given (default front) and pull a block similar to a vanilla sticky piston.")
    public Object[] pull(Context context, Arguments args) {
        final Direction side = pushDirection(args, 0);
        return doPistonAction(context, side, false);
    }

    // ----------------------------------------------------------------------- //

    public static class Drone extends UpgradeStickyPiston implements PistonTraits.DroneLike {
        public Drone(li.cil.oc.api.internal.Drone drone) {
            super(drone);
        }
    }

    public static class Rotatable extends UpgradeStickyPiston implements PistonTraits.RotatableLike {
        public final li.cil.oc.api.internal.Rotatable rotatable;

        public <T extends li.cil.oc.api.internal.Rotatable & EnvironmentHost> Rotatable(T rotatable) {
            super(rotatable);
            this.rotatable = rotatable;
        }

        @Override
        public li.cil.oc.api.internal.Rotatable rotatable() {
            return rotatable;
        }
    }

    public static class Tablet extends Rotatable implements PistonTraits.TabletLike {
        public final li.cil.oc.api.internal.Tablet tablet;

        public Tablet(li.cil.oc.api.internal.Tablet tablet) {
            super(tablet);
            this.tablet = tablet;
        }

        @Override
        public li.cil.oc.api.internal.Tablet tablet() {
            return tablet;
        }
    }
}
