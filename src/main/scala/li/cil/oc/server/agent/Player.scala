package li.cil.oc.server.agent

import java.util
import java.util.UUID
import com.mojang.datafixers.util.Either
import com.mojang.authlib.GameProfile
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common.EventHandler
import li.cil.oc.server.agent.{Inventory => AgentInventory}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.PistonBlock
import net.minecraft.world.entity.{Entity, EntityDimensions, EquipmentSlot, LivingEntity, Pose}
import net.minecraft.entity.Pose
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.entity.merchant.IMerchant
import net.minecraft.world.entity.player
import net.minecraft.world.entity.player.Player.{BedSleepingProblem, SleepResult}
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.{BlockItem, ItemStack, ItemUseContext, Items}
import net.minecraft.inventory._
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.inventory.container.PlayerContainer
import net.minecraft.item.BlockItem
import net.minecraft.item.MerchantOffers
import net.minecraft.network.protocol.game.{ServerPlayNetHandler, ServerboundInteractPacket, ServerboundPlayerActionPacket}
import net.minecraft.potion.EffectInstance
import net.minecraft.server.management.{OpEntry, PlayerInteractionManager}
import net.minecraft.tileentity._
import net.minecraft.world.{Container, InteractionHand, InteractionResult, MenuProvider}
import net.minecraft.util.DamageSource
import net.minecraft.core.{BlockPos, Direction, NonNullList}
import net.minecraft.world.phys.{BlockHitResult, Vec3}
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component
import net.minecraft.world.level.{BaseCommandBlock, Level}
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.players.{ServerOpList, ServerOpListEntry}
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity.RemovalReason
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.block.entity.{CommandBlockEntity, SignBlockEntity}
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.{Event, EventPriority, SubscribeEvent}
import net.minecraftforge.fml.util.ObfuscationReflectionHelper
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper._
import org.joml.Vector3d

import scala.jdk.CollectionConverters._

object Player {
  // These use unobfuscated names because they're added by forge (LazyOptional / capabilities).
  private val playerMainHandler = ObfuscationReflectionHelper.findField(classOf[Player], "playerMainHandler")

  private val playerEquipmentHandler = ObfuscationReflectionHelper.findField(classOf[Player], "playerEquipmentHandler")

  private val playerJoinedHandler = ObfuscationReflectionHelper.findField(classOf[Player], "playerJoinedHandler")

  def profileFor(agent: internal.Agent): GameProfile = {
    val uuid = agent.ownerUUID
    val randomId = (agent.world.random.nextInt(0xFFFFFF) + 1).toString
    val name = Settings.get.nameFormat.
      replace("$player$", agent.ownerName).
      replace("$random$", randomId)
    new GameProfile(uuid, name)
  }

  def determineUUID(playerUUID: Option[UUID] = None): UUID = {
    val format = Settings.get.uuidFormat
    val randomUUID = UUID.randomUUID()
    try UUID.fromString(format.
      replaceAllLiterally("$random$", randomUUID.toString).
      replaceAllLiterally("$player$", playerUUID.getOrElse(randomUUID).toString)) catch {
      case t: Throwable =>
        OpenComputers.log.warn("Failed determining robot UUID, check your config's `uuidFormat` entry!", t)
        randomUUID
    }
  }

