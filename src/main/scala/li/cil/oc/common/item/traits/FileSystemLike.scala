package li.cil.oc.common.item.traits

import li.cil.oc.{Localization, Settings}
import li.cil.oc.client.gui
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.util.Tooltip
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.Level
import net.minecraft.world.{InteractionHand, InteractionResult, InteractionResultHolder}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}

import java.util

trait FileSystemLike extends SimpleItem {
  override protected def tooltipName = None

  def kiloBytes: Int

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: Level, tooltip: util.List[Component], flag: TooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    if (stack.hasTag) {
      val nbt = stack.getTag
      if (nbt.contains(Settings.namespace + "data")) {
        val data = nbt.getCompound(Settings.namespace + "data")
        if (data.contains(Settings.namespace + "fs.label")) {
          tooltip.add(Component.literal(data.getString(Settings.namespace + "fs.label")).setStyle(Tooltip.DefaultStyle))
        }
        if (flag.isAdvanced && data.contains("fs")) {
          val fsNbt = data.getCompound("fs")
          if (fsNbt.contains("capacity.used")) {
            val used = fsNbt.getLong("capacity.used")
            tooltip.add(Component.literal(Localization.Tooltip.DiskUsage(used, kiloBytes * 1024)).setStyle(Tooltip.DefaultStyle))
          }
        }
      }
      val data = new DriveData(stack)
      tooltip.add(Component.literal(Localization.Tooltip.DiskMode(data.isUnmanaged)).setStyle(Tooltip.DefaultStyle))
      tooltip.add(Component.literal(Localization.Tooltip.DiskLock(data.lockInfo)).setStyle(Tooltip.DefaultStyle))
    }
  }

  override def use(stack: ItemStack, world: Level, player: Player): InteractionResultHolder[ItemStack] = {
    if (!player.isCrouching && (!stack.hasTag || !stack.getTag.contains(Settings.namespace + "lootFactory"))) {
      if (world.isClientSide) showGui(stack, player)
      player.swing(InteractionHand.MAIN_HAND)
    }
    new InteractionResultHolder[ItemStack](InteractionResult.sidedSuccess(world.isClientSide), stack)
  }

  @OnlyIn(Dist.CLIENT)
  private def showGui(stack: ItemStack, player: Player) {
    Minecraft.getInstance.pushGuiLayer(new gui.Drive(player.getInventory, () => stack))
  }
}
