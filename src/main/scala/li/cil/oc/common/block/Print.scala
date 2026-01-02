package li.cil.oc.common.block

import li.cil.oc.{Localization, Settings}
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.Tooltip
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.{Component, TextComponent}
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.{InteractionHand, InteractionResult}
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.{BlockGetter, Level}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.{CollisionContext, VoxelShape}
import net.minecraftforge.common.extensions.IForgeBlock

import java.util
import java.util.Random
import scala.collection.convert.ImplicitConversionsToJava._

class Print(props: Properties) extends RedstoneAware(props) with IForgeBlock {
  @Deprecated
  override def propagatesSkylightDown(state: BlockState, world: BlockGetter, pos: BlockPos) = false

  // ----------------------------------------------------------------------- //

  override protected def tooltipBody(stack: ItemStack, world: BlockGetter, tooltip: util.List[Component], advanced: TooltipFlag) = {
    super.tooltipBody(stack, world, tooltip, advanced)
    val data = new PrintData(stack)
    data.tooltip.foreach(s => tooltip.addAll(s.linesIterator.map(new TextComponent(_).setStyle(Tooltip.DefaultStyle)).toIterable))
  }

  override protected def tooltipTail(stack: ItemStack, world: BlockGetter, tooltip: util.List[Component], advanced: TooltipFlag) = {
    super.tooltipTail(stack, world, tooltip, advanced)
    val data = new PrintData(stack)
    if (data.isBeaconBase) {
      tooltip.add(new TextComponent(Localization.Tooltip.PrintBeaconBase).setStyle(Tooltip.DefaultStyle))
    }
    if (data.emitRedstone) {
      tooltip.add(new TextComponent(Localization.Tooltip.PrintRedstoneLevel(data.redstoneLevel)).setStyle(Tooltip.DefaultStyle))
    }
    if (data.emitLight) {
      tooltip.add(new TextComponent(Localization.Tooltip.PrintLightValue(data.lightLevel)).setStyle(Tooltip.DefaultStyle))
    }
  }

  override def getLightEmission(state: BlockState, world: BlockGetter, pos: BlockPos): Int =
    world match {
      case world: Level if world.isLoaded(pos) => world.getBlockEntity(pos) match {
        case print: tileentity.Print => print.data.lightLevel
        case _ => super.getLightEmission(state, world, pos)
      }
      case _ => super.getLightEmission(state, world, pos)
    }

  @Deprecated
  override def getLightBlock(state: BlockState, world: BlockGetter, pos: BlockPos): Int =
    world match {
      case world: Level if world.isLoaded(pos) => world.getBlockEntity(pos) match {
        case print: tileentity.Print if Settings.get.printsHaveOpacity => (print.data.opacity * 4).toInt
        case _ => super.getLightBlock(state, world, pos)
      }
      case _ => super.getLightBlock(state, world, pos)
    }

//  override def getPickBlock(state: BlockState, target: HitResult, world: BlockGetter, pos: BlockPos, player: Player): ItemStack = {
//    world.getBlockEntity(pos) match {
//      case print: tileentity.Print => print.data.createItemStack()
//      case _ => ItemStack.EMPTY
//    }
//  }

  override def getShape(state: BlockState, world: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => print.shape
      case _ => super.getShape(state, world, pos, ctx)
    }
  }

  def tickRate(world: Level) = 20

  override def tick(state: BlockState, world: ServerLevel, pos: BlockPos, rand: Random): Unit = {
    if (!world.isClientSide) world.getBlockEntity(pos) match {
      case print: tileentity.Print =>
        if (print.state) print.toggleState()
      case _ =>
    }
  }

  @Deprecated
  def isBeaconBase(world: BlockGetter, pos: BlockPos, beacon: BlockPos): Boolean = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => print.data.isBeaconBase
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new tileentity.Print(tileentity.BlockEntityTypes.PRINT, pos, state)

  // ----------------------------------------------------------------------- //

  override def use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: InteractionHand, trace: BlockHitResult): InteractionResult = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => if (print.activate()) InteractionResult.sidedSuccess(world.isClientSide) else InteractionResult.PASS
      case _ => super.use(state, world, pos, player, hand, trace)
    }
  }

  override def onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print if print.data.emitRedstone(print.state) =>
        world.updateNeighborsAt(pos, this)
        for (side <- Direction.values) {
          world.updateNeighborsAt(pos.relative(side), this)
        }
      case _ =>
    }
    super.onRemove(state, world, pos, newState, moved)
  }

  override def setPlacedBy(world: Level, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case tileEntity: tileentity.Print => {
        tileEntity.data.loadData(stack)
        tileEntity.updateShape()
        tileEntity.updateRedstone()
        tileEntity.getLevel.getLightEngine.checkBlock(tileEntity.getBlockPos)
      }
      case _ =>
    }
  }

  override def getDrops(state: BlockState, ctx: LootContext.Builder): util.List[ItemStack] = {
    val newCtx = ctx.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, (c, f) => {
      c.getParamOrNull(LootContextParams.BLOCK_ENTITY) match {
        case tileEntity: tileentity.Print => f.accept(tileEntity.data.createItemStack())
        case _ =>
      }
    })
    super.getDrops(state, newCtx)
  }
}
