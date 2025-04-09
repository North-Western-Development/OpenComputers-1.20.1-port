package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.network.{EnvironmentHost, Visibility}
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.{Constants, Settings}
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome

import java.util
import scala.collection.convert.ImplicitConversionsToJava._

class UpgradeSolarGenerator(val host: EnvironmentHost) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withConnector().
    create()

  var ticksUntilCheck = 0

  var isSunShining = false

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Solar panel",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Enligh10"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()

    ticksUntilCheck -= 1
    if (ticksUntilCheck <= 0) {
      ticksUntilCheck = 100
      isSunShining = isSunVisible
    }
    if (isSunShining) {
      node.changeBuffer(Settings.get.solarGeneratorEfficiency)
    }
  }

  private def isSunVisible = {
    val blockPos = BlockPosition(host).offset(Direction.UP)
    host.world.isDay &&
      (host.world.dimension != Level.NETHER) &&
      host.world.canSeeSkyFromBelowWater(blockPos.toBlockPos) &&
      (host.world.getBiome(blockPos.toBlockPos).get().getPrecipitationAt(blockPos.toBlockPos) == Biome.Precipitation.NONE || (!host.world.isRaining && !host.world.isThundering))
  }
}
