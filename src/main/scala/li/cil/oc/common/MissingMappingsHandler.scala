package li.cil.oc.common

import com.google.common.base.Strings
import li.cil.oc.{Constants, OpenComputers}
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.{ForgeRegistries, MissingMappingsEvent}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object MissingMappingsHandler {

  // Yes, this could be boiled down even further, but I like to keep it
  // explicit like this, because it makes it a) clearer, b) easier to
  // extend, in case that should ever be needed.

  // Example usage: OpenComputers.ID + ":rack" -> "serverRack"
  private val blockRenames = Map[String, String](
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack // Yay, full circle >_>
  )

  // Example usage: OpenComputers.ID + ":tabletCase" -> "tabletCase1"
  private val itemRenames = Map[String, String](
    OpenComputers.ID + ":dataCard" -> Constants.ItemName.DataCardTier1,
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack,
    OpenComputers.ID + ":wlanCard" -> Constants.ItemName.WirelessNetworkCardTier2
  )

  @SubscribeEvent
  def missingMappings(e: MissingMappingsEvent): Unit = {
    for (missing <- e.getMappings(ForgeRegistries.Keys.BLOCKS, OpenComputers.ID).asScala) {
      blockRenames.get(missing.getKey.getPath) match {
        case Some(name) =>
          if (Strings.isNullOrEmpty(name)) {
            missing.ignore()
          }
          else {
            val block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name))
            if (block != null) missing.remap(block)
            else missing.warn()
          }
        case _ => missing.warn()
      }
    }

    for (missing <- e.getMappings(ForgeRegistries.Keys.ITEMS, OpenComputers.ID).asScala) {
      itemRenames.get(missing.getKey.getPath) match {
        case Some(name) =>
          if (Strings.isNullOrEmpty(name)) {
            missing.ignore()
          }
          else {
            val item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name))
            if (item != null) missing.remap(item)
            else missing.warn()
          }
        case _ => missing.warn()
      }
    }
  }
}
