package li.cil.oc.common.tileentity

import li.cil.oc.{Constants, Settings, api}
import li.cil.oc.common.block.{Print => PrintBlock}
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.client.model.EntityModel
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.{BooleanOp, Shapes, VoxelShape}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.model.data.{ModelData, ModelProperty}

import java.util

class Print(selfType: BlockEntityType[_ <: Print], pos: BlockPos, state2: BlockState, val canToggle: Option[() => Boolean], val scheduleUpdate: Option[Int => Unit], val onStateChange: Option[() => Unit])
  extends BlockEntity(selfType, pos, state2) with traits.TileEntity with traits.RedstoneAware with traits.RotatableTile with ModelData {

  def this(selfType: BlockEntityType[_ <: Print], pos: BlockPos, state: BlockState) = this(selfType, pos, state, None, None, None)
  def this(selfType: BlockEntityType[_ <: Print], pos: BlockPos, state: BlockState, canToggle: () => Boolean, scheduleUpdate: Int => Unit, onStateChange: () => Unit) =
    this(selfType, pos, state, Option(canToggle), Option(scheduleUpdate), Option(onStateChange))

  _isOutputEnabled = true

  val data = new PrintData()

  var shapeOff = Shapes.block
  var shapeOn = Shapes.block
  var state = false

  def shape = if (state) shapeOn else shapeOff
  def noclip = if (state) data.noclipOn else data.noclipOff
  def shapes = if (state) data.stateOn else data.stateOff

  def activate(): Boolean = {
    if (data.hasActiveState) {
      if (!state || !data.isButtonMode) {
        toggleState()
        return true
      }
    }
    false
  }

  private def buildValueSet(value: Int): util.Map[AnyRef, AnyRef] = {
    val map: util.Map[AnyRef, AnyRef] = new util.HashMap[AnyRef, AnyRef]()
    Direction.values.foreach {
      side => map.put(Integer.valueOf(side.ordinal), Integer.valueOf(value))
    }
    map
  }

  def toggleState(): Unit = {
    if (canToggle.fold(true)(_.apply())) {
      state = !state
      getLevel.playSound(null, getBlockPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, if (state) 0.6F else 0.5F)
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      updateRedstone()
      if (state && data.isButtonMode) {
        val block = api.Items.get(Constants.BlockName.Print).block().asInstanceOf[PrintBlock]
        val delay = block.tickRate(getLevel)
        scheduleUpdate match {
          case Some(callback) => callback(delay)
          case _ if !getLevel.isClientSide => getLevel.asInstanceOf[ServerLevel].getBlockTicks.scheduleTick(getBlockPos, block, delay)
          case _ =>
        }
      }
      onStateChange.foreach(_.apply())
    }
  }

  private def convertShape(state: Iterable[PrintData.Shape]): VoxelShape = if (!state.isEmpty) {
    state.foldLeft(Shapes.empty)((curr, s) => {
      val voxel = Shapes.create(s.bounds.rotateTowards(facing))
      Shapes.joinUnoptimized(curr, voxel, BooleanOp.OR)
    }).optimize()
  }
  else Shapes.block

  def updateShape(): Unit = {
    shapeOff = convertShape(data.stateOff)
    shapeOn = convertShape(data.stateOn)
  }

  def updateRedstone(): Unit = {
    if (data.emitRedstone) {
      setOutput(buildValueSet(if (data.emitRedstone(state)) data.redstoneLevel else 0))
    }
  }

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs): Unit = {
    val newState = args.newValue > 0
    if (!data.emitRedstone && data.hasActiveState && state != newState) {
      toggleState()
    }
  }

  override protected def onRotationChanged(): Unit = {
    super.onRotationChanged()
    updateShape()
  }

  // ----------------------------------------------------------------------- //

  private final val DataTag = Settings.namespace + "data"
  @Deprecated
  private final val DataTagCompat = "data"
  private final val StateTag = Settings.namespace + "state"
  @Deprecated
  private final val StateTagCompat = "state"

  override def loadForServer(nbt: CompoundTag): Unit = {
    super.loadForServer(nbt)
    if (nbt.contains(DataTagCompat))
      data.loadData(nbt.getCompound(DataTagCompat))
    else
      data.loadData(nbt.getCompound(DataTag))
    if (nbt.contains(StateTagCompat))
      state = nbt.getBoolean(StateTagCompat)
    else
      state = nbt.getBoolean(StateTag)
    updateShape()
  }

  override def saveForServer(nbt: CompoundTag): Unit = {
    super.saveForServer(nbt)
    nbt.setNewCompoundTag(DataTag, data.saveData)
    nbt.putBoolean(StateTag, state)
  }

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundTag): Unit = {
    super.loadForClient(nbt)
    data.loadData(nbt.getCompound(DataTag))
    state = nbt.getBoolean(StateTag)
    updateShape()
    if (getLevel != null) {
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      if (data.emitLight) getLevel.getLightEngine.checkBlock(getBlockPos)
    }
  }

  override def saveForClient(nbt: CompoundTag): Unit = {
    super.saveForClient(nbt)
    nbt.setNewCompoundTag(DataTag, data.saveData)
    nbt.putBoolean(StateTag, state)
  }

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def getModelData() = this

  @Deprecated
  override def hasProperty(prop: ModelProperty[_]) = false

  @Deprecated
  override def getData[T](prop: ModelProperty[T]): T = null.asInstanceOf[T]

  @Deprecated
  override def setData[T](prop: ModelProperty[T], value: T): T = null.asInstanceOf[T]
}
