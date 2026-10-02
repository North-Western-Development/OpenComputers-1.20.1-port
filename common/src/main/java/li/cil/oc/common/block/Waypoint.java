package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Arrays;

public class Waypoint extends RedstoneAware {
    public Waypoint(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.WAYPOINT.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!player.isCrouching()) {
            if (world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Waypoint t) {
                li.cil.oc.client.ClientHooks.showWaypointGui(t);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return super.use(state, world, pos, player, hand, trace);
    }

    @Override
    public Direction[] getValidRotations(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Waypoint waypoint) {
            Direction facing = waypoint.facing();
            return Arrays.stream(Direction.values()).filter(d -> d != facing && d != facing.getOpposite()).toArray(Direction[]::new);
        }
        return super.getValidRotations(world, pos);
    }
}
