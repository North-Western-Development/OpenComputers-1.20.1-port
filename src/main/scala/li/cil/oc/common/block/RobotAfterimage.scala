package li.cil.oc.common.block

import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.world.level.block.Blocks
net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.fluid.FluidState
import net.minecraft.world.item.ItemStack
import net.minecraft.util.ActionResultType
import net.minecraft.core.Direction
import net.minecraft.util.Hand
import net.minecraft.core.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.server.ServerLevel

class RobotAfterimage(props: Properties) extends SimpleBlock(props) {
  override def getPickBlock(state: BlockState, target: RayTraceResult, world: BlockGetter, pos: BlockPos, player: Player): ItemStack =
    findMovingRobot(world, pos) match {
      case Some(robot) => robot.info.createItemStack()
      case _ => ItemStack.EMPTY
    }

  override def getShape(state: BlockState, world: BlockGetter, pos: BlockPos, ctx: ISelectionContext): VoxelShape = {
    findMovingRobot(world, pos) match {
      case Some(robot) =>
        val block = robot.getBlockState.getBlock.asInstanceOf[SimpleBlock]
        val shape = block.getShape(state, world, robot.getBlockPos, ctx)
        val delta = robot.moveFrom.fold(BlockPos.ZERO)(vec => {
          val blockPos = robot.getBlockPos
          new BlockPos(blockPos.getX - vec.getX, blockPos.getY - vec.getY, blockPos.getZ - vec.getZ)
        })
        shape.move(delta.getX, delta.getY, delta.getZ)
      case _ => super.getShape(state, world, pos, ctx)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onPlace(state: BlockState, world: Level, pos: BlockPos, prevState: BlockState, moved: Boolean): Unit = {
    if (!world.isClientSide) {
      world.asInstanceOf[ServerLevel].getBlockTicks.scheduleTick(pos, this, Math.max((Settings.get.moveDelay * 20).toInt, 1) - 1)
    }
  }

  override def tick(state: BlockState, world: ServerLevel, pos: BlockPos, rand: Random) {
    world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState)
  }

  override def removedByPlayer(state: BlockState, world: Level, pos: BlockPos, player: Player, willHarvest: Boolean, fluid: FluidState): Boolean = {
    findMovingRobot(world, pos) match {
      case Some(robot) if robot.isAnimatingMove && robot.moveFrom.contains(pos) =>
        robot.proxy.getBlockState.getBlock.removedByPlayer(state, world, pos, player, false, fluid)
      case _ => super.removedByPlayer(state, world, pos, player, willHarvest, fluid) // Probably broken by the robot we represent.
    }
  }

  @Deprecated
  override def use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: Hand, trace: BlockRayTraceResult): ActionResultType = {
    findMovingRobot(world, pos) match {
      case Some(robot) => api.Items.get(Constants.BlockName.Robot).block.use(world.getBlockState(robot.getBlockPos), world, robot.getBlockPos, player, hand, trace)
      case _ => if (world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState)) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
    }
  }

  def findMovingRobot(world: BlockGetter, pos: BlockPos): Option[tileentity.Robot] = {
    for (side <- Direction.values) {
      val tpos = pos.relative(side)
      if (world match {
        case world: Level => world.isLoaded(tpos)
        case _ => true
      }) world.getBlockEntity(tpos) match {
        case proxy: tileentity.RobotProxy if proxy.robot.moveFrom.contains(pos) => return Some(proxy.robot)
        case _ =>
      }
    }
    None
  }
}
