package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedLevel._
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.extensions.IForgeItem

class Debugger(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def onItemUse(stack: ItemStack, player: Player, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = position.world.get
    player match {
      case _: FakePlayer => false // Nope
      case realPlayer: ServerPlayer =>
        world.getBlockEntity(position) match {
          case host: SidedEnvironment =>
            if (!world.isClientSide) {
              Debugger.reconnect(Array(host.sidedNode(side)))
            }
            true
          case host: Environment =>
            if (!world.isClientSide) {
              Debugger.reconnect(Array(host.node))
            }
            true
          case _ =>
            if (!world.isClientSide) {
              Debugger.node.remove()
            }
            true
        }
      case _ => false
    }
  }
}

object Debugger extends Environment {
  var node = api.Network.newNode(this, Visibility.Network).create()

  override def onConnect(node: Node) {
    OpenComputers.log.info(s"[NETWORK DEBUGGER] New node in network: ${nodeInfo(node)}")
  }

  override def onDisconnect(node: Node) {
    OpenComputers.log.info(s"[NETWORK DEBUGGER] Node removed from network: ${nodeInfo(node)}")
  }

  override def onMessage(message: Message) {
    OpenComputers.log.info(s"[NETWORK DEBUGGER] Received message: ${messageInfo(message)}.")
  }

  def reconnect(nodes: Array[Node]) {
    node.remove()
    api.Network.joinNewNetwork(node)
    for (node <- nodes if node != null) {
      this.node.connect(node)
    }
  }

  private def nodeInfo(node: Node) = s"{address = ${node.address}, reachability = ${node.reachability.name}" + (node match {
    case componentConnector: ComponentConnector => componentInfo(componentConnector) + connectorInfo(componentConnector)
    case component: Component => componentInfo(component)
    case connector: Connector => connectorInfo(connector)
    case _ =>
  }) + "}"

  private def componentInfo(component: Component) = s", type = component, name = ${component.name}, visibility = ${component.visibility.name}"

  private def connectorInfo(connector: Connector) = s", type = connector, buffer = ${connector.localBuffer}, bufferSize = ${connector.localBufferSize}"

  private def messageInfo(message: Message) = s"{name = ${message.name()}, source = ${nodeInfo(message.source)}, data = [${message.data.mkString(", ")}]}"
}
