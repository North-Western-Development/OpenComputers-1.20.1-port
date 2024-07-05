package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.event.SignChangeEvent
import li.cil.oc.api.internal
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Message
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedLevel._
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.tileentity.SignBlockEntity
import net.minecraft.core.Direction
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.server.ServerLevel
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.eventbus.api.Event

import scala.collection.convert.ImplicitConversionsToJava._

abstract class UpgradeSign extends AbstractManagedEnvironment with DeviceInfo {
  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Sign upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Labelizer Deluxe"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  def host: EnvironmentHost

  protected def getValue(tileEntity: Option[SignBlockEntity]): Array[AnyRef] = {
    tileEntity match {
      case Some(sign) => result(sign.messages.map(_.getString).mkString("\n"))
      case _ => result((), "no sign")
    }
  }

  protected def setValue(tileEntity: Option[SignBlockEntity], text: String): Array[AnyRef] = {
    tileEntity match {
      case Some(sign) =>
        val player = host match {
          case robot: internal.Robot => robot.player
          case _ => FakePlayerFactory.get(host.world.asInstanceOf[ServerLevel], Settings.get.fakePlayerProfile)
        }

        val lines = text.linesIterator.padTo(4, "").map(line => if (line.length > 15) line.substring(0, 15) else line).toArray

        if (!canChangeSign(player, sign, lines)) {
          return result((), "not allowed")
        }

        lines.map(line => new StringTextComponent(line)).copyToArray(sign.messages)
        host.world.notifyBlockUpdate(sign.getBlockPos)

        MinecraftForge.EVENT_BUS.post(new SignChangeEvent.Post(sign, lines))

        result(sign.messages.mkString("\n"))
      case _ => result((), "no sign")
    }
  }

  protected def findSign(side: Direction) = {
    val hostPos = BlockPosition(host)
    host.world.getBlockEntity(hostPos) match {
      case sign: SignBlockEntity => Option(sign)
      case _ => host.world.getBlockEntity(hostPos.offset(side)) match {
        case sign: SignBlockEntity => Option(sign)
        case _ => None
      }
    }
  }

  private def canChangeSign(player: Player, tileEntity: SignBlockEntity, lines: Array[String]): Boolean = {
    if (!host.world.mayInteract(player, tileEntity.getBlockPos)) {
      return false
    }
    val event = new BlockEvent.BreakEvent(host.world, tileEntity.getBlockPos, tileEntity.getLevel.getBlockState(tileEntity.getBlockPos), player)
    MinecraftForge.EVENT_BUS.post(event)
    if (event.isCanceled || event.getResult == Event.Result.DENY) {
      return false
    }

    val signEvent = new SignChangeEvent.Pre(tileEntity, lines)
    MinecraftForge.EVENT_BUS.post(signEvent)
    !(signEvent.isCanceled || signEvent.getResult == Event.Result.DENY)
  }

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.name == "tablet.use") message.source.host match {
      case machine: api.machine.Machine => (machine.host, message.data) match {
        case (tablet: internal.Tablet, Array(nbt: CompoundTag, stack: ItemStack, player: Player, blockPos: BlockPosition, side: Direction, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          host.world.getBlockEntity(blockPos) match {
            case sign: SignBlockEntity =>
              nbt.putString("signText", sign.messages.map(_.getString).mkString("\n"))
            case _ =>
          }
        case _ => // Ignore.
      }
      case _ => // Ignore.
    }
  }
}
