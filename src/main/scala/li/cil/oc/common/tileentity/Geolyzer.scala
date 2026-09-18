package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class Geolyzer(selfType: BlockEntityType[_ <: Geolyzer], pos: BlockPos, state: BlockState) extends BlockEntity(selfType, pos, state) with traits.Environment {
  val geolyzer = new component.Geolyzer(this)

  def node = geolyzer.node

  override def loadForServer(nbt: CompoundTag) {
    super.loadForServer(nbt)
    geolyzer.loadData(nbt)
  }

  override def saveForServer(nbt: CompoundTag) {
    super.saveForServer(nbt)
    geolyzer.saveData(nbt)
  }
}
