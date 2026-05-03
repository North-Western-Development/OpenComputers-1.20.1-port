package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.PacketSender
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.{BlockGetter, Level}
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.{BlockState, StateDefinition}

class Charger(props: Properties) extends RedstoneAware(props) with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  protected override def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.chargerRate

  override def openGui(player: ServerPlayer, world: Level, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Charger => ContainerTypes.openChargerGui(player, te)
    case _ =>
  }

  override def newBlockEntity(pos:BlockPos, state: BlockState) = new tileentity.Charger(tileentity.BlockEntityTypes.CHARGER.get(), pos, state)

  override def getTicker[T <: BlockEntity](level: Level, blockState: BlockState, blockEntityType: BlockEntityType[T]): BlockEntityTicker[T] = {
    (_: Level, pos: BlockPos, state: BlockState, entity: T) =>
      entity match {
        case tileEntity: tileentity.Charger => tileEntity.updateEntity()
        case _ =>
      }
  }

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(state: BlockState, world: BlockGetter, pos: BlockPos, side: Direction): Boolean = true

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: Level, pos: BlockPos, player: Player, hand: InteractionHand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) =
    if (Wrench.holdsApplicableWrench(player, pos)) world.getBlockEntity(pos) match {
      case charger: tileentity.Charger =>
        if (!world.isClientSide) {
          charger.invertSignal = !charger.invertSignal
          charger.chargeSpeed = 1.0 - charger.chargeSpeed
          PacketSender.sendChargerState(charger)
          Wrench.wrenchUsed(player, pos)
        }
        true
      case _ => false
    }
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)

  @Deprecated
  override def neighborChanged(state: BlockState, world: Level, pos: BlockPos, block: Block, fromPos: BlockPos, b: Boolean): Unit = {
    world.getBlockEntity(pos) match {
      case charger: tileentity.Charger => charger.onNeighborChanged()
      case _ =>
    }
    super.neighborChanged(state, world, pos, block, fromPos, b)
  }
}
