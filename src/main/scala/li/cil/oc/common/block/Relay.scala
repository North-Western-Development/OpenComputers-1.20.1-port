package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.server.level.ServerPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

class Relay(props: Properties) extends SimpleBlock(props) with traits.GUI with traits.PowerAcceptor {
  override def openGui(player: ServerPlayer, world: Level, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Relay => ContainerTypes.openRelayGui(player, te)
    case _ =>
  }

  override def energyThroughput = Settings.get.accessPointRate

  override def newBlockEntity(world: BlockGetter) = new tileentity.Relay(tileentity.BlockEntityTypes.RELAY)
}
