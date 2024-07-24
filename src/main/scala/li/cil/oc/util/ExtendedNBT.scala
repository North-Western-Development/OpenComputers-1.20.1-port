package li.cil.oc.util

import com.google.common.base.Charsets
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.core.Direction
import scala.collection.JavaConverters.mapAsScalaMap
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

object ExtendedNBT {

  implicit def toNbt(value: Boolean): ByteTag = ByteTag.valueOf(value)

  implicit def toNbt(value: Byte): ByteTag = ByteTag.valueOf(value)

  implicit def toNbt(value: Short): ShortTag = ShortTag.valueOf(value)

  implicit def toNbt(value: Int): IntTag = IntTag.valueOf(value)

  implicit def toNbt(value: Long): LongTag = LongTag.valueOf(value)

  implicit def toNbt(value: Float): FloatTag = FloatTag.valueOf(value)

  implicit def toNbt(value: Double): DoubleTag = DoubleTag.valueOf(value)

  implicit def toNbt(value: Array[Byte]): ByteArrayTag = new ByteArrayTag(value)

  implicit def toNbt(value: Array[Int]): IntArrayTag = new IntArrayTag(value)

  implicit def toNbt(value: Array[Boolean]): ByteArrayTag = new ByteArrayTag(value.map(if (_) 1: Byte else 0: Byte))

  implicit def toNbt(value: String): StringTag = StringTag.valueOf(value)

  implicit def toNbt(value: ItemStack): CompoundTag = {
    val nbt = new CompoundTag()
    if (value != null) {
      value.save(nbt)
    }
    nbt
  }

  implicit def toNbt(value: CompoundTag => Unit): CompoundTag = {
    val nbt = new CompoundTag()
    value(nbt)
    nbt
  }

  implicit def toNbt(value: Map[String, _]): CompoundTag = {
    val nbt = new CompoundTag()
    for ((key, value) <- value) value match {
      case value: Boolean => nbt.put(key, value)
      case value: Byte => nbt.put(key, value)
      case value: Short => nbt.put(key, value)
      case value: Int => nbt.put(key, value)
      case value: Long => nbt.put(key, value)
      case value: Float => nbt.put(key, value)
      case value: Double => nbt.put(key, value)
      case value: Array[Byte] => nbt.put(key, value)
      case value: Array[Int] => nbt.put(key, value)
      case value: String => nbt.put(key, value)
      case value: ItemStack => nbt.put(key, value)
      case _ =>
    }
    nbt
  }

