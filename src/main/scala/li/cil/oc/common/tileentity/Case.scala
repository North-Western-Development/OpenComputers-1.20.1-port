package li.cil.oc.common.tileentity

import java.util
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common
import li.cil.oc.common.InventorySlots
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.block.property.PropertyRunning
import li.cil.oc.common.container
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.util.Color
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.MenuProvider
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToJava._

class Case(selfType: BlockEntityType[_ <: Case], pos: BlockPos, state: BlockState, var tier: Int) extends BlockEntity(selfType, pos, state) with traits.PowerAcceptor with traits.Computer with traits.Colored with internal.Case with DeviceInfo with MenuProvider {
  def this(selfType: BlockEntityType[_ <: Case], pos: BlockPos, state: BlockState) = {
    this(selfType, pos, state, 0)
    // If no tier was defined when constructing this case, then we don't yet know the inventory size
    // this is set back to true when the nbt data is loaded
    isSizeInventoryReady = false
  }

  // Used on client side to check whether to render disk activity/network indicators.
  var lastFileSystemAccess = 0L
  var lastNetworkActivity = 0L

  setColor(Color.rgbValues(Color.byTier(tier)))

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Computer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Blocker",
    DeviceAttribute.Capacity -> getContainerSize.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override protected def hasConnector(side: Direction) = side != facing

  override protected def connector(side: Direction) = Option(if (side != facing && machine != null) machine.node.asInstanceOf[Connector] else null)

  override def energyThroughput = Settings.get.caseRate(tier)

  def isCreative = tier == Tier.Four

  // ----------------------------------------------------------------------- //

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer && isCreative && getLevel.getGameTime % Settings.get.tickFrequency == 0) {
      // Creative case, make it generate power.
      node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
    }
    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override protected def onRunningChanged(): Unit = {
    super.onRunningChanged()
    getBlockState.getBlock match {
      case block: common.block.Case => {
        val state = getLevel.getBlockState(getBlockPos)
        // race condition that the world no longer has this block at the position (e.g. it was broken)
        if (block == state.getBlock) {
          getLevel.setBlockAndUpdate(getBlockPos, state.setValue(PropertyRunning.Running, Boolean.box(isRunning)))
        }
      }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  private final val TierTag = Settings.namespace + "tier"

  override def loadForServer(nbt: CompoundTag) {
    tier = nbt.getByte(TierTag) max 0 min 3
    setColor(Color.rgbValues(Color.byTier(tier)))
    super.loadForServer(nbt)
    isSizeInventoryReady = true
  }

  override def saveForServer(nbt: CompoundTag) {
    nbt.putByte(TierTag, tier.toByte)
    super.saveForServer(nbt)
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) {
      if (InventorySlots.computer(tier)(slot).slot == Slot.Floppy) {
        common.Sound.playDiskInsert(this)
      }
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      val slotType = InventorySlots.computer(tier)(slot).slot
      if (slotType == Slot.Floppy) {
        common.Sound.playDiskEject(this)
      }
      if (slotType == Slot.CPU) {
        machine.stop()
      }
    }
  }

  override def getContainerSize = if (tier < 0 || tier >= InventorySlots.computer.length) 0 else InventorySlots.computer(tier).length

  override def stillValid(player: Player) =
    super.stillValid(player) && (!isCreative || player.isCreative)

  override def canPlaceItem(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack, getClass)).fold(false)(driver => {
      val provided = InventorySlots.computer(tier)(slot)
      driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
    })

  // ----------------------------------------------------------------------- //

  override def createMenu(id: Int, playerInventory: Inventory, player: Player) =
    new container.Case(ContainerTypes.CASE, id, playerInventory, this, tier)
}
