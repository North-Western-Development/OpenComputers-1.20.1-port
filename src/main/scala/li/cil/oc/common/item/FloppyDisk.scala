package li.cil.oc.common.item

import li.cil.oc.{Constants, Settings}
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.{DyeColor, Item, ItemStack}
import net.minecraft.world.level.LevelReader
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.common.extensions.IForgeItem

class FloppyDisk(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with CustomModel with traits.FileSystemLike {
  // Necessary for anonymous subclasses used for loot disks.
  unlocalizedName = "floppydisk"

  val kiloBytes = Settings.get.floppySize

  @OnlyIn(Dist.CLIENT)
  private def modelLocationFromDyeName(dye: DyeColor) = {
    new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.ItemName.Floppy + "_" + dye.getName), "inventory")
  }

  @OnlyIn(Dist.CLIENT)
  override def getModelLocation(stack: ItemStack): ModelResourceLocation = {
    val dyeIndex =
      if (stack.hasTag && stack.getTag.contains(Settings.namespace + "color"))
        stack.getTag.getInt(Settings.namespace + "color")
      else
        DyeColor.GRAY.getId
    modelLocationFromDyeName(DyeColor.byId(dyeIndex max 0 min 15))
  }

  @OnlyIn(Dist.CLIENT)
  override def registerModelLocations(): Unit = {
    for (dye <- DyeColor.values) {
      val location = modelLocationFromDyeName(dye)
      ModelLoader.addSpecialModel(location)
    }
  }

  override def doesSneakBypassUse(stack: ItemStack, world: LevelReader, pos: BlockPos, player: Player): Boolean = true
}
