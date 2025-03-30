package li.cil.oc.client

import li.cil.oc.{Constants, api}
import li.cil.oc.api.internal.Colored
import li.cil.oc.common.block
import li.cil.oc.util.{Color, ItemColorizer, ItemUtils}
import net.minecraft.client.Minecraft
import net.minecraft.client.color.block.BlockColor
import net.minecraft.client.color.item.ItemColor
import net.minecraft.core.BlockPos
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.{BlockAndTintGetter, BlockGetter, ItemLike}

object ColorHandler {
  def init(): Unit = {
    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Cable => block.colorMultiplierOverride.getOrElse(0xFFFFFFFF)
      case _ => 0xFFFFFFFF
    },
      api.Items.get(Constants.BlockName.Cable).block())

    register((state, world, pos, tintIndex) => if (pos == null) 0xFFFFFFFF else world.getBlockEntity(pos) match {
      case colored: Colored => colored.getColor
      case _ => state.getBlock match {
        case block: block.Case => Color.rgbValues(Color.byTier(block.tier))
        case _ => 0xFFFFFFFF
      }
    },
      api.Items.get(Constants.BlockName.CaseTier1).block(),
      api.Items.get(Constants.BlockName.CaseTier2).block(),
      api.Items.get(Constants.BlockName.CaseTier3).block(),
      api.Items.get(Constants.BlockName.CaseCreative).block())

    register((state, world, pos, tintIndex) => Color.rgbValues(state.getValue(block.ChameliumBlock.Color)),
      api.Items.get(Constants.BlockName.ChameliumBlock).block())

    register((state, world, pos, tintIndex) => tintIndex,
      api.Items.get(Constants.BlockName.Print).block())

    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Screen => Color.rgbValues(Color.byTier(block.tier))
      case _ => 0xFFFFFFFF
    },
      api.Items.get(Constants.BlockName.ScreenTier1).block(),
      api.Items.get(Constants.BlockName.ScreenTier2).block(),
      api.Items.get(Constants.BlockName.ScreenTier3).block())

    register((stack, tintIndex) => if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else tintIndex,
      api.Items.get(Constants.BlockName.Cable).block())

    register((stack, tintIndex) => Color.rgbValues(Color.byTier(ItemUtils.caseTier(stack))),
      api.Items.get(Constants.BlockName.CaseTier1).block(),
      api.Items.get(Constants.BlockName.CaseTier2).block(),
      api.Items.get(Constants.BlockName.CaseTier3).block(),
      api.Items.get(Constants.BlockName.CaseCreative).block())

    register((stack, tintIndex) => Color.rgbValues(DyeColor.byId(stack.getDamageValue)),
      api.Items.get(Constants.BlockName.ChameliumBlock).block())

    register((stack, tintIndex) => tintIndex,
      api.Items.get(Constants.BlockName.ScreenTier1).block(),
      api.Items.get(Constants.BlockName.ScreenTier2).block(),
      api.Items.get(Constants.BlockName.ScreenTier3).block(),
      api.Items.get(Constants.BlockName.Print).block(),
      api.Items.get(Constants.BlockName.Robot).block())

    register((stack, tintIndex) =>
      if (tintIndex == 1) {
        if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else 0x66DD55
      } else 0xFFFFFF,
      api.Items.get(Constants.ItemName.HoverBoots).item())
  }

  def register(handler: (BlockState, BlockGetter, BlockPos, Int) => Int, blocks: Block*): Unit = {
    Minecraft.getInstance.getBlockColors.register(new BlockColor {
      override def getColor(state: BlockState, world: BlockAndTintGetter, pos: BlockPos, tintIndex: Int): Int = handler(state, world, pos, tintIndex)
    }, blocks: _*)
  }

  def register(handler: (ItemStack, Int) => Int, items: ItemLike*): Unit = {
    Minecraft.getInstance.getItemColors.register(new ItemColor {
      override def getColor(stack: ItemStack, tintIndex: Int): Int = handler(stack, tintIndex)
    }, items: _*)
  }
}
