package li.cil.oc.server.component

import li.cil.oc.{Constants, Settings}
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.common.item.TabletWrapper

import java.util
import scala.collection.convert.ImplicitConversionsToJava._

class Tablet(val tablet: TabletWrapper) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("tablet").
    withConnector(Settings.get.bufferTablet).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Tablet",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Jogger",
    DeviceAttribute.Capacity -> tablet.getContainerSize.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number -- Gets the pitch of the player holding the tablet.""")
  def getPitch(context: Context, args: Arguments): Array[AnyRef] = result(tablet.player.getXRot)
  
  @Callback(doc = """function():number -- Gets the yaw of the player holding the tablet.""")
  def getYaw(context: Context, args: Arguments): Array[AnyRef] = result(tablet.player.getYRot)
}
