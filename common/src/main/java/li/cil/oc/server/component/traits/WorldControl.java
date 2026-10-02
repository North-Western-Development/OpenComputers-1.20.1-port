package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.tuple.Pair;

import static li.cil.oc.util.ResultWrapper.result;

public interface WorldControl extends WorldAware, SideRestricted {
    @Callback(doc = "function(side:number):boolean, string -- Checks the contents of the block on the specified sides and returns the findings.")
    default Object[] detect(Context context, Arguments args) {
        final Direction side = checkSideForAction(args, 0);
        final Pair<Boolean, String> content = blockContent(side);
        return result(content.getLeft(), content.getRight());
    }
}
