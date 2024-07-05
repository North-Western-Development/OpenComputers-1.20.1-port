package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import net.minecraft.block.AbstractBlock.Properties
net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.util.ActionResultType
import net.minecraft.core.Direction
import net.minecraft.util.Hand
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

class NetSplitter(props: Properties) extends RedstoneAware(props) {
  override def newBlockEntity(world: BlockGetter) = new tileentity.NetSplitter(tileentity.BlockEntityTypes.NET_SPLITTER)

  // ----------------------------------------------------------------------- //

  // NOTE: must not be final for immibis microblocks to work.
  override def use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: Hand, trace: BlockHitResult): ActionResultType = {
    if (Wrench.holdsApplicableWrench(player, pos)) {
      val side = trace.getDirection
      val sideToToggle = if (player.isCrouching) side.getOpposite else side
      world.getBlockEntity(pos) match {
        case splitter: tileentity.NetSplitter =>
          if (!world.isClientSide) {
            val oldValue = splitter.openSides(sideToToggle.ordinal())
            splitter.setSideOpen(sideToToggle, !oldValue)
          }
          ActionResultType.sidedSuccess(world.isClientSide)
        case _ => ActionResultType.PASS
      }
    }
    else super.use(state, world, pos, player, hand, trace)
  }
}
