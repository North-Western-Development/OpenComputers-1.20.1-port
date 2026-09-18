package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level
import net.minecraftforge.common.extensions.IForgeItem

class Chamelium(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def use(stack: ItemStack, world: Level, player: Player): InteractionResultHolder[ItemStack] = {
    if (Settings.get.chameliumEdible) {
      player.startUsingItem(if (player.getItemInHand(InteractionHand.MAIN_HAND) == stack) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND)
    }
    new InteractionResultHolder(InteractionResult.sidedSuccess(world.isClientSide), stack)
  }

  override def getUseAnimation(stack: ItemStack): UseAnim = UseAnim.EAT

  override def getUseDuration(stack: ItemStack): Int = 32

  override def finishUsingItem(stack: ItemStack, world: Level, player: LivingEntity): ItemStack = {
    if (!world.isClientSide) {
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0))
      player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0))
    }
    stack.shrink(1)
    if (stack.getCount > 0) stack
    else ItemStack.EMPTY
  }
}