  def typedMapToNbt(map: Map[_, _]): Tag = {
    def mapToList(value: Array[(_, _)]) = value.collect {
      // Ignore, can be stuff like the 'n' introduced by Lua's `pack`.
      case (k: Number, v) => k -> v
    }.sortBy(_._1.intValue()).map(_._2)
    def asList(value: Option[Any]): IndexedSeq[_] = value match {
      case Some(v: Array[_]) => v
      case Some(v: Map[_, _]) => mapToList(v.toArray)
      case Some(v: mutable.Map[_, _]) => mapToList(v.toArray)
      case Some(v: java.util.Map[_, _]) => mapToList(mapAsScalaMap(v).toArray)
      case Some(v: String) => v.getBytes(Charsets.UTF_8)
      case _ => throw new IllegalArgumentException("Illegal or missing value.")
    }
    def asMap[K](value: Option[Any]): Map[K, _] = value match {
      case Some(v: Map[K, _]@unchecked) => v
      case Some(v: mutable.Map[K, _]@unchecked) => v.toMap
      case Some(v: java.util.Map[K, _]@unchecked) => mapAsScalaMap(v).toMap
      case _ => throw new IllegalArgumentException("Illegal value.")
    }
    val typeAndValue = asMap[String](Option(map))
    val nbtType = typeAndValue.get("type")
    val nbtValue = typeAndValue.get("value")
    nbtType match {
      case Some(n: Number) => n.intValue() match {
        case Tag.TAG_BYTE => ByteTag.valueOf(nbtValue match {
          case Some(v: Number) => v.byteValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case Tag.TAG_SHORT => ShortTag.valueOf(nbtValue match {
          case Some(v: Number) => v.shortValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case Tag.TAG_INT => IntTag.valueOf(nbtValue match {
          case Some(v: Number) => v.intValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case Tag.TAG_LONG => LongTag.valueOf(nbtValue match {
          case Some(v: Number) => v.longValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case Tag.TAG_FLOAT => FloatTag.valueOf(nbtValue match {
          case Some(v: Number) => v.floatValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case Tag.TAG_DOUBLE => DoubleTag.valueOf(nbtValue match {
          case Some(v: Number) => v.doubleValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case Tag.TAG_BYTE_ARRAY => new ByteArrayTag(asList(nbtValue).map {
          case n: Number => n.byteValue()
          case _ => throw new IllegalArgumentException("Illegal value.")
        }.toArray)

        case Tag.TAG_STRING => StringTag.valueOf(nbtValue match {
          case Some(v: String) => v
          case Some(v: Array[Byte]) => new String(v, Charsets.UTF_8)
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case Tag.TAG_LIST =>
          val list = new ListTag()
          asList(nbtValue).map(v => asMap(Option(v))).foreach(v => list.add(typedMapToNbt(v)))
          list

        case Tag.TAG_COMPOUND =>
          val nbt = new CompoundTag()
          val values = asMap[String](nbtValue)
          for ((name, entry) <- values) {
            try nbt.put(name, typedMapToNbt(asMap[Any](Option(entry)))) catch {
              case t: Throwable => throw new IllegalArgumentException(s"Error converting entry '$name': ${t.getMessage}")
            }
          }
          nbt

        case Tag.TAG_INT_ARRAY =>
          new IntArrayTag(asList(nbtValue).map {
            case n: Number => n.intValue()
            case _ => throw new IllegalArgumentException()
          }.toArray)

        case _ => throw new IllegalArgumentException(s"Unsupported NBT type '$n'.")
      }
      case Some(t) => throw new IllegalArgumentException(s"Illegal NBT type '$t'.")
      case _ => throw new IllegalArgumentException(s"Missing NBT type.")
    }
  }

  implicit def booleanIterableToNbt(value: Iterable[Boolean]): Iterable[ByteTag] = value.map(toNbt)

  implicit def byteIterableToNbt(value: Iterable[Byte]): Iterable[ByteTag] = value.map(toNbt)

  implicit def shortIterableToNbt(value: Iterable[Short]): Iterable[ShortTag] = value.map(toNbt)

  implicit def intIterableToNbt(value: Iterable[Int]): Iterable[IntTag] = value.map(toNbt)

  implicit def intArrayIterableToNbt(value: Iterable[Array[Int]]): Iterable[IntArrayTag] = value.map(toNbt)

  implicit def longIterableToNbt(value: Iterable[Long]): Iterable[LongTag] = value.map(toNbt)

  implicit def floatIterableToNbt(value: Iterable[Float]): Iterable[FloatTag] = value.map(toNbt)

  implicit def doubleIterableToNbt(value: Iterable[Double]): Iterable[DoubleTag] = value.map(toNbt)

  implicit def byteArrayIterableToNbt(value: Iterable[Array[Byte]]): Iterable[ByteArrayTag] = value.map(toNbt)

  implicit def stringIterableToNbt(value: Iterable[String]): Iterable[StringTag] = value.map(toNbt)

  implicit def writableIterableToNbt(value: Iterable[CompoundTag => Unit]): Iterable[CompoundTag] = value.map(toNbt)

  implicit def itemStackIterableToNbt(value: Iterable[ItemStack]): Iterable[CompoundTag] = value.map(toNbt)

  implicit def extendINBT(nbt: Tag): ExtendedINBT = new ExtendedINBT(nbt)

  implicit def extendCompoundTag(nbt: CompoundTag): ExtendedCompoundTag = new ExtendedCompoundTag(nbt)

  implicit def extendListTag(nbt: ListTag): ExtendedListTag = new ExtendedListTag(nbt)

  class ExtendedINBT(val nbt: Tag) {
    def toTypedMap: Map[String, _] = Map("type" -> nbt.getId, "value" -> (nbt match {
      case tag: ByteTag => tag.getAsByte
      case tag: ShortTag => tag.getAsShort
      case tag: IntTag => tag.getAsInt
      case tag: LongTag => tag.getAsLong
      case tag: FloatTag => tag.getAsFloat
      case tag: DoubleTag => tag.getAsDouble
      case tag: ByteArrayTag => tag.getAsByteArray
      case tag: StringTag => tag.getAsString
      case tag: ListTag => tag.map((entry: Tag) => entry)
      case tag: CompoundTag => tag.getAllKeys.collect {
        case key: String => key -> tag.get(key)
      }.toMap
      case tag: IntArrayTag => tag.getAsIntArray
      case _ => throw new IllegalArgumentException()
    }))
  }

  class ExtendedCompoundTag(val nbt: CompoundTag) {
    def setNewCompoundTag(name: String, f: (CompoundTag) => Any) = {
      val t = new CompoundTag()
      f(t)
      nbt.put(name, t)
      nbt
    }

    def setNewTagList(name: String, values: Iterable[Tag]) = {
      val t = new ListTag()
      t.append(values)
      nbt.put(name, t)
      nbt
    }

    def setNewTagList(name: String, values: Tag*): CompoundTag = setNewTagList(name, values)

    def getDirection(name: String) = {
      nbt.getByte(name) match {
        case id if id < 0 || id > Direction.values.length => None
        case id => Option(Direction.from3DDataValue(id))
      }
    }

    def setDirection(name: String, d: Option[Direction]): Unit = {
      d match {
        case Some(side) => nbt.putByte(name, side.ordinal.toByte)
        case _ => nbt.putByte(name, -1: Byte)
      }
    }

    def getBooleanArray(name: String) = nbt.getByteArray(name).map(_ == 1)

    def setBooleanArray(name: String, value: Array[Boolean]) = nbt.put(name, toNbt(value))
  }

  class ExtendedListTag(val nbt: ListTag) {
    def appendNewCompoundTag(f: (CompoundTag) => Unit) {
      val t = new CompoundTag()
      f(t)
      nbt.add(t)
    }

    def append(values: Iterable[Tag]) {
      for (value <- values) {
        nbt.add(value)
      }
    }

    def append(values: Tag*): Unit = append(values)

    def foreach[T <: Tag](f: T => Unit) {
      val iterable = nbt.copy(): ListTag
      while (iterable.size > 0) {
        f((iterable.remove(0): Tag).asInstanceOf[T])
      }
    }

    def map[T <: Tag, Value](f: T => Value): IndexedSeq[Value] = {
      val iterable = nbt.copy(): ListTag
      val buffer = mutable.ArrayBuffer.empty[Value]
      while (iterable.size > 0) {
        buffer += f((iterable.remove(0): Tag).asInstanceOf[T])
      }
      buffer.toIndexedSeq
    }

    def toTagArray[Tag: ClassTag] = map((t: Tag) => t).toArray
  }

}
