package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

abstract class RedstoneAware(props: Properties) extends SimpleBlock(props) {
  override def isSignalSource(state: BlockState): Boolean = true

  override def canConnectRedstone(state: BlockState, world: BlockGetter, pos: BlockPos, side: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def getDirectSignal(state: BlockState, world: BlockGetter, pos: BlockPos, side: Direction) =
    getSignal(state, world, pos, side)

  @Deprecated
  override def getSignal(state: BlockState, world: BlockGetter, pos: BlockPos, side: Direction) =
    world.getBlockEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware if side != null => redstone.getOutput(side.getOpposite) max 0
      case _ => super.getSignal(state, world, pos, side)
    }

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def neighborChanged(state: BlockState, world: Level, pos: BlockPos, block: Block, fromPos: BlockPos, b: Boolean): Unit = {
    world.getBlockEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.checkRedstoneInputChanged()
      case _ => // Ignore.
    }
  }
}
