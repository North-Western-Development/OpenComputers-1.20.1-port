package li.cil.oc.common.tileentity.traits

import li.cil.oc.client.Sound
import li.cil.oc.common.SaveHandler
import li.cil.oc.util.{BlockPosition, SideTracker}
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.{ClientGamePacketListener, ClientboundBlockEntityDataPacket}
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}

trait TileEntity extends BlockEntity {
  private final val IsServerDataTag = Settings.namespace + "isServerData"

  def x: Int = getBlockPos.getX

  def y: Int = getBlockPos.getY

  def z: Int = getBlockPos.getZ

  def position = BlockPosition(x, y, z, getLevel)

  def isClient: Boolean = !isServer

  def isServer: Boolean = if (getLevel != null) !getLevel.isClientSide else SideTracker.isServer

  // ----------------------------------------------------------------------- //

  def updateEntity() {
    if (Settings.get.periodicallyForceLightUpdate && getLevel.getGameTime % 40 == 0 && getBlockState.getLightBlock(getLevel, getBlockPos) > 0) {
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
    }
  }

  override def clearRemoved() {
    super.clearRemoved()
    initialize()
  }

  override def setRemoved() {
    super.setRemoved()
    dispose()
  }

  override def onChunkUnloaded() {
    super.onChunkUnloaded()
    try dispose() catch {
      case t: Throwable => OpenComputers.log.error("Failed properly disposing a tile entity, things may leak and or break.", t)
    }
  }

  protected def initialize() {
  }

  def dispose() {
    if (isClient) {
      // Note: chunk unload is handled by sound via event handler.
      Sound.stopLoop(this)
    }
  }

  // ----------------------------------------------------------------------- //

  def loadForServer(nbt: CompoundTag) {}

  def saveForServer(nbt: CompoundTag): Unit = {
    nbt.putBoolean(IsServerDataTag, true)
  }

  @OnlyIn(Dist.CLIENT)
  def loadForClient(nbt: CompoundTag) {}

  def saveForClient(nbt: CompoundTag): Unit = {
    nbt.putBoolean(IsServerDataTag, false)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: CompoundTag): Unit = {
    super.load(nbt)
    if (isServer || nbt.getBoolean(IsServerDataTag)) {
      loadForServer(nbt)
    }
    else {
      loadForClient(nbt)
    }
  }

  override def saveAdditional(nbt: CompoundTag): CompoundTag = {
    if (isServer) {
      saveForServer(nbt)
    }
    nbt
  }

  override def getUpdatePacket: Packet[ClientGamePacketListener] = {
    // Obfuscation workaround. If it works.
    val te = this.asInstanceOf[net.minecraft.world.level.block.entity.BlockEntity]
    ClientboundBlockEntityDataPacket.create(te)
  }

  override def getUpdateTag: CompoundTag = {
    val nbt = super.getUpdateTag

    // See comment on savingForClients variable.
    SaveHandler.savingForClients = true
    try {
      try saveForClient(nbt) catch {
        case e: Throwable => OpenComputers.log.warn("There was a problem writing a TileEntity description packet. Please report this if you see it!", e)
      }
    } finally {
      SaveHandler.savingForClients = false
    }

    nbt
  }
  override def handleUpdateTag(tag: CompoundTag) {
    try loadForClient(tag) catch {
      case e: Throwable => OpenComputers.log.warn("There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
