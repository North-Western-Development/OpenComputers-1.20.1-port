package li.cil.oc.server.agent

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.NetworkManager
import net.minecraft.network.IPacket
import net.minecraft.network.PacketDirection
import net.minecraft.network.protocol.Packet

object FakeNetworkManager extends NetworkManager(PacketDirection.CLIENTBOUND) {
  override def send(packetIn: Packet[_]): Unit = {}

  override def send(packetIn: Packet[_], listener: GenericFutureListener[_ <: Future[_ >: Void]]): Unit = {}
}
