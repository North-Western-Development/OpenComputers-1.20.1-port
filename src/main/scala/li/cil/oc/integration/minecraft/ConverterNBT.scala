package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import net.minecraft.nbt._

import scala.collection.convert.ImplicitConversionsToScala._

object ConverterNBT extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case nbt: CompoundTag => output += "oc:flatten" -> convert(nbt)
      case _ =>
    }

  private def convert(nbt: Tag): AnyRef = nbt match {
    case tag: ByteTag => Byte.box(tag.getAsByte)
    case tag: ShortTag => Short.box(tag.getAsShort)
    case tag: IntTag => Int.box(tag.getAsInt)
    case tag: LongTag => Long.box(tag.getAsLong)
    case tag: FloatTag => Float.box(tag.getAsFloat)
    case tag: DoubleTag => Double.box(tag.getAsDouble)
    case tag: ByteArrayTag => tag.getAsByteArray
    case tag: StringTag => tag.getAsString
    case tag: ListTag =>
      val copy = tag.copy(): ListTag
      (0 until copy.size).map(_ => convert(copy.remove(0))).toArray
    case tag: CompoundTag =>
      tag.getAllKeys.collect {
        case key: String => key -> convert(tag.get(key))
      }.toMap
    case tag: IntArrayTag => tag.getAsIntArray
  }
}
