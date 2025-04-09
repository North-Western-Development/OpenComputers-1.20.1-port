package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.{ImageProvider, ImageRenderer, InteractiveImageRenderer}
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags._
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.world.level.block.Block
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.tags.ITag

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

object OreDictImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val desired = new ResourceLocation(data.toLowerCase)
    val stacks = mutable.ArrayBuffer.empty[ItemStack]
    ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, desired)) match {
      case tag: ITag[Item] => stacks ++= tag.stream().map(new ItemStack(_)).toList
      case _ =>
    }
    if (stacks.isEmpty) {
      ForgeRegistries.BLOCKS.tags().getTag(TagKey.create(Registries.BLOCK, desired)) match {
        case tag: ITag[Block] => stacks ++= tag.stream().map(new ItemStack(_)).toList
        case _ =>
      }
    }
    if (stacks.nonEmpty) new ItemStackImageRenderer(stacks.toArray)
    else new TextureImageRenderer(TextureImageProvider.ManualMissingItem) with InteractiveImageRenderer {
      override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.OreDictMissing"

      override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
    }
  }
}