  def updatePositionAndRotation(player: Player, facing: Direction, side: Direction) {
    player.facing = facing
    player.side = side
    val direction = new Vector3d(
      facing.getStepX + side.getStepX,
      facing.getStepY + side.getStepY,
      facing.getStepZ + side.getStepZ).normalize()
    val yaw = Math.toDegrees(-Math.atan2(direction.x, direction.z)).toFloat
    val pitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt((direction.x * direction.x) + (direction.z * direction.z)))).toFloat * 0.99f
    player.setPos(player.agent.xPosition, player.agent.yPosition, player.agent.zPosition)
    player.setXRot(pitch % 360f)
    player.setXRot(yaw % 360f)
    player.xRotO = player.getXRot
    player.yRotO = player.getYRot
  }

  def setPlayerInventoryItems(player: Player): Unit = {
    // the offhand is simply the agent's tool item
    val agent = player.agent
    def setCopyOrNull(inv: NonNullList[ItemStack], agentInv: Container, slot: Int): Unit = {
      val item = agentInv.getItem(slot)
      inv.set(slot, if (item != null) item.copy() else ItemStack.EMPTY)
    }

    for (i <- 0 until 4) {
      setCopyOrNull(player.getInventory.armor, agent.equipmentInventory, i)
    }

    // items is 36 items
    // the agent inventory is 100 items with some space for components
    // leaving us 88..we'll copy what we can
    val size = player.getInventory.items.size min agent.mainInventory.getContainerSize
    for (i <- 0 until size) {
      setCopyOrNull(player.getInventory.items, agent.mainInventory, i)
    }
    player.inventoryMenu.broadcastChanges()
  }

  def detectPlayerInventoryChanges(player: Player): Unit = {
  	val agent = player.agent
    player.inventoryMenu.broadcastChanges()
    // The follow code will set agent.inventories = FakePlayer's inv.stack
    def setCopy(inv: Container, index: Int, item: ItemStack): Unit = {
      val result = if (item != null) item.copy else ItemStack.EMPTY
      val current = inv.getItem(index)
      if (!ItemStack.matches(result, current)) {
        inv.setItem(index, result)
      }
    }
    for (i <- 0 until 4) {
      setCopy(agent.equipmentInventory(), i, player.getInventory.armor.get(i))
    }
    val size = player.getInventory.items.size min agent.mainInventory.getContainerSize
    for (i <- 0 until size) {
      setCopy(agent.mainInventory, i, player.getInventory.items.get(i))
    }
  }
}

class Player(val agent: internal.Agent) extends FakePlayer(agent.world.asInstanceOf[ServerLevel], Player.profileFor(agent)) {
  connection= new ServerPlayNetHandler(server, FakeNetworkManager, this)

  getAbilities.mayfly = true
  getAbilities.invulnerable = true
  getAbilities.flying = true
  setOnGround(true)

  override def getMyRidingOffset = 0.5

  override def getStandingEyeHeight(pose: Pose, size: EntityDimensions) = 0f

  override def getDimensions(pose: Pose) = new EntityDimensions(1, 1, true)
  refreshDimensions()

  {
    this.inventory = new AgentInventory(this, agent)
    // because the inventory was just overwritten, the container is now detached
    this.inventoryMenu = new InventoryMenu(inventory, !level.isClientSide, this)
    this.containerMenu = this.inventoryMenu

    try {
      Player.playerMainHandler.set(this, LazyOptional.of(new NonNullSupplier[IItemHandler] {
        override def get = new PlayerMainInvWrapper(getInventory)
      }))
      Player.playerEquipmentHandler.set(this, LazyOptional.of(new NonNullSupplier[IItemHandler] {
        override def get = new CombinedInvWrapper(new PlayerArmorInvWrapper(getInventory), new PlayerOffhandInvWrapper(inventory))
      }))
      Player.playerJoinedHandler.set(this, LazyOptional.of(new NonNullSupplier[IItemHandler] {
        override def get = new PlayerInvWrapper(getInventory)
      }))
    } catch {
      case _: Exception =>
    }
  }

  var facing, side = Direction.SOUTH

  override def getName = Component.literal(agent.name)

  // ----------------------------------------------------------------------- //

  def closestEntity[Type <: Entity](clazz: Class[Type], side: Direction = facing): Option[Entity] = {
    val bounds = BlockPosition(agent).offset(side).bounds
    val candidates = level.getEntitiesOfClass(clazz, bounds, null)
    if (candidates.isEmpty) return None
    Some(candidates.asScala.minBy(e => distanceToSqr(e)))
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: Direction): util.List[Type] = {
    entitiesInBlock(clazz, BlockPosition(agent).offset(side))
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition): util.List[Type] = {
    level.getEntitiesOfClass(clazz, blockPos.bounds, null)
  }

  private def adjacentItems: util.List[ItemEntity] = {
    level.getEntitiesOfClass(classOf[ItemEntity], BlockPosition(agent).bounds.inflate(2, 2, 2), null)
  }

