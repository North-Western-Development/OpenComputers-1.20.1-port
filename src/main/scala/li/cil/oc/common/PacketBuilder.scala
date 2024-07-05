package li.cil.oc.common

import java.util.function.Supplier

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import io.netty.buffer.Unpooled
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.api.network.EnvironmentHost
import net.minecraft.world.entity.Entity
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.Direction
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.server.ServerLevel
import net.minecraftforge.fml.network.PacketDistributor
import net.minecraftforge.fml.server.ServerLifecycleHooks
import net.minecraftforge.registries._

import scala.collection.convert.ImplicitConversionsToScala._

abstract class PacketBuilder(stream: OutputStream) extends DataOutputStream(stream) {
  def writeRegistryEntry[T <: IForgeRegistryEntry[T]](registry: IForgeRegistry[T], value: T): Unit =
    writeInt(registry.asInstanceOf[ForgeRegistry[T]].getID(value))

  def writeBlockEntity(t: BlockEntity) {
    writeUTF(t.getLevel.dimension.location.toString)
    writeInt(t.getBlockPos.getX)
    writeInt(t.getBlockPos.getY)
    writeInt(t.getBlockPos.getZ)
  }

  def writeEntity(e: Entity) {
    writeUTF(e.level.dimension.location.toString)
    writeInt(e.getId)
  }

  def writeDirection(d: Option[Direction]) = d match {
    case Some(side) => writeByte(side.ordinal.toByte)
    case _ => writeByte(-1: Byte)
  }

  def writeItemStack(stack: ItemStack) = {
    val haveStack = !stack.isEmpty && stack.getCount > 0
    writeBoolean(haveStack)
    if (haveStack) {
      writeNBT(stack.save(new CompoundTag()))
    }
  }

  def writeNBT(nbt: CompoundTag) = {
    val haveNbt = nbt != null
    writeBoolean(haveNbt)
    if (haveNbt) {
      CompressedStreamTools.write(nbt, this)
    }
  }

  def writeMedium(v: Int) = {
    writeByte(v & 0xFF)
    writeByte((v >> 8) & 0xFF)
    writeByte((v >> 16) & 0xFF)
  }

  def writePacketType(pt: PacketType.Value) = writeByte(pt.id)

  def sendToAllPlayers() = OpenComputers.channel.send(PacketDistributor.ALL.noArg(), packet)

  def sendToPlayersNearEntity(e: Entity, range: Option[Double] = None): Unit = sendToNearbyPlayers(e.level, e.getX, e.getY, e.getZ, range)

  def sendToPlayersNearHost(host: EnvironmentHost, range: Option[Double] = None): Unit = {
    host match {
      case t: BlockEntity => sendToPlayersNearBlockEntity(t, range)
      case _ => sendToNearbyPlayers(host.world, host.xPosition, host.yPosition, host.zPosition, range)
    }
  }

  def sendToPlayersNearBlockEntity(t: BlockEntity, range: Option[Double] = None) {
    t.getLevel match {
      case w: ServerLevel =>
        val chunk = new ChunkPos(t.getBlockPos)

        val manager = ServerLifecycleHooks.getCurrentServer.getPlayerList
        var maxPacketRange = range.getOrElse((manager.getViewDistance + 1) * 16.0)
        val maxPacketRangeConfig = Settings.get.maxNetworkClientPacketDistance
        if (maxPacketRangeConfig > 0.0D) {
          maxPacketRange = maxPacketRange min maxPacketRangeConfig
        }
        val maxPacketRangeSq = maxPacketRange * maxPacketRange

        w.getChunkSource.chunkMap.getPlayers(chunk, false).forEach {
          case player =>
            if (player.distanceToSqr(t.getBlockPos.getX + 0.5D, t.getBlockPos.getY + 0.5D, t.getBlockPos.getZ + 0.5D) <= maxPacketRangeSq)
              sendToPlayer(player)
        }
      case _ => sendToNearbyPlayers(t.getLevel, t.getBlockPos.getX + 0.5D, t.getBlockPos.getY + 0.5D, t.getBlockPos.getZ + 0.5D, range)
    }
  }

  def sendToNearbyPlayers(world: Level, x: Double, y: Double, z: Double, range: Option[Double]) {
    val server = ServerLifecycleHooks.getCurrentServer
    val manager = server.getPlayerList

    var maxPacketRange = range.getOrElse((manager.getViewDistance + 1) * 16.0)
    val maxPacketRangeConfig = Settings.get.maxNetworkClientPacketDistance
    if (maxPacketRangeConfig > 0.0D) {
      maxPacketRange = maxPacketRange min maxPacketRangeConfig
    }
    val maxPacketRangeSq = maxPacketRange * maxPacketRange

    for (player <- manager.getPlayers if player.level == world) {
      if (player.distanceToSqr(x, y, z) <= maxPacketRangeSq) {
        sendToPlayer(player)
      }
    }
  }

  def sendToPlayer(player: ServerPlayer) = OpenComputers.channel.send(PacketDistributor.PLAYER.`with`(new Supplier[ServerPlayer] {
    override def get = player
  }), packet)

  def sendToServer() = OpenComputers.channel.sendToServer(packet)

  protected def packet: Array[Byte]
}

// Necessary to keep track of the GZIP stream.
abstract class PacketBuilderBase[T <: OutputStream](protected val stream: T) extends PacketBuilder(new BufferedOutputStream(stream))

class SimplePacketBuilder(val packetType: PacketType.Value) extends PacketBuilderBase(PacketBuilder.newData(compressed = false)) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    stream.toByteArray
  }
}

class CompressedPacketBuilder(val packetType: PacketType.Value, private val data: ByteArrayOutputStream = PacketBuilder.newData(compressed = true)) extends PacketBuilderBase(new DeflaterOutputStream(data, new Deflater(Deflater.BEST_SPEED))) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    stream.finish()
    data.toByteArray
  }
}

object PacketBuilder {
  def newData(compressed: Boolean) = {
    val data = new ByteArrayOutputStream
    data.write(if (compressed) 1 else 0)
    data
  }
}
