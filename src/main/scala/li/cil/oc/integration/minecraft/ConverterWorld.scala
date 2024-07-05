package li.cil.oc.integration.minecraft

import java.nio.charset.StandardCharsets
import java.util
import java.util.UUID

import com.google.common.hash.Hashing
import li.cil.oc.api
import net.minecraft.world.level.Level
import net.minecraft.world.server.ServerLevel

import scala.collection.convert.ImplicitConversionsToScala._

object ConverterLevel extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) = {
    value match {
      case world: ServerLevel =>
        output += "id" -> UUID.nameUUIDFromBytes(Hashing.md5().newHasher().
          putLong(world.getSeed).
          putString(world.dimension.location.toString, StandardCharsets.UTF_8).
          hash().asBytes()).toString
      case _ =>
    }

    value match {
      case world: Level =>
        output += "name" -> world.dimension.location.toString
      case _ =>
    }
  }
}
