package li.cil.oc.common.item

import li.cil.oc.{Constants, OpenComputers, Settings}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelReader
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.common.extensions.IForgeItem

class FloppyDisk(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with CustomModel with traits.FileSystemLike {
  // Necessary for anonymous subclasses used for loot disks.
  unlocalizedName = "floppydisk"

  val kiloBytes = Settings.get.floppySize

  @OnlyIn(Dist.CLIENT)
  private def modelLocationFromDyeName(dye: DyeColor) = {
    val clazz = Class.forName("net.minecraft.client.resources.model.ModelResourceLocation")
    val ctor = clazz.getConstructor(classOf[String], classOf[String])
    ctor.newInstance(
      Settings.resourceDomain + ":" + Constants.ItemName.Floppy + "_" + dye.getName,
      "inventory"
    ).asInstanceOf[AnyRef]
  }

  @OnlyIn(Dist.CLIENT)
  override def getModelLocation(stack: ItemStack) = {
    val dyeIndex =
      if (stack.hasTag && stack.getTag.contains(Settings.namespace + "color"))
        stack.getTag.getInt(Settings.namespace + "color")
      else
        DyeColor.GRAY.getId
    modelLocationFromDyeName(DyeColor.byId(dyeIndex max 0 min 15))
  }

  @OnlyIn(Dist.CLIENT)
  override def registerModelLocations(event: ModelEvent.RegisterAdditional): Unit = {
    for (dye <- DyeColor.values) {
      val location = modelLocationFromDyeName(dye)
      event.register(location.asInstanceOf[ResourceLocation])
    }
  }

  override def doesSneakBypassUse(stack: ItemStack, world: LevelReader, pos: BlockPos, player: Player): Boolean = true
}
