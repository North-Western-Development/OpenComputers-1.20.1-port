package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.client.gui
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.util.RotationHelper
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraft.state.StateContainer
import net.minecraft.util.ActionResultType
import net.minecraft.core.Direction
import net.minecraft.util.Hand
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.{BlockGetter, Level}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}

class Waypoint(props: Properties) extends RedstoneAware(props) {
  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw)

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: BlockGetter) = new tileentity.Waypoint(tileentity.BlockEntityTypes.WAYPOINT)

  // ----------------------------------------------------------------------- //

  override def use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: Hand, trace: BlockHitResult): ActionResultType = {
    if (!player.isCrouching) {
      if (world.isClientSide) world.getBlockEntity(pos) match {
        case t: tileentity.Waypoint => showGui(t)
        case _ =>
      }
      ActionResultType.sidedSuccess(world.isClientSide)
    }
    else super.use(state, world, pos, player, hand, trace)
  }

  @OnlyIn(Dist.CLIENT)
  private def showGui(t: tileentity.Waypoint) {
    Minecraft.getInstance.pushGuiLayer(new gui.Waypoint(t))
  }

  override def getValidRotations(world: Level, pos: BlockPos): Array[Direction] =
    world.getBlockEntity(pos) match {
      case waypoint: tileentity.Waypoint =>
        Direction.values.filter {
          d => d != waypoint.facing && d != waypoint.facing.getOpposite
        }
      case _ => super.getValidRotations(world, pos)
    }
}
