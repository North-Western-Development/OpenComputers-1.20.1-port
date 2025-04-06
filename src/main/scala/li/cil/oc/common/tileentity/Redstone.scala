package li.cil.oc.common.tileentity

import li.cil.oc.{Settings, api}
import li.cil.oc.api.network.{Component, Node, Visibility}
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.component
import li.cil.oc.server.component.RedstoneVanilla
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState

class Redstone(selfType: BlockEntityType[_ <: Redstone], pos: BlockPos, state: BlockState)
  extends BlockEntity(selfType, pos, state) with traits.Environment with traits.BundledRedstoneAware with traits.Tickable {
  val instance: RedstoneVanilla =
    if (BundledRedstone.isAvailable)
      new component.Redstone.Bundled(this)
    else
      new component.Redstone.Vanilla(this)
  instance.wakeNeighborsOnly = false
  val node: Component = instance.node
  val dummyNode: Node = if (node != null) {
    node.setVisibility(Visibility.Network)
    _isOutputEnabled = true
    api.Network.newNode(this, Visibility.None).create()
  }
  else null

  // ----------------------------------------------------------------------- //

  private final val RedstoneTag = Settings.namespace + "redstone"

  override def loadForServer(nbt: CompoundTag) {
    super.loadForServer(nbt)
    instance.loadData(nbt.getCompound(RedstoneTag))
  }

  override def saveForServer(nbt: CompoundTag) {
    super.saveForServer(nbt)
    nbt.setNewCompoundTag(RedstoneTag, instance.saveData)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs) {
    super.onRedstoneInputChanged(args)
    if (node != null && node.network != null) {
      node.connect(dummyNode)
      dummyNode.sendToNeighbors("redstone.changed", args)
    }
  }
}
