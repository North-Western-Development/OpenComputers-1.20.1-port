package li.cil.oc.server.component.traits

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.util.{BlockInventorySource, BlockPosition, EntityInventorySource, InventorySource}
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedLevel._
import net.minecraft.block.FlowingFluidBlock
import net.minecraft.world.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.item.minecart.MinecartEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.util.{ActionResult, Direction, Hand}
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.world.server.ServerLevel
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.eventbus.api.Event.Result
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.InvWrapper

import scala.collection.convert.ImplicitConversionsToScala._

trait LevelAware {
  def position: BlockPosition

  def world = position.world.get

  def fakePlayer: Player = {
    val player = FakePlayerFactory.get(world.asInstanceOf[ServerLevel], Settings.get.fakePlayerProfile)
    player.setPos(position.x + 0.5, position.y + 0.5, position.z + 0.5)
    player
  }

  private def mayInteract(blockPos: BlockPosition, face: Direction): Boolean = {
    try {
      val trace = new BlockRayTraceResult(fakePlayer.position, face, blockPos.toBlockPos, false)
      val event = new PlayerInteractEvent.RightClickBlock(fakePlayer, Hand.MAIN_HAND, blockPos.toBlockPos, trace)
      MinecraftForge.EVENT_BUS.post(event)
      !event.isCanceled && event.getUseBlock != Result.DENY
    } catch {
      case t: Throwable =>
        OpenComputers.log.warn("Some event handler threw up while checking for permission to access a block.", t)
        true
    }
  }

  private def mayInteract(entity: Entity): Boolean = {
    try {
      val event = new PlayerInteractEvent.EntityInteract(fakePlayer, Hand.MAIN_HAND, entity)
      MinecraftForge.EVENT_BUS.post(event)
      !event.isCanceled
    } catch {
      case t: Throwable =>
        OpenComputers.log.warn("Some event handler threw up while checking for permission to access an entity.", t)
        true
    }
  }

  def mayInteract(inv: InventorySource): Boolean = (inv.inventory match {
    case inv: InvWrapper if inv.getInv != null => inv.getInv.stillValid(fakePlayer)
    case _ => true
  }) && (inv match {
    case inv: BlockInventorySource => mayInteract(inv.position, inv.side)
    case inv: EntityInventorySource => mayInteract(inv.entity)
    case _ => true
  })

  def entitiesInBounds[Type <: Entity](clazz: Class[Type], bounds: AxisAlignedBB) = {
    world.getEntitiesOfClass(clazz, bounds)
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition) = {
    entitiesInBounds(clazz, blockPos.bounds)
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: Direction) = {
    entitiesInBlock(clazz, position.offset(side))
  }

  def closestEntity[Type <: Entity](clazz: Class[Type], side: Direction) = {
    val blockPos = position.offset(side)
    val candidates = world.getEntitiesOfClass(clazz, blockPos.bounds, null)
    if (!candidates.isEmpty) Some(candidates.minBy(e => fakePlayer.distanceToSqr(e))) else None
  }

  def blockContent(side: Direction) = {
    closestEntity[Entity](classOf[Entity], side) match {
      case Some(_@(_: LivingEntity | _: MinecartEntity)) =>
        (true, "entity")
      case _ =>
        val blockPos = position.offset(side)
        val state = world.getBlockState(blockPos.toBlockPos)
        val block = state.getBlock
        if (block.isAir(state, world, blockPos.toBlockPos)) {
          (false, "air")
        }
        else if (block.isInstanceOf[FlowingFluidBlock] || block.isInstanceOf[IFluidBlock]) {
          val event = new BlockEvent.BreakEvent(world, blockPos.toBlockPos, state, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "liquid")
        }
        else if (block.isReplaceable(blockPos)) {
          val event = new BlockEvent.BreakEvent(world, blockPos.toBlockPos, state, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "replaceable")
        }
        else if (state.getCollisionShape(world, blockPos.toBlockPos, ISelectionContext.empty).isEmpty) {
          (true, "passable")
        }
        else {
          (true, "solid")
        }
    }
  }
}
