package li.cil.oc.util

import java.util.Optional
import java.util.function.Consumer

import li.cil.oc.OpenComputers
import li.cil.oc.util.ExtendedLevel._
import li.cil.oc.util.StackOption._
import net.minecraft.world.entity.Entity
import net.minecraft.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.inventory.ISidedInventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.wrapper.InvWrapper
import net.minecraftforge.items.wrapper.SidedInvWrapper

import scala.collection.convert.ImplicitConversionsToScala._

object InventoryUtils {

  def asItemHandler(inventory: Container, side: Direction): IItemHandlerModifiable = inventory match {
    case inv: ISidedInventory if side != null => new SidedInvWrapper(inv, side)
    case _ => new InvWrapper(inventory)
  }

  def asItemHandler(inventory: Container): IItemHandlerModifiable = asItemHandler(inventory, null)

  /**
   * Check if two item stacks are of equal type, ignoring the stack size.
   * <br>
   * Optionally check for equality in NBT data.
   */
  def haveSameItemType(stackA: ItemStack, stackB: ItemStack, checkNBT: Boolean = false): Boolean =
    !stackA.isEmpty && !stackB.isEmpty &&
      stackA.getItem == stackB.getItem &&
      (stackA.getDamageValue == stackB.getDamageValue) &&
      (!checkNBT || ItemStack.tagMatches(stackA, stackB))

