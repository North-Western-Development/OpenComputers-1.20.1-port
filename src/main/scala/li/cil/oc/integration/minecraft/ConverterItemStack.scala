package li.cil.oc.integration.minecraft

import java.util
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.nbt.{CompoundTag, ListTag, StringTag, Tag}
import net.minecraft.world.item.enchantment.EnchantmentHelper

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

object ConverterItemStack extends api.driver.Converter {
  def getTagValue(tag: CompoundTag, key: String): AnyRef = tag.getTagType(key) match {
    case Tag.TAG_INT => Int.box(tag.getInt(key))
    case Tag.TAG_STRING => tag.getString(key)
    case Tag.TAG_BYTE => Byte.box(tag.getByte(key))
    case Tag.TAG_COMPOUND => tag.getCompound(key)
    case Tag.TAG_LIST => tag.getList(key, Tag.TAG_STRING)
    case _ => null
  }

  def withTag(tag: CompoundTag, key: String, tagId: Int, f: AnyRef => AnyRef): AnyRef = {
    if (tag.contains(key, tagId)) {
      Option(getTagValue(tag, key)) match {
        case Some(value) => f(value)
        case _ => null
      }
    } else null
  }

  def withCompound(tag: CompoundTag, key: String, f: CompoundTag => AnyRef): AnyRef = {
    withTag(tag, key, Tag.TAG_COMPOUND, { case value: CompoundTag => f(value)})
  }

  def withList(tag: CompoundTag, key: String, f: ListTag => AnyRef): AnyRef = {
    withTag(tag, key, Tag.TAG_STRING, { case value: ListTag => f(value)})
  }

  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(Item.getId(stack.getItem))
          output += "oreNames" -> stack.getTags.map(_.toString).toArray
        }
        output += "damage" -> Int.box(stack.getDamageValue)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.getCount)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(stack.hasTag)
        output += "name" -> stack.getItem.getRegistryName
        output += "label" -> stack.getDisplayName.getString

        // custom mod tags
        if (stack.hasTag) {
          val tags = stack.getTag

          //Lore tags
          withCompound(tags, "display", withList(_, "Lore", {
              output += "lore" -> _.map((tag: StringTag) => tag.getAsString).mkString("\n")
            })
          )

          withTag(tags, "Energy", Tag.TAG_INT, value => output += "Energy" -> value)

          if (Settings.get.allowItemStackNBTTags) {
            output += "tag" -> ItemUtils.saveTag(stack.getTag)
          }
        }

        val enchantments = mutable.ArrayBuffer.empty[mutable.Map[String, Any]]
        EnchantmentHelper.getEnchantments(stack).collect {
          case (enchantment, level) =>
            val map = mutable.Map[String, Any](
              "name" -> enchantment.getRegistryName,
              "label" -> enchantment.getFullname(level),
              "level" -> level
            )
            enchantments += map
        }
        if (enchantments.nonEmpty) {
          output += "enchantments" -> enchantments
        }
      case _ =>
    }
}
