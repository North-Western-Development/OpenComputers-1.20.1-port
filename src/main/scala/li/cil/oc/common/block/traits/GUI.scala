package li.cil.oc.common.block.traits

import li.cil.oc.OpenComputers
import li.cil.oc.common.block.SimpleBlock
net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.util.Hand
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

trait GUI extends SimpleBlock {
  def openGui(player: ServerPlayer, world: Level, pos: BlockPos)

  // This gets forwarded to the vanilla Player.openMenu call which doesn't support extra data.
  override def getMenuProvider(state: BlockState, world: Level, pos: BlockPos): INamedContainerProvider = null

  override def localOnBlockActivated(world: Level, pos: BlockPos, player: Player, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.isCrouching) {
      player match {
        case srvPlr: ServerPlayer if !world.isClientSide => openGui(srvPlr, world, pos)
        case _ =>
      }
      true
    }
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }
}
