package li.cil.oc.server.component

import li.cil.oc.{Constants, Settings}
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.{Network, internal}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{EnvironmentHost, Visibility}
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.{BlockPosition, InventoryUtils}
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.item.ItemEntity

import java.util
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

object UpgradeTractorBeam {

  abstract class Common extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("tractor_beam").
    create()

  private val pickupRadius = 3

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Generic,
      DeviceAttribute.Description -> "Tractor beam",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "T313-K1N.3515"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo

  protected def position: BlockPosition

  protected def collectItem(item: ItemEntity): Unit

  private def world = position.world.get

  @Callback(doc = """function():boolean -- Tries to pick up a random item in the robots' vicinity.""")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val items = world.getEntitiesOfClass(classOf[ItemEntity], position.bounds.inflate(pickupRadius, pickupRadius, pickupRadius))
      .filter(item => item.isAlive && !item.hasPickUpDelay)
    if (items.nonEmpty) {
      val item = items(world.random.nextInt(items.size))
      val stack = item.getItem
      val size = stack.getCount
      collectItem(item)
      if (stack.getCount < size || !item.isAlive) {
        context.pause(Settings.get.suckDelay)
        world.levelEvent(2003, new BlockPos(math.floor(item.getX).toInt, math.floor(item.getY).toInt, math.floor(item.getZ).toInt), 0)
        return result(true)
      }
    }
    result(false)
  }
  }

  class Player(val owner: EnvironmentHost, val player: () => net.minecraft.world.entity.player.Player) extends Common {
    override protected def position = BlockPosition(owner)

    override protected def collectItem(item: ItemEntity) = item.playerTouch(player())
  }

  class Drone(val owner: internal.Agent) extends Common {
    override protected def position = BlockPosition(owner)

    override protected def collectItem(item: ItemEntity) = {
      InventoryUtils.insertIntoInventory(item.getItem, owner.mainInventory, None, 64, simulate = false, Some(insertionSlots))
    }

    private def insertionSlots = (owner.selectedSlot until owner.mainInventory.getContainerSize) ++ (0 until owner.selectedSlot)
  }

}
