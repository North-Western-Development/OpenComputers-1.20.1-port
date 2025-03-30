package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.Settings.DebugCardAccess
import li.cil.oc.common.item.data.DebugCardData
import li.cil.oc.server.component.{DebugCard => CDebugCard}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.Util
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraftforge.common.extensions.IForgeItem

class DebugCard(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component]): Unit = {
    super.tooltipExtended(stack, tooltip)
    val data = new DebugCardData(stack)
    data.access.foreach(access => tooltip.add(Component.literal(s"§8${access.player}§r")))
  }

  override def use(stack: ItemStack, world: Level, player: Player): ActionResult[ItemStack] = {
    if (!world.isClientSide && player.isCrouching) {
      val data = new DebugCardData(stack)
      val name = player.getName

      if (data.access.exists(_.player == name)) data.access = None
      else data.access =
        Some(CDebugCard.AccessContext(name.getString, Settings.get.debugCardAccess match {
          case wl: DebugCardAccess.Whitelist => wl.nonce(name.getString) match {
            case Some(n) => n
            case None =>
              player.sendMessage(Component.literal("§cYou are not whitelisted to use debug card"), Util.NIL_UUID)
              player.swing(Hand.MAIN_HAND)
              return new ActionResult[ItemStack](InteractionResult.FAIL, stack)
          }

          case _ => ""
        }))

      data.saveData(stack)
      player.swing(Hand.MAIN_HAND)
    }
    new ActionResult(InteractionResult.sidedSuccess(world.isClientSide), stack)
  }
}
