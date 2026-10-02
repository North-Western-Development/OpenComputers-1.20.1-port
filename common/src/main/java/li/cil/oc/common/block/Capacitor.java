package li.cil.oc.common.block;

import li.cil.oc.api.network.Connector;
import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Capacitor extends SimpleBlock {
    public Capacitor(Properties props) {
        super(props);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.CAPACITOR.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Capacitor capacitor
            && capacitor.node() instanceof Connector node) {
            return (int) Math.round(15 * node.localBuffer() / node.localBufferSize());
        }
        return 0;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        // In 1.16 random ticks defaulted to tick(); keep updating comparators periodically.
        tick(state, world, pos, rand);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        world.updateNeighborsAt(pos, this);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Capacitor capacitor) {
            capacitor.recomputeCapacity();
        }
    }
}
