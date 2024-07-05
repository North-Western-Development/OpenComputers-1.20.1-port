package li.cil.oc.common.block

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.Colored
import li.cil.oc.common.tileentity.traits.Inventory
import li.cil.oc.common.tileentity.traits.Rotatable
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedLevel._
import li.cil.oc.util.Tooltip
import net.minecraft.block.AbstractBlock.Properties
net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BlockRenderType
import net.minecraft.block.ContainerBlock
import net.minecraft.world.level.material.Material
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemGroup
import net.minecraft.world.item.ItemStack
import net.minecraft.loot.LootContext
import net.minecraft.loot.LootParameters
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.util.ActionResultType
import net.minecraft.core.Direction
import net.minecraft.util.Hand
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.ILevelReader
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.ToolType

import scala.collection.convert.ImplicitConversionsToScala._

abstract class SimpleBlock(props: Properties) extends ContainerBlock(props) {
  @Deprecated
  private var unlocalizedName = super.getDescriptionId()

  @Deprecated
  private[oc] def setUnlocalizedName(name: String): Unit = unlocalizedName = "tile." + name

  @Deprecated
  override def getDescriptionId = unlocalizedName

  protected val validRotations_ = Array(Direction.UP, Direction.DOWN)

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def newBlockEntity(world: BlockGetter): BlockEntity = null

  override def getRenderShape(state: BlockState): BlockRenderType = BlockRenderType.MODEL

  // ----------------------------------------------------------------------- //
  // BlockItem
  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: BlockGetter, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    tooltipHead(stack, world, tooltip, flag)
    tooltipBody(stack, world, tooltip, flag)
    tooltipTail(stack, world, tooltip, flag)
  }

  protected def tooltipHead(stack: ItemStack, world: BlockGetter, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
  }

  protected def tooltipBody(stack: ItemStack, world: BlockGetter, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase)) {
      tooltip.add(new StringTextComponent(curr).setStyle(Tooltip.DefaultStyle))
    }
  }

  protected def tooltipTail(stack: ItemStack, world: BlockGetter, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
  }

  // ----------------------------------------------------------------------- //
  // Rotation
  // ----------------------------------------------------------------------- //

  def getFacing(world: BlockGetter, pos: BlockPos): Direction =
    world.getBlockEntity(pos) match {
      case tileEntity: Rotatable => tileEntity.facing
      case _ => Direction.SOUTH
    }

  def setFacing(world: Level, pos: BlockPos, value: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromFacing(value); true
      case _ => false
    }

  def setRotationFromEntityPitchAndYaw(world: Level, pos: BlockPos, value: Entity): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromEntityPitchAndYaw(value); true
      case _ => false
    }

  def toLocal(world: BlockGetter, pos: BlockPos, value: Direction): Direction =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.toLocal(value)
      case _ => value
    }

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  override def canHarvestBlock(state: BlockState, world: BlockGetter, pos: BlockPos, player: Player) = true

  override def getHarvestTool(state: BlockState): ToolType = null

  override def canBeReplacedByLeaves(state: BlockState, world: ILevelReader, pos: BlockPos): Boolean = false

  def getValidRotations(world: Level, pos: BlockPos): Array[Direction] = validRotations_

  override def getDrops(state: BlockState, ctx: LootContext.Builder): util.List[ItemStack] = {
    val newCtx = ctx.getOptionalParameter(LootParameters.BLOCK_ENTITY) match {
      case _: Inventory => ctx.withDynamicDrop(LootFunctions.DYN_VOLATILE_CONTENTS, (c, f) => {
          c.getParamOrNull(LootParameters.BLOCK_ENTITY) match {
            case inventory: Inventory => inventory.forAllLoot(f)
            case _ =>
          }
        })
      case _ => ctx
    }
    super.getDrops(state, newCtx)
  }

  override def playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player) {
    if (!world.isClientSide && player.isCreative) world.getBlockEntity(pos) match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }
    super.playerWillDestroy(world, pos, state, player)
  }

  // ----------------------------------------------------------------------- //

  @Deprecated
  def rotateBlock(world: Level, pos: BlockPos, axis: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: tileentity.traits.Rotatable if rotatable.rotate(axis) =>
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        true
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: Hand, trace: BlockHitResult): ActionResultType = {
    val heldItem = player.getItemInHand(hand)
    world.getBlockEntity(pos) match {
      case colored: Colored if Color.isDye(heldItem) =>
        colored.setColor(Color.rgbValues(Color.dyeColor(heldItem)))
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        if (!player.isCreative && colored.consumesDye) {
          heldItem.split(1)
        }
        ActionResultType.sidedSuccess(world.isClientSide)
      case _ => {
        val loc = trace.getLocation
        val pos = trace.getBlockPos
        val x = loc.x.toFloat - pos.getX
        val y = loc.y.toFloat - pos.getY
        val z = loc.z.toFloat - pos.getZ
        if (localOnBlockActivated(world, pos, player, hand, heldItem, trace.getDirection, x, y, z))
          ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
      }
    }
  }

  def localOnBlockActivated(world: Level, pos: BlockPos, player: Player, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = false
}
