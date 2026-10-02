package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.Direction;

public interface SideRestricted {
    // Note: implementations that want to delegate to the static helper must call
    // li.cil.oc.util.ExtendedArguments.checkSideForAction(args, n) fully qualified,
    // since this member shadows a static import of the same name.
    Direction checkSideForAction(Arguments args, int n);
}
