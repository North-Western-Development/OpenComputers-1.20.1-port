package li.cil.oc.common.block

import li.cil.oc.client.KeyMappings
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.item.data.RaidData
import li.cil.oc.common.tileentity
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.Tooltip
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.{BlockGetter, Level}
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.{BlockState, StateDefinition}
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraftforge.common.extensions.IForgeBlock

import java.util

class Raid(props: Properties) extends SimpleBlock(props) with IForgeBlock with traits.GUI {

  protected override def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  override protected def tooltipTail(stack: ItemStack, world: BlockGetter, tooltip: util.List[Component], advanced: TooltipFlag) {
    super.tooltipTail(stack, world, tooltip, advanced)
    if (KeyMappings.showExtendedTooltips) {
      val data = new RaidData(stack)
      for (disk <- data.disks if !disk.isEmpty) {
        tooltip.add(Component.literal("- " + disk.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def openGui(player: ServerPlayer, world: Level, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Raid => ContainerTypes.openRaidGui(player, te)
    case _ =>
  }

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new tileentity.Raid(tileentity.TileEntityTypes.RAID)

  // ----------------------------------------------------------------------- //

  override def hasAnalogOutputSignal(state: BlockState): Boolean = true

  override def getAnalogOutputSignal(state: BlockState, world: Level, pos: BlockPos): Int =
    world.getBlockEntity(pos) match {
      case raid: tileentity.Raid if raid.presence.forall(ok => ok) => 15
      case _ => 0
    }

  override def setPlacedBy(world: Level, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case tileEntity: tileentity.Raid if !world.isClientSide => {
        val data = new RaidData(stack)
        for (i <- 0 until math.min(data.disks.length, tileEntity.getContainerSize)) {
          tileEntity.setItem(i, data.disks(i))
        }
        data.label.foreach(tileEntity.label.setLabel)
        if (!data.filesystem.isEmpty) {
          tileEntity.tryCreateRaid(data.filesystem.getCompound("node").getString("address"))
          tileEntity.filesystem.foreach(_.loadData(data.filesystem))
        }
      }
      case _ =>
    }
  }

  override def getDrops(state: BlockState, ctx: LootParams.Builder): util.List[ItemStack] = {
    val newCtx = ctx.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, (c, f) => {
      c.getParamOrNull(LootContextParams.BLOCK_ENTITY) match {
        case tileEntity: tileentity.Raid => {
          val stack = createItemStack()
          if (tileEntity.items.exists(!_.isEmpty)) {
            val data = new RaidData()
            data.disks = tileEntity.items.clone()
            tileEntity.filesystem.foreach(_.saveData(data.filesystem))
            data.label = Option(tileEntity.label.getLabel)
            data.saveData(stack)
          }
          f.accept(stack)
        }
        case _ =>
      }
    })
    super.getDrops(state, newCtx)
  }

  override def playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player) {
    if (!world.isClientSide && player.isCreative) {
      world.getBlockEntity(pos) match {
        case tileEntity: tileentity.Raid if tileEntity.items.exists(!_.isEmpty) =>
          Block.dropResources(state, world, pos, tileEntity, player, player.getMainHandItem)
        case _ =>
      }
    }
    super.playerWillDestroy(world, pos, state, player)
  }
}
