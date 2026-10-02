package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PowerConverter extends SimpleBlock implements PowerAcceptor {
    public PowerConverter(Properties props) {
        super(props);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().powerConverterRate;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.POWER_CONVERTER.get().create(pos, state);
    }
}
