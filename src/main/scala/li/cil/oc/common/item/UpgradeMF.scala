package li.cil.oc.common.item

import li.cil.oc.{Localization, Settings}
import li.cil.oc.util.Tooltip
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.{InteractionHand, InteractionResult}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.world.level.Level
import net.minecraftforge.common.extensions.IForgeItem

import java.util

class UpgradeMF(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  override def onItemUseFirst(stack: ItemStack, player: Player, world: Level, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: InteractionHand): InteractionResult = {
    if (!player.level.isClientSide && player.isCrouching) {
      val data = stack.getOrCreateTag
      data.putString(Settings.namespace + "dimension", world.dimension.location.toString)
      data.putIntArray(Settings.namespace + "coord", Array(pos.getX, pos.getY, pos.getZ, side.ordinal()))
      return InteractionResult.sidedSuccess(player.level.isClientSide)
    }
    super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component]) {
    tooltip.add(Component.literal(Localization.Tooltip.MFULinked(stack.getTag match {
      case data: CompoundTag => data.contains(Settings.namespace + "coord")
      case _ => false
    })).setStyle(Tooltip.DefaultStyle))
  }
}