  private def collectDroppedItems(itemsBefore: Iterable[ItemEntity]) {
    val itemsDropped = adjacentItems.asScala --= itemsBefore
    if (itemsDropped.nonEmpty) {
      for (drop <- itemsDropped) {
        drop.setDefaultPickUpDelay()
        drop.playerTouch(this)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def attack(entity: Entity) {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => entity match {
      case player: Player if !canHarmPlayer(player) => // Avoid player damage.
      case _ =>
        val event = new RobotAttackEntityEvent.Pre(agent, entity)
        MinecraftForge.EVENT_BUS.post(event)
        if (!event.isCanceled) {
          super.attack(entity)
          MinecraftForge.EVENT_BUS.post(new RobotAttackEntityEvent.Post(agent, entity))
        }
    })
  }

  override def interactOn(entity: Entity, hand: InteractionHand): InteractionResult = {
    val cancel = try MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(this, hand, entity)) catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
    if(!cancel && callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      val result = isItemUseAllowed(stack) && (entity.interact(this, hand).consumesAction || (entity match {
        case living: LivingEntity if !getItemInHand(InteractionHand.MAIN_HAND).isEmpty => getItemInHand(InteractionHand.MAIN_HAND).interactLivingEntity(this, living, hand).consumesAction
        case _ => false
      }))
      if (!getItemInHand(InteractionHand.MAIN_HAND).isEmpty) {
        if (getItemInHand(InteractionHand.MAIN_HAND).getCount <= 0) {
          val orig = getItemInHand(InteractionHand.MAIN_HAND)
          this.getInventory.setItem(this.getInventory.selected, ItemStack.EMPTY)
          ForgeEventFactory.onPlayerDestroyItem(this, orig, hand)
        } else {
          // because of various hacks for IC2, we expect the in-hand result to be moved to our offhand buffer
          this.getInventory.offhand.set(0, getItemInHand(InteractionHand.MAIN_HAND))
          this.getInventory.setItem(this.getInventory.selected, ItemStack.EMPTY)
        }
      }
      result
    })) InteractionResult.sidedSuccess(level.isClientSide) else InteractionResult.PASS
  }

  def activateBlockOrUseItem(pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, duration: Double): ActivationType.Value = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return ActivationType.None
      }

      val item = if (!stack.isEmpty) stack.getItem else null
      val state = level.getBlockState(pos)
      val traceEndPos = new Vec3(pos.getX + hitX, pos.getY + hitY, pos.getZ + hitZ)
      val traceCtx = if (state.isAir) BlockHitResult.miss(traceEndPos, side, pos) else new BlockHitResult(traceEndPos, side, pos, false)
      if (item != null && item.onItemUseFirst(stack, new UseOnContext(level, this, InteractionHand.OFF_HAND, stack, traceCtx)).consumesAction) {
        return ActivationType.ItemUsed
      }

      val canActivate = !state.isAir && Settings.get.allowActivateBlocks
      val shouldActivate = canActivate && (!isCrouching || (item == null || item.doesSneakBypassUse(stack, level, pos, this)))
      val result =
        if (shouldActivate && state.use(level, this, InteractionHand.OFF_HAND, new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos, false)).consumesAction)
          ActivationType.BlockActivated
        else if (duration <= Double.MinPositiveValue && isItemUseAllowed(stack) && tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ))
          ActivationType.ItemPlaced
        else if (useEquippedItem(duration, Option(stack)))
          ActivationType.ItemUsed
        else
          ActivationType.None

      result
    })
  }

  override def setItemSlot(slotIn: EquipmentSlot, stack: ItemStack): Unit = {
    var superCall: () => Unit = () => super.setItemSlot(slotIn, stack)
    if (slotIn == EquipmentSlot.MAINHAND) {
      agent.equipmentInventory.setItem(0, stack)
      superCall = () => {
        val slot = getInventory.selected
        // So, if we're not in the main inventory, selected is set to -1
        // for compatibility with mods that try accessing the inv directly
        // using inventory.selected. See li.cil.oc.server.agent.Inventory
        if(getInventory.selected < 0) getInventory.selected = ~getInventory.selected
        super.setItemSlot(slotIn, stack)
        getInventory.selected = slot
      }
    } else if(slotIn == EquipmentSlot.OFFHAND) {
      getInventory.offhand.set(0, stack)
    }
    superCall()
  }

  override def getItemBySlot(slotIn: EquipmentSlot): ItemStack = {
    if (slotIn == EquipmentSlot.MAINHAND)
      agent.equipmentInventory.getItem(0)
    else if(slotIn == EquipmentSlot.OFFHAND)
      getInventory.offhand.get(0)
    else super.getItemBySlot(slotIn)
  }

  def fireRightClickBlock(pos: BlockPos, side: Direction): PlayerInteractEvent.RightClickBlock = {
    val hitVec = new Vec3(0.5 + side.getStepX * 0.5, 0.5 + side.getStepY * 0.5, 0.5 + side.getStepZ * 0.5)
    val event = new PlayerInteractEvent.RightClickBlock(this, InteractionHand.OFF_HAND, pos, new BlockHitResult(hitVec, side, pos, false))
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  def fireLeftClickBlock(pos: BlockPos, side: Direction): PlayerInteractEvent.LeftClickBlock = {
    net.minecraftforge.common.ForgeHooks.onLeftClickBlock(this, pos, side)
  }

  def fireRightClickAir(): PlayerInteractEvent.RightClickItem = {
    val event = new PlayerInteractEvent.RightClickItem(this, InteractionHand.OFF_HAND)
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  private def trySetActiveHand(duration: Double): Boolean = {
    releaseUsingItem()
    val entity = this
    val durationHandler = new {
      @SubscribeEvent(priority = EventPriority.LOWEST)
      def onItemUseStart(startUse: LivingEntityUseItemEvent.Start): Unit = {
        if (startUse.getEntity == entity && !startUse.isCanceled) {
          startUse.setDuration(duration.toInt)
        }
      }
    }
    MinecraftForge.EVENT_BUS.register(durationHandler)
    try {
      startUsingItem(InteractionHand.OFF_HAND)
      isUsingItem
    } catch {
        case _: Exception => false
    } finally {
      MinecraftForge.EVENT_BUS.unregister(durationHandler)
    }
  }

  def useItemWithHand(duration: Double, stack: ItemStack): Boolean = {
    if (!trySetActiveHand(duration)) {
      if (duration > 0) {
        return false
      }
    }

    val oldStack = stack.copy
    if (!isItemUseAllowed(stack)) {
      return false
    }

    val maxDuration = stack.getUseDuration
    val heldTicks = Math.max(0, Math.min(maxDuration, (duration * 20).toInt))
    agent.machine.pause(heldTicks / 20.0)

    // setting the active hand will also set its initial duration
    val useItemResult = stack.use(level, this, InteractionHand.OFF_HAND)
    releaseUsingItem()

    if (!useItemResult.getResult.consumesAction) {
      return false
    }

    val newStack = useItemResult.getObject
    val stackChanged: Boolean =
      !ItemStack.matches(oldStack, newStack) ||
      !ItemStack.matches(oldStack, stack)

    if (stackChanged) {
      getInventory.offhand.set(0, newStack)
    }
    stackChanged
  }

  def useEquippedItem(duration: Double, stackOption: Option[ItemStack] = None): Boolean = {
    if (stackOption.isEmpty) {
      return callUsingItemInSlot(agent.equipmentInventory, 0, {
        case item: ItemStack if item != null => useEquippedItem(duration, Option(item))
        case _ => false
      })
    }

    if (shouldCancel(() => fireRightClickAir())) {
      return false
    }

    // Change the offset at which items are used, to avoid hitting
    // the robot itself (e.g. with bows, potions, mining laser, ...).
    setPos(getX + facing.getStepX / 2.0, getY, getZ + facing.getStepZ / 2.0)

    try {
      useItemWithHand(duration, stackOption.get)
    }
    finally {
      setPos(getX - facing.getStepX / 2.0, getY, getZ - facing.getStepZ / 2.0)
    }
  }

  def placeBlock(slot: Int, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    callUsingItemInSlot(agent.mainInventory, slot, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return false
      }

      tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ)
    }, repair = false)
  }

  def clickBlock(pos: BlockPos, side: Direction): Double = callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
    val state = level.getBlockState(pos)
    val block = state.getBlock

    if (!state.canHarvestBlock(level, pos, this)) return 0

    val hardness = state.getDestroySpeed(level, pos)
    val cobwebOverride = block == Blocks.COBWEB && Settings.get.screwCobwebs

    val strength = getDigSpeed(state, pos)
    val breakTime =
      if (cobwebOverride) Settings.get.swingDelay
      else hardness * 1.5 / strength

    if (breakTime.isInfinity) return 0
    if (breakTime < 0) return breakTime

    val preEvent = new RobotBreakBlockEvent.Pre(agent, level, pos, breakTime * Settings.get.harvestRatio)
    MinecraftForge.EVENT_BUS.post(preEvent)
    if (preEvent.isCanceled) return 0
    val adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime)

    if (!PlayerInteractionManagerHelper.onBlockClicked(this, pos, side)) {
      if (level.isEmptyBlock(pos)) {
        return 1.0 / 20.0
      }
      return 0
    }

    EventHandler.scheduleServer(() => new DamageOverTime(this, pos, side, (adjustedBreakTime * 20).toInt).tick())

    adjustedBreakTime
  })

  private def isItemUseAllowed(stack: ItemStack) = stack.isEmpty || {
    (Settings.get.allowUseItemsWithDuration || stack.getUseDuration <= 0) && !ItemStack.isSameItem(stack,new ItemStack(Items.LEAD))
  }

  override def drop(stack: ItemStack, dropAround: Boolean, traceItem: Boolean): ItemEntity =
    InventoryUtils.spawnStackInWorld(BlockPosition(agent), stack, if (dropAround) None else Option(facing))

  private def shouldCancel(f: () => PlayerInteractEvent) = {
    try {
      val event = f()
      event.isCanceled || (event match {
        case rightClick: PlayerInteractEvent.RightClickBlock => rightClick.getUseBlock == Event.Result.DENY || rightClick.getUseItem == Event.Result.DENY
        case leftClick: PlayerInteractEvent.LeftClickBlock => leftClick.getUseBlock == Event.Result.DENY || leftClick.getUseItem == Event.Result.DENY
        case rightClick: PlayerInteractEvent.RightClickItem => rightClick.getResult == Event.Result.DENY
        case _ => false
      })
    }
    catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
  }

  private def callUsingItemInSlot[T](inventory: Container, slot: Int, f: ItemStack => T, repair: Boolean = true) = {
    val itemsBefore = adjacentItems
    val stack = inventory.getItem(slot)
    val oldStack = stack.copy()
    this.getInventory.selected = if (inventory == agent.mainInventory) slot else ~slot
    this.getInventory.offhand.set(0, inventory.getItem(slot))
    try {
      f(stack)
    }
    finally {
      this.getInventory.selected = 0
      inventory.setItem(slot, this.getInventory.offhand.get(0))
      this.getInventory.offhand.set(0, ItemStack.EMPTY)
      val newStack = inventory.getItem(slot)
      // this is only possible if f() modified the stack object in-place
      // looking at you, ic2
      if (ItemStack.matches(oldStack, newStack) &&
         !ItemStack.matches(oldStack, stack)) {
        inventory.setItem(slot, stack)
      }
      if (!newStack.isEmpty) {
        if (newStack.getCount <= 0) {
          inventory.setItem(slot, ItemStack.EMPTY)
        }
        if (repair) {
          if (newStack.getCount > 0) tryRepair(newStack, oldStack)
          else ForgeEventFactory.onPlayerDestroyItem(this, newStack, InteractionHand.OFF_HAND)
        }
      }
      collectDroppedItems(itemsBefore.asScala)
    }
  }

  private def tryRepair(stack: ItemStack, oldStack: ItemStack) {
    // Only if the underlying type didn't change.
    if (!stack.isEmpty && !oldStack.isEmpty && stack.getItem == oldStack.getItem) {
      val damageRate = new RobotUsedToolEvent.ComputeDamageRate(agent, oldStack, stack, Settings.get.itemDamageRate)
      MinecraftForge.EVENT_BUS.post(damageRate)
      if (damageRate.getDamageRate < 1) {
        MinecraftForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(agent, oldStack, stack, damageRate.getDamageRate))
      }
    }
  }

  private def tryPlaceBlockWhileHandlingFunnySpecialCases(stack: ItemStack, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = {
    !stack.isEmpty && stack.getCount > 0 && {
      val event = new RobotPlaceBlockEvent.Pre(agent, stack, level, pos)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) false
      else {
        val fakeEyeHeight = if (getXRot < 0 && isSomeKindOfPiston(stack)) 1.82 else 0
        setPos(getX, getY - fakeEyeHeight, getZ)
        Player.setPlayerInventoryItems(this)
        val state = level.getBlockState(pos)
        val traceEndPos = new Vec3(pos.getX + hitX, pos.getY + hitY, pos.getZ + hitZ)
        val traceCtx = if (state.isAir) BlockHitResult.miss(traceEndPos, side, pos) else new BlockHitResult(traceEndPos, side, pos, false)
        val didPlace = stack.useOn(new UseOnContext(level, this, InteractionHand.OFF_HAND, stack, traceCtx))
        Player.detectPlayerInventoryChanges(this)
        setPos(getX, getY + fakeEyeHeight, getZ)
        if (didPlace.consumesAction) {
          MinecraftForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(agent, stack, level, pos))
        }
        didPlace.consumesAction
      }
    }
  }

  private def isSomeKindOfPiston(stack: ItemStack) =
    stack.getItem match {
      case itemBlock: BlockItem =>
        val block = itemBlock.getBlock
        block != null && block.isInstanceOf[PistonBaseBlock]
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def causeFoodExhaustion(amount: Float) {
    if (Settings.get.robotExhaustionCost > 0) {
      agent.machine.node match {
        case connector: Connector => connector.changeBuffer(-Settings.get.robotExhaustionCost * amount)
        case _ => // This shouldn't happen... oh well.
      }
    }
    MinecraftForge.EVENT_BUS.post(new RobotExhaustionEvent(agent, amount))
  }

  override def closeContainer() {}

  override def swing(hand: InteractionHand): Unit = {}

  override protected def getPermissionLevel: Int = {
    val config = server.getPlayerList
    if (config.isOp(getGameProfile)) {
      config.getOps.get(getGameProfile) match {
        case opEntry: ServerOpListEntry => opEntry.getLevel
        case _ => server.getOperatorUserPermissionLevel
      }
    }
    else 0
  }

  override def canHarmPlayer(player: net.minecraft.world.entity.player.Player): Boolean = Settings.get.canAttackPlayers

  override def canEat(value: Boolean) = false

  override def canBeAffected(effect: MobEffectInstance) = false

  override def doHurtTarget(entity: Entity) = false

  override def hurt(source: DamageSource, damage: Float) = false

  override def heal(amount: Float) {}

  override def setHealth(value: Float) {}

  override def remove(invalidate: Entity.RemovalReason): Unit = super.remove(RemovalReason.UNLOADED_WITH_PLAYER)

  override def aiStep() {}

  override def take(entity: Entity, count: Int) {}

  override def setLastHurtByMob(entity: LivingEntity) {}

  override def setLastHurtMob(entity: Entity) {}

  override def startRiding(entityIn: Entity, force: Boolean): Boolean = false

  override def startSleepInBed(bedLocation: BlockPos) = Either.left[BedSleepingProblem, net.minecraft.util.Unit](BedSleepingProblem.OTHER_PROBLEM)

  override def sendSystemMessage(p_240560_ : Component, p_240545_ : Boolean) {}

  override def openCommandBlock(commandBlock: CommandBlockEntity): Unit = {}

  override def sendMerchantOffers(containerId: Int, offers: MerchantOffers, villagerLevel: Int, villagerXP: Int, showProgress: Boolean, canRestock: Boolean): Unit = {}

  override def openMenu(guiOwner: MenuProvider) = util.OptionalInt.empty

  override def openMinecartCommandBlock(thing: BaseCommandBlock): Unit = {}

  override def openTextEdit(p_277909_ : SignBlockEntity, p_277495_ : Boolean) {}

  // ----------------------------------------------------------------------- //

  class DamageOverTime(val player: Player, val pos: BlockPos, val side: Direction, val ticksTotal: Int) {
    val level: Level = player.level
    var ticks = 0
    var lastDamageSent = 0

    def tick(): Unit = {
      // Cancel if the agent stopped or our action is invalidated some other way.
      if (level != player.level || !level.isLoaded(pos) || level.isEmptyBlock(pos) || !player.agent.machine.isRunning) {
        player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side, player.level().getMaxBuildHeight(), 0)
        return
      }

      val damage = 10 * ticks / Math.max(ticksTotal, 1)
      if (damage < 10) {
        ticks += 1
        if (damage != lastDamageSent) {
          lastDamageSent = damage
          if (!PlayerInteractionManagerHelper.updateBlockRemoving(player))
            return
        }
        EventHandler.scheduleServer(() => tick())
      }
      else {
        callUsingItemInSlot(player.agent.equipmentInventory(), 0, _ => {
          this.player.setPos(this.player.getX - side.getStepX / 2.0, this.player.getY, this.player.getZ - side.getStepZ / 2.0)
          val expGained: Int = PlayerInteractionManagerHelper.blockRemoving(player, pos)
          this.player.setPos(this.player.getX + side.getStepX / 2.0, this.player.getY, this.player.getZ + side.getStepZ / 2.0)
          if (expGained >= 0) {
            MinecraftForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(agent, expGained))
          }
        })
      }
    }
  }
}
