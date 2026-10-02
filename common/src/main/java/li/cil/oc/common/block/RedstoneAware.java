package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public abstract class RedstoneAware extends SimpleBlock {
    protected RedstoneAware(Properties props) {
        super(props);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * Same signature as Forge's {@code IForgeBlock.canConnectRedstone}, so it overrides that on Forge.
     * On Fabric vanilla redstone dust connects to every signal source.
     */
    // TODO(port): Fabric has no per-side redstone connection hook.
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        return world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.traits.RedstoneAware redstone && redstone.isOutputEnabled();
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return getSignal(state, world, pos, side);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof li.cil.oc.common.tileentity.traits.RedstoneAware redstone && side != null) {
            return Math.max(redstone.getOutput(side.getOpposite()), 0);
        }
        return super.getSignal(state, world, pos, side);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.traits.RedstoneAware redstone) {
            redstone.checkRedstoneInputChanged();
        }
    }
}
