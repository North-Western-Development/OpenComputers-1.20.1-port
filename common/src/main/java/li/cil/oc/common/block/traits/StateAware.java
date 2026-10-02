package li.cil.oc.common.block.traits;

import li.cil.oc.api.util.StateAware.State;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Marker for blocks that emit a comparator signal based on the tile entity's
 * {@link li.cil.oc.api.util.StateAware} state. Implemented in SimpleBlock via
 * {@code instanceof StateAware}.
 */
public interface StateAware {
    static int analogOutputSignal(Level world, BlockPos pos) {
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof li.cil.oc.api.util.StateAware stateful) {
            if (stateful.getCurrentState().contains(State.IsWorking)) return 15;
            else if (stateful.getCurrentState().contains(State.CanWork)) return 10;
        }
        return 0;
    }
}
