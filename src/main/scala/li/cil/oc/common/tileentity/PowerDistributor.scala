package li.cil.oc.common.tileentity

import li.cil.oc.{Settings, api}
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.nbt.{CompoundTag, Tag}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}

class PowerDistributor(selfType: BlockEntityType[_ <: PowerDistributor], pos: BlockPos, state: BlockState) extends BlockEntity(selfType, pos, state) with traits.Environment with traits.PowerBalancer with traits.NotAnalyzable {
  val node = null

  private val nodes = Array.fill(6)(api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferDistributor).
    create())

  override protected def isConnected: Boolean = nodes.exists(node => node.address != null && node.network != null)

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction) = true

  override def sidedNode(side: Direction): Connector = nodes(side.ordinal)

  // ----------------------------------------------------------------------- //

  private final val ConnectorTag = Settings.namespace + "connector"

  override def loadForServer(nbt: CompoundTag) {
    super.loadForServer(nbt)
    nbt.getList(ConnectorTag, Tag.TAG_COMPOUND).toTagArray[CompoundTag].
      zipWithIndex.foreach {
      case (tag, index) => nodes(index).loadData(tag)
    }
  }

  override def saveForServer(nbt: CompoundTag) {
    super.saveForServer(nbt)
    // Side check for Waila (and other mods that may call this client side).
    if (isServer) {
      nbt.setNewTagList(ConnectorTag, nodes.map(connector => {
        val connectorNbt = new CompoundTag()
        connector.saveData(connectorNbt)
        connectorNbt
      }))
    }
  }
}
