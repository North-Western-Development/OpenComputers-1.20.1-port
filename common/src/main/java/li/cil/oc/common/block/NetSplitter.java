package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.integration.util.Wrench;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NetSplitter extends RedstoneAware {
    public NetSplitter(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.NET_SPLITTER.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (Wrench.holdsApplicableWrench(player, pos)) {
            Direction side = trace.getDirection();
            Direction sideToToggle = player.isCrouching() ? side.getOpposite() : side;
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.NetSplitter splitter) {
                if (!world.isClientSide) {
                    boolean oldValue = splitter.openSides()[sideToToggle.ordinal()];
                    splitter.setSideOpen(sideToToggle, !oldValue);
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
            return InteractionResult.PASS;
        }
        return super.use(state, world, pos, player, hand, trace);
    }
}
