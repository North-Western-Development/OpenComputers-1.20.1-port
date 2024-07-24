package li.cil.oc.common.block.traits

import li.cil.oc.api
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

trait StateAware extends Block {
  override def hasAnalogOutputSignal(state: BlockState): Boolean = true

  override def getAnalogOutputSignal(state: BlockState, world: Level, pos: BlockPos): Int =
    world.getBlockEntity(pos) match {
      case stateful: api.util.StateAware =>
        if (stateful.getCurrentState.contains(api.util.StateAware.State.IsWorking)) 15
        else if (stateful.getCurrentState.contains(api.util.StateAware.State.CanWork)) 10
        else 0
      case _ => 0
    }
}
