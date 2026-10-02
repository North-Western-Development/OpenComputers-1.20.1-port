package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CarpetedCapacitor extends Capacitor {
    public CarpetedCapacitor(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.CARPETED_CAPACITOR.get().create(pos, state);
    }
}
