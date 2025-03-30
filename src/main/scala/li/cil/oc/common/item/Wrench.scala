package li.cil.oc.common.item

import li.cil.oc.api
import li.cil.oc.common.block.SimpleBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.util.Rotation
import net.minecraft.core.BlockPos
import net.minecraft.world.IWorldReader
import net.minecraft.world.level.Level
import net.minecraftforge.common.extensions.IForgeItem

class Wrench(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with api.internal.Wrench {
  override def doesSneakBypassUse(stack: ItemStack, world: IWorldReader, pos: BlockPos, player: Player): Boolean = true

  override def onItemUseFirst(stack: ItemStack, player: Player, world: Level, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: Hand): InteractionResult = {
    if (world.isLoaded(pos) && world.mayInteract(player, pos)) {
      val state = world.getBlockState(pos)
      state.getBlock match {
        case block: SimpleBlock if block.rotateBlock(world, pos, side) =>
          state.neighborChanged(world, pos, Blocks.AIR, pos, false)
          player.swing(hand)
          if (!world.isClientSide) InteractionResult.sidedSuccess(world.isClientSide) else InteractionResult.PASS
        case _ =>
          val updated = state.rotate(world, pos, Rotation.CLOCKWISE_90)
          if (updated != state) {
            world.setBlock(pos, updated, 3)
            player.swing(hand)
            if (!world.isClientSide) InteractionResult.sidedSuccess(world.isClientSide) else InteractionResult.PASS
          }
          else super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand)
      }
    }
    else super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  def useWrenchOnBlock(player: Player, world: Level, pos: BlockPos, simulate: Boolean): Boolean = {
    if (!simulate) player.swing(Hand.MAIN_HAND)
    true
  }
}
