package li.cil.oc.api.prefab;

import li.cil.oc.api.driver.DriverBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * To limit sidedness, I recommend overriding {@link #worksWith(Level, BlockPos, Direction)}
 * and calling <code>super.worksWith</code> in addition to the side check.
 */
public abstract class DriverSidedTileEntity implements DriverBlock {
    public abstract Class<?> getTileEntityClass();

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final Class<?> filter = getTileEntityClass();
        if (filter == null) {
            // This can happen if filter classes are deduced by reflection and
            // the class in question is not present.
            return false;
        }
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity != null && filter.isAssignableFrom(tileEntity.getClass());
    }
}
