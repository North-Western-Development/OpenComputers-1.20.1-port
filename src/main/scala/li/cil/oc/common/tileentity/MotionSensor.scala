package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Node
import li.cil.oc.server.component
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class MotionSensor(selfType: BlockEntityType[_ <: MotionSensor], pos: BlockPos, state: BlockState) extends BlockEntity(selfType, pos, state) with traits.Environment with traits.Tickable {
  val motionSensor = new component.MotionSensor(this)

  def node: Node = motionSensor.node

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      motionSensor.update()
    }
  }

  override def loadForServer(nbt: CompoundTag) {
    super.loadForServer(nbt)
    motionSensor.loadData(nbt)
  }

  override def saveForServer(nbt: CompoundTag) {
    super.saveForServer(nbt)
    motionSensor.saveData(nbt)
  }
}
