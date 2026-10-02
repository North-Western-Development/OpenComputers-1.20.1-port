package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class RobotAfterimage extends SimpleBlock {
    public RobotAfterimage(Properties props) {
        super(props);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return findMovingRobot(world, pos).map(robot -> robot.info.createItemStack()).orElse(ItemStack.EMPTY);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        Optional<li.cil.oc.common.tileentity.Robot> found = findMovingRobot(world, pos);
        if (found.isPresent()) {
            li.cil.oc.common.tileentity.Robot robot = found.get();
            SimpleBlock block = (SimpleBlock) robot.getBlockState().getBlock();
            VoxelShape shape = block.getShape(state, world, robot.getBlockPos(), ctx);
            BlockPos blockPos = robot.getBlockPos();
            BlockPos delta = robot.moveFrom.map(vec -> new BlockPos(blockPos.getX() - vec.getX(), blockPos.getY() - vec.getY(), blockPos.getZ() - vec.getZ())).orElse(BlockPos.ZERO);
            return shape.move(delta.getX(), delta.getY(), delta.getZ());
        }
        return super.getShape(state, world, pos, ctx);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState prevState, boolean moved) {
        if (!world.isClientSide) {
            world.scheduleTick(pos, this, Math.max((int) (Settings.get().moveDelay * 20), 1) - 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public boolean removedByPlayer(BlockState state, Level world, BlockPos pos, Player player) {
        Optional<li.cil.oc.common.tileentity.Robot> found = findMovingRobot(world, pos);
        if (found.isPresent() && found.get().isAnimatingMove() && found.get().moveFrom.isPresent() && found.get().moveFrom.get().equals(pos)) {
            if (found.get().proxy.getBlockState().getBlock() instanceof SimpleBlock block) {
                return block.removedByPlayer(state, world, pos, player);
            }
        }
        return super.removedByPlayer(state, world, pos, player); // Probably broken by the robot we represent.
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        Optional<li.cil.oc.common.tileentity.Robot> found = findMovingRobot(world, pos);
        if (found.isPresent()) {
            BlockPos robotPos = found.get().getBlockPos();
            return Items.get(Constants.BlockName.Robot).block().use(world.getBlockState(robotPos), world, robotPos, player, hand, trace);
        }
        return world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
    }

    public Optional<li.cil.oc.common.tileentity.Robot> findMovingRobot(BlockGetter world, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos tpos = pos.relative(side);
            boolean loaded = !(world instanceof Level level) || level.isLoaded(tpos);
            if (loaded && world.getBlockEntity(tpos) instanceof li.cil.oc.common.tileentity.RobotProxy proxy
                && proxy.robot.moveFrom.isPresent() && proxy.robot.moveFrom.get().equals(pos)) {
                return Optional.of(proxy.robot);
            }
        }
        return Optional.empty();
    }
}
