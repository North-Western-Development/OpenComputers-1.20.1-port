package li.cil.oc.common.block;

import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.integration.util.Wrench;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Adapter extends SimpleBlock implements GUI {
    public Adapter(Properties props) {
        super(props);
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Adapter te) {
            ContainerTypes.openAdapterGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.ADAPTER.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Adapter adapter) {
            adapter.neighborChanged();
        }
    }

    /**
     * Same signature as Forge's {@code IForgeBlock.onNeighborChange}, so it overrides that on Forge
     * (called e.g. when a neighboring block entity changes).
     */
    // TODO(port): Fabric has no equivalent of onNeighborChange; only neighborChanged() triggers there.
    public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Adapter adapter) {
            Direction side = null;
            for (Direction d : Direction.values()) {
                if (pos.relative(d).equals(neighbor)) {
                    side = d;
                    break;
                }
            }
            if (side == null) throw new IllegalArgumentException("not a neighbor");
            adapter.neighborChanged(side);
        }
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (Wrench.holdsApplicableWrench(player, pos)) {
            Direction sideToToggle = player.isCrouching() ? side.getOpposite() : side;
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Adapter adapter) {
                if (!world.isClientSide) {
                    boolean oldValue = adapter.openSides()[sideToToggle.ordinal()];
                    adapter.setSideOpen(sideToToggle, !oldValue);
                }
                return true;
            }
            return false;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }
}