  /**
   * Retrieves an actual inventory implementation for a specified world coordinate,
   * complete with a reference to the source of said implementation.
   */
  def inventorySourceAt(position: BlockPosition, side: Direction): Option[InventorySource] = position.world match {
    case Some(world) if world.blockExists(position) => world.getBlockEntity(position) match {
      case tile: BlockEntity if tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).isPresent => Option(BlockInventorySource(position, side, tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null)))
      case tile: Container => Option(BlockInventorySource(position, side, asItemHandler(tile, side)))
      case _ => world.getEntitiesOfClass(classOf[Entity], position.bounds)
        .filter(e => e.isAlive && e.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).isPresent)
        .map(a => EntityInventorySource(a, side, a.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null)))
        .find(a => a != null && a.inventory != null)
    }
    case _ => None
  }

  /**
   * Retrieves an actual inventory implementation for a specified world coordinate.
   */
  def inventoryAt(position: BlockPosition, side: Direction): Option[IItemHandler] = inventorySourceAt(position, side)
    .map(a => a.inventory)

  def anyInventorySourceAt(position: BlockPosition): Option[InventorySource] = {
    for(side <- null :: Direction.values.toList) {
      inventorySourceAt(position, side) match {
        case inv: Some[InventorySource] => return inv
        case _ =>
      }
    }
    None
  }

  def anyInventoryAt(position: BlockPosition): Option[IItemHandler] = anyInventorySourceAt(position)
    .map(a => a.inventory)

  /**
   * Inserts a stack into an inventory.
   * <br>
   * Only tries to insert into the specified slot. This <em>cannot</em> be
   * used to empty a slot. It can only insert stacks into empty slots and
   * merge additional items into an existing stack in the slot.
   * <br>
   * The passed stack's size will be adjusted to reflect the number of items
   * inserted into the inventory, i.e. if 10 more items could fit into the
   * slot, the stack's size will be 10 smaller than before the call.
   * <br>
   * This will return <tt>true</tt> if <em>at least</em> one item could be
   * inserted into the slot. It will return <tt>false</tt> if the passed
   * stack did not change. Note that it will also change the stack
   * when called with <tt>simulate = true</tt>.
   * <br>
   * This takes care of handling special cases such as sided inventories,
   * maximum inventory and item stack sizes.
   * <br>
   * The number of items inserted can be limited, to avoid unnecessary
   * changes to the inventory the stack may come from, for example.
   */
  def insertIntoInventorySlot(stack: ItemStack, inventory: IItemHandler, slot: Int, limit: Int = 64, simulate: Boolean = false): Boolean =
    (!stack.isEmpty && limit > 0 && stack.getCount > 0) && {
      val amount = stack.getCount min limit
      val toInsert = stack.split(amount)
      inventory.insertItem(slot, toInsert, simulate) match {
        case remaining: ItemStack =>
          val result = remaining.getCount < amount
          stack.grow(remaining.getCount)
          result
        case _ => true
      }
    }

  def insertIntoInventorySlot(stack: ItemStack, inventory: Container, side: Option[Direction], slot: Int, limit: Int, simulate: Boolean): Boolean =
    insertIntoInventorySlot(stack, asItemHandler(inventory, side.orNull), slot, limit, simulate)

  /**
   * Extracts a stack from an inventory.
   * <br>
   * Only tries to extract from the specified slot. This <em>can</em> be used
   * to empty a slot. It will extract items using the specified consumer method
   * which is called with the extracted stack and a simulation flag before the
   * stack in the inventory that we extract from is cleared from. This allows
   * placing back excess items with as few inventory updates as possible.
   * <br>
   * The consumer is the only way to retrieve the actually extracted stack. It
   * is called with a separate stack instance, so it does not have to be copied
   * again.
   * <br>
   * This will return the <tt>number</tt> of items extracted. It will return
   * <tt>zero</tt> if the stack in the slot did not change.
   * <br>
   * This takes care of handling special cases such as sided inventories and
   * maximum stack sizes.
   * <br>
   * The number of items extracted can be limited, to avoid unnecessary
   * changes to the inventory the stack is extracted from. Note that this could
   * also be achieved by a check in the consumer, but it saves some unnecessary
   * code repetition this way.
   */
  def extractFromInventorySlot(consumer: (ItemStack, Boolean) => Unit, inventory: IItemHandler, slot: Int, limit: Int = 64): Int = {
    val stack = inventory.getStackInSlot(slot)
    if (stack.isEmpty || limit <= 0 || stack.getCount <= 0)
      return 0
    var amount = stack.getMaxStackSize min stack.getCount min limit
    inventory.extractItem(slot, amount, true) match {
      case simExtracted: ItemStack =>
        val extracted = simExtracted.copy
        amount = extracted.getCount
        consumer(extracted, true)
        val count = (amount - extracted.getCount) max 0
        if (count > 0) inventory.extractItem(slot, count, false) match {
          case realExtracted: ItemStack if realExtracted.getCount == count => consumer(realExtracted, false)
          case realExtracted =>
            OpenComputers.log.warn("An IItemHandler instance acted differently between simulated and non-simulated extraction. Offender: " + inventory)
            // Attempt inserting the stack anyway, to minimize world-side item loss.
            if (realExtracted != null && !realExtracted.isEmpty) {
              consumer(realExtracted, false)
            }
         }
        count
      case _ => 0
    }
  }

  def extractFromInventorySlot(consumer: (ItemStack, Boolean) => Unit, inventory: Container, side: Direction, slot: Int, limit: Int): Int =
    extractFromInventorySlot(consumer, asItemHandler(inventory, side), slot, limit)

    /**
   * Inserts a stack into an inventory.
   * <br>
   * This will try to fit the stack in any and as many as necessary slots in
   * the inventory. It will first try to merge the stack in stacks already
   * present in the inventory. After that it will try to fit the stack into
   * empty slots in the inventory.
   * <br>
   * This uses the <tt>insertIntoInventorySlot</tt> method, and therefore
   * handles special cases such as sided inventories and stack size limits.
   * <br>
   * This returns <tt>true</tt> if at least one item was inserted. The passed
   * item stack will be adjusted to reflect the number items inserted, by
   * having its size decremented accordingly.
   */
  def insertIntoInventory(stack: ItemStack, inventory: IItemHandler, limit: Int = 64, simulate: Boolean = false, slots: Option[Iterable[Int]] = None): Boolean =
    (!stack.isEmpty && limit > 0 && stack.getCount > 0) && {
      var success = false
      var remaining = limit min stack.getCount
      val range = slots.getOrElse(0 until inventory.getSlots)

      range.forall(slot => {
        val previousCount = stack.getCount
        if (remaining > 0 && insertIntoInventorySlot(stack, inventory, slot, remaining, simulate)) {
          remaining -= previousCount - stack.getCount
          success = true
        }
        remaining > 0
      })

      success
    }

  def insertIntoInventory(stack: ItemStack, inventory: Container, side: Option[Direction], limit: Int, simulate: Boolean, slots: Option[Iterable[Int]]): Boolean =
    insertIntoInventory(stack, asItemHandler(inventory, side.orNull), limit, simulate, slots)

  /**
   * Extracts a slot from an inventory.
   * <br>
   * This will try to extract a stack from any inventory slot. It will iterate
   * all slots until an item can be extracted from a slot.
   * <br>
   * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
   * handles special cases such as sided inventories and stack size limits.
   * <br>
   * This returns <tt>true</tt> if at least one item was extracted.
   */
  def extractAnyFromInventory(consumer: (ItemStack, Boolean) => Unit, inventory: IItemHandler, limit: Int = 64): Int = {
    for (slot <- 0 until inventory.getSlots) {
      val extracted = extractFromInventorySlot(consumer, inventory, slot, limit)
      if (extracted > 0)
        return extracted
    }
    0
  }

  def extractAnyFromInventory(consumer: (ItemStack, Boolean) => Unit, inventory: Container, side: Direction, limit: Int): Int =
    extractAnyFromInventory(consumer, asItemHandler(inventory, side), limit)

  /**
   * Extracts an item stack from an inventory.
   * <br>
   * This will try to remove items of the same type as the specified item stack
   * up to the number of the stack's size for all slots in the specified inventory.
   * If exact is true, the items colated will also match meta data
   * <br>
   * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
   * handles special cases such as sided inventories and stack size limits.
   */
  def extractFromInventory(stack: ItemStack, inventory: IItemHandler, simulate: Boolean = false, exact: Boolean = true): ItemStack = {
    val remaining = stack.copy()
    for (slot <- 0 until inventory.getSlots if remaining.getCount > 0) {
      extractFromInventorySlot((stackInInv, simulateInsert) => {
        if (stackInInv != null && remaining.getItem == stackInInv.getItem && (!exact || haveSameItemType(remaining, stackInInv, checkNBT = true))) {
          val transferred = stackInInv.getCount min remaining.getCount
          if(!simulateInsert) {
            remaining.shrink(transferred)
          }
          if (simulateInsert || !simulate) {
            stackInInv.shrink(transferred)
          }
        }
      }, inventory, slot, limit = remaining.getCount)
    }
    remaining
  }

  def extractFromInventory(stack: ItemStack, inventory: Container, side: Direction, simulate: Boolean, exact: Boolean): ItemStack =
    extractFromInventory(stack, asItemHandler(inventory, side), simulate, exact)

    /**
   * Utility method for calling <tt>insertIntoInventory</tt> on an inventory
   * in the world.
   */
  def insertIntoInventoryAt(stack: ItemStack, position: BlockPosition, side: Option[Direction] = None, limit: Int = 64, simulate: Boolean = false): Boolean =
    inventoryAt(position, side.orNull).exists(insertIntoInventory(stack, _, limit, simulate))

  type Extractor = () => Int

  /**
   * Utility method for calling <tt>extractFromInventory</tt> on an inventory
   * in the world.
   */
  def getExtractorFromInventoryAt(consumer: (ItemStack, Boolean) => Unit, position: BlockPosition, side: Direction, limit: Int = 64): Extractor =
    inventoryAt(position, side) match {
      case Some(inventory) => () => extractAnyFromInventory(consumer, inventory, limit)
      case _ => null
    }

  /**
   * Transfers some items between two inventories.
   * <br>
   * This will try to extract up the specified number of items from any inventory,
   * then insert it into the specified sink inventory. If the insertion fails, the
   * items will remain in the source inventory.
   * <br>
   * This uses the <tt>extractFromInventory</tt> and <tt>insertIntoInventory</tt>
   * methods, and therefore handles special cases such as sided inventories and
   * stack size limits.
   * <br>
   * This returns <tt>true</tt> if at least one item was transferred.
   */
  def transferBetweenInventories(source: IItemHandler, sink: IItemHandler, limit: Int = 64): Int =
    extractAnyFromInventory(
      insertIntoInventory(_, sink, limit, _), source, limit = limit)

  def transferBetweenInventories(source: Container, sourceSide: Direction, sink: Container, sinkSide: Option[Direction], limit: Int): Int =
    transferBetweenInventories(asItemHandler(source, sourceSide), asItemHandler(sink, sinkSide.orNull), limit)

  /**
   * Like <tt>transferBetweenInventories</tt> but moving between specific slots.
   */
  def transferBetweenInventoriesSlots(source: IItemHandler, sourceSlot: Int, sink: IItemHandler, sinkSlot: Option[Int], limit: Int = 64): Int =
    sinkSlot match {
      case Some(explicitSinkSlot) =>
        extractFromInventorySlot(
          insertIntoInventorySlot(_, sink, explicitSinkSlot, limit, _), source, sourceSlot, limit = limit)
      case _ =>
        extractFromInventorySlot(
          insertIntoInventory(_, sink, limit, _), source, sourceSlot, limit = limit)
    }

  def transferBetweenInventoriesSlots(source: Container, sourceSide: Direction, sourceSlot: Int, sink: Container, sinkSide: Option[Direction], sinkSlot: Option[Int], limit: Int): Int =
    transferBetweenInventoriesSlots(asItemHandler(source, sourceSide), sourceSlot, asItemHandler(sink, sinkSide.orNull), sinkSlot, limit)

  /**
   * Utility method for calling <tt>transferBetweenInventories</tt> on inventories
   * in the world.
   */
  def getTransferBetweenInventoriesAt(source: BlockPosition, sourceSide: Direction, sink: BlockPosition, sinkSide: Option[Direction], limit: Int = 64): Extractor =
    inventoryAt(source, sourceSide) match {
      case Some(sourceInventory) =>
        inventoryAt(sink, sinkSide.orNull) match {
          case Some(sinkInventory) => () => transferBetweenInventories(sourceInventory, sinkInventory, limit)
          case _ => null
        }
      case _ => null
    }

  /**
   * Utility method for calling <tt>transferBetweenInventoriesSlots</tt> on inventories
   * in the world.
   */
  def getTransferBetweenInventoriesSlotsAt(sourcePos: BlockPosition, sourceSide: Direction, sourceSlot: Int, sinkPos: BlockPosition, sinkSide: Option[Direction], sinkSlot: Option[Int], limit: Int = 64): Extractor =
    inventoryAt(sourcePos, sourceSide) match {
      case Some(sourceInventory) =>
        inventoryAt(sinkPos, sinkSide.orNull) match {
          case Some(sinkInventory) => () => transferBetweenInventoriesSlots(sourceInventory, sourceSlot, sinkInventory, sinkSlot, limit)
          case _ => null
        }
      case _ => null
    }

  /**
   * Utility method mirroring dropAllSlots but instead piping slots into
   * a provided consumer for use with LootContext.
   */
  def forAllSlots(inventory: Container, dst: Consumer[ItemStack]): Unit = {
    for (slot <- 0 until inventory.getContainerSize) {
      StackOption(inventory.getItem(slot)) match {
        case SomeStack(stack) if stack.getCount > 0 => dst.accept(stack)
        case _ => // Nothing.
      }
    }
  }

  /**
   * Utility method for dropping contents from a single inventory slot into
   * the world.
   */
  def dropSlot(position: BlockPosition, inventory: Container, slot: Int, count: Int, direction: Option[Direction] = None): Boolean = {
    StackOption(inventory.removeItem(slot, count)) match {
      case SomeStack(stack) if stack.getCount > 0 => spawnStackInLevel(position, stack, direction); true
      case _ => false
    }
  }

  /**
   * Utility method for dumping all inventory contents into the world.
   */
  def dropAllSlots(position: BlockPosition, inventory: Container): Unit = {
    for (slot <- 0 until inventory.getContainerSize) {
      StackOption(inventory.getItem(slot)) match {
        case SomeStack(stack) if stack.getCount > 0 =>
          inventory.setItem(slot, ItemStack.EMPTY)
          spawnStackInLevel(position, stack)
        case _ => // Nothing.
      }
    }
  }

  /**
   * Try inserting an item stack into a player inventory. If that fails, drop it into the world.
   */
  def addToPlayerInventory(stack: ItemStack, player: Player, spawnInLevel: Boolean = true): Unit = {
    if (!stack.isEmpty) {
      if (player.inventory.add(stack)) {
        player.inventory.setChanged()
        if (player.containerMenu != null) {
          player.containerMenu.broadcastChanges()
        }
      }
      if (stack.getCount > 0 && spawnInLevel) {
        player.drop(stack, false, false)
      }
    }
  }

  /**
   * Utility method for spawning an item stack in the world.
   */
  def spawnStackInLevel(position: BlockPosition, stack: ItemStack, direction: Option[Direction] = None, validator: Option[ItemEntity => Boolean] = None): ItemEntity = position.world match {
    case Some(world) if !stack.isEmpty && stack.getCount > 0 =>
      val rng = world.random
      val (ox, oy, oz) = direction.fold((0, 0, 0))(d => (d.getStepX, d.getStepY, d.getStepZ))
      val (tx, ty, tz) = (
        0.1 * (rng.nextDouble - 0.5) + ox * 0.65,
        0.1 * (rng.nextDouble - 0.5) + oy * 0.75 + (ox + oz) * 0.25,
        0.1 * (rng.nextDouble - 0.5) + oz * 0.65)
      val dropPos = position.offset(0.5 + tx, 0.5 + ty, 0.5 + tz)
      val entity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack.copy())
      entity.setDeltaMovement(new Vec3(
        0.0125 * (rng.nextDouble - 0.5) + ox * 0.03,
        0.0125 * (rng.nextDouble - 0.5) + oy * 0.08 + (ox + oz) * 0.03,
        0.0125 * (rng.nextDouble - 0.5) + oz * 0.03))
      if (validator.fold(true)(_ (entity))) {
        entity.setPickUpDelay(15)
        world.addFreshEntity(entity)
        entity
      }
      else null
    case _ => null
  }
}

sealed trait InventorySource {
  def side: Direction
  def inventory: IItemHandler
}
final case class BlockInventorySource(position: BlockPosition, side: Direction, inventory: IItemHandler) extends InventorySource
final case class EntityInventorySource(entity: Entity, side: Direction, inventory: IItemHandler) extends InventorySource