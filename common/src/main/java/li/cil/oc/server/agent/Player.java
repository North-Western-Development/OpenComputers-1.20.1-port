package li.cil.oc.server.agent;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.platform.AgentPlatform;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The fake player used by agents (robots, drones) to interact with the world.
 * <p>
 * Formerly a Forge {@code FakePlayer}; now extends {@link ServerPlayer}
 * directly, with a channel-less connection that swallows all packets.
 */
public class Player extends ServerPlayer {
    // ----------------------------------------------------------------------- //
    // Former companion object.

    public static GameProfile profileFor(Agent agent) {
        final UUID uuid = agent.ownerUUID();
        final String randomId = String.valueOf(agent.world().random.nextInt(0xFFFFFF) + 1);
        final String name = Settings.get().nameFormat.
                replace("$player$", agent.ownerName()).
                replace("$random$", randomId);
        return new GameProfile(uuid, name);
    }

    public static UUID determineUUID(Optional<UUID> playerUUID) {
        final String format = Settings.get().uuidFormat;
        final UUID randomUUID = UUID.randomUUID();
        try {
            return UUID.fromString(format.
                    replace("$random$", randomUUID.toString()).
                    replace("$player$", playerUUID.orElse(randomUUID).toString()));
        } catch (Throwable t) {
            OpenComputers.log.warn("Failed determining robot UUID, check your config's `uuidFormat` entry!", t);
            return randomUUID;
        }
    }

    public static UUID determineUUID() {
        return determineUUID(Optional.empty());
    }

    public static void updatePositionAndRotation(Player player, Direction facing, Direction side) {
        player.facing = facing;
        player.side = side;
        final Vec3 direction = new Vec3(
                facing.getStepX() + side.getStepX(),
                facing.getStepY() + side.getStepY(),
                facing.getStepZ() + side.getStepZ()).normalize();
        final float yaw = (float) Math.toDegrees(-Math.atan2(direction.x, direction.z));
        final float pitch = (float) Math.toDegrees(-Math.atan2(direction.y, Math.sqrt((direction.x * direction.x) + (direction.z * direction.z)))) * 0.99f;
        player.setPos(player.agent.xPosition(), player.agent.yPosition(), player.agent.zPosition());
        player.setXRot(pitch % 360f);
        player.setYRot(yaw % 360f);
        player.xRotO = player.getXRot();
        player.yRotO = player.getYRot();
    }

    private static void setCopyOrNull(NonNullList<ItemStack> inv, Container agentInv, int slot) {
        final ItemStack item = agentInv.getItem(slot);
        inv.set(slot, item != null ? item.copy() : ItemStack.EMPTY);
    }

    public static void setPlayerInventoryItems(Player player) {
        // the offhand is simply the agent's tool item
        final Agent agent = player.agent;

        for (int i = 0; i < 4; i++) {
            setCopyOrNull(player.inventory.armor, agent.equipmentInventory(), i);
        }

        // items is 36 items
        // the agent inventory is 100 items with some space for components
        // leaving us 88..we'll copy what we can
        final int size = Math.min(player.inventory.items.size(), agent.mainInventory().getContainerSize());
        for (int i = 0; i < size; i++) {
            setCopyOrNull(player.inventory.items, agent.mainInventory(), i);
        }
        player.inventoryMenu.broadcastChanges();
    }

    private static void setCopy(Container inv, int index, ItemStack item) {
        final ItemStack result = item != null ? item.copy() : ItemStack.EMPTY;
        final ItemStack current = inv.getItem(index);
        if (!ItemStack.matches(result, current)) {
            inv.setItem(index, result);
        }
    }

    public static void detectPlayerInventoryChanges(Player player) {
        final Agent agent = player.agent;
        player.inventoryMenu.broadcastChanges();
        // The follow code will set agent.inventories = FakePlayer's inv.stack
        for (int i = 0; i < 4; i++) {
            setCopy(agent.equipmentInventory(), i, player.inventory.armor.get(i));
        }
        final int size = Math.min(player.inventory.items.size(), agent.mainInventory().getContainerSize());
        for (int i = 0; i < size; i++) {
            setCopy(agent.mainInventory(), i, player.inventory.items.get(i));
        }
    }

    // ----------------------------------------------------------------------- //

    public final Agent agent;

    public Direction facing = Direction.SOUTH;

    public Direction side = Direction.SOUTH;

    /**
     * While set, block breaking progresses instantly (see
     * {@link PlayerInteractionManagerHelper#blockRemoving}).
     */
    public boolean instantBreak = false;

    public Player(Agent agent) {
        super(((ServerLevel) agent.world()).getServer(), (ServerLevel) agent.world(), profileFor(agent));
        this.agent = agent;

        // No real client: packets go nowhere.
        this.connection = new FakePacketListener(this.server, this);

        getAbilities().mayfly = true;
        getAbilities().invulnerable = true;
        getAbilities().flying = true;
        setOnGround(true);

        refreshDimensions();

        this.inventory = new Inventory(this, agent);
        // because the inventory was just overwritten, the container is now detached
        this.inventoryMenu = new InventoryMenu(inventory, !level().isClientSide(), this);
        this.containerMenu = this.inventoryMenu;

        // TODO(port): Forge exposed the player's inventory as item handler capabilities
        //  (playerMainHandler / playerEquipmentHandler / playerJoinedHandler, built from the
        //  original inventory at construction); those were re-pointed at the agent inventory.
        //  Forge now builds them lazily from getInventory(), and there is no Fabric equivalent.
    }

    /**
     * Packet listener for the fake connection; never ticked, sends nothing.
     */
    private static final class FakePacketListener extends ServerGamePacketListenerImpl {
        FakePacketListener(MinecraftServer server, ServerPlayer player) {
            super(server, new FakeNetworkManager(), player);
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet) {
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet, @Nullable net.minecraft.network.PacketSendListener listener) {
        }
    }

    @Override
    public double getMyRidingOffset() {
        return 0.5;
    }

    @Override
    public float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        return 0f;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1, 1);
    }

    @Override
    public Component getName() {
        return agent == null ? super.getName() : Component.literal(agent.name());
    }

    // ----------------------------------------------------------------------- //

    public <T extends Entity> Optional<Entity> closestEntity(Class<T> clazz, Direction side) {
        final net.minecraft.world.phys.AABB bounds = BlockPosition.apply(agent).offset(side).bounds();
        final List<T> candidates = level().getEntitiesOfClass(clazz, bounds);
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(Collections.min(candidates, Comparator.comparingDouble(this::distanceToSqr)));
    }

    public <T extends Entity> Optional<Entity> closestEntity(Class<T> clazz) {
        return closestEntity(clazz, facing);
    }

    public <T extends Entity> List<T> entitiesOnSide(Class<T> clazz, Direction side) {
        return entitiesInBlock(clazz, BlockPosition.apply(agent).offset(side));
    }

    public <T extends Entity> List<T> entitiesInBlock(Class<T> clazz, BlockPosition blockPos) {
        return level().getEntitiesOfClass(clazz, blockPos.bounds());
    }

    private List<ItemEntity> adjacentItems() {
        return level().getEntitiesOfClass(ItemEntity.class, BlockPosition.apply(agent).bounds().inflate(2, 2, 2));
    }

    private void collectDroppedItems(List<ItemEntity> itemsBefore) {
        final List<ItemEntity> itemsDropped = new ArrayList<>(adjacentItems());
        itemsDropped.removeAll(itemsBefore);
        for (ItemEntity drop : itemsDropped) {
            drop.setNoPickUpDelay();
            drop.playerTouch(this);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void attack(Entity entity) {
        callUsingItemInSlot(agent.equipmentInventory(), 0, stack -> {
            if (entity instanceof net.minecraft.world.entity.player.Player player && !canHarmPlayer(player)) {
                // Avoid player damage.
                return null;
            }
            final RobotAttackEntityEvent.Pre event = new RobotAttackEntityEvent.Pre(agent, entity);
            EventBus.INSTANCE.post(event);
            if (!event.isCanceled()) {
                super.attack(entity);
                EventBus.INSTANCE.post(new RobotAttackEntityEvent.Post(agent, entity));
            }
            return null;
        });
    }

    @Override
    public InteractionResult interactOn(Entity entity, InteractionHand hand) {
        boolean cancel;
        try {
            cancel = AgentPlatform.fireEntityInteract(this, entity, hand);
        } catch (Throwable t) {
            OpenComputers.log.warn("Some event handler screwed up!", t);
            cancel = false;
        }
        if (!cancel && callUsingItemInSlot(agent.equipmentInventory(), 0, stack -> {
            final boolean result = isItemUseAllowed(stack) && (entity.interact(this, hand).consumesAction() ||
                    (entity instanceof LivingEntity living && !getItemInHand(InteractionHand.MAIN_HAND).isEmpty() &&
                            getItemInHand(InteractionHand.MAIN_HAND).interactLivingEntity(this, living, hand).consumesAction()));
            if (!getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                if (getItemInHand(InteractionHand.MAIN_HAND).getCount() <= 0) {
                    final ItemStack orig = getItemInHand(InteractionHand.MAIN_HAND);
                    this.inventory.setItem(this.inventory.selected, ItemStack.EMPTY);
                    AgentPlatform.onPlayerDestroyItem(this, orig, hand);
                } else {
                    // because of various hacks for IC2, we expect the in-hand result to be moved to our offhand buffer
                    this.inventory.offhand.set(0, getItemInHand(InteractionHand.MAIN_HAND));
                    this.inventory.setItem(this.inventory.selected, ItemStack.EMPTY);
                }
            }
            return result;
        })) return InteractionResult.sidedSuccess(level().isClientSide());
        return InteractionResult.PASS;
    }

    private static UseOnContext useOnContext(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return new UseOnContext(level, player, hand, stack, hit) {
        };
    }

    public ActivationType activateBlockOrUseItem(BlockPos pos, Direction side, float hitX, float hitY, float hitZ, double duration) {
        return callUsingItemInSlot(agent.equipmentInventory(), 0, stack -> {
            if (shouldCancel(() -> fireRightClickBlock(pos, side))) {
                return ActivationType.None;
            }

            final Item item = !stack.isEmpty() ? stack.getItem() : null;
            final BlockState state = level().getBlockState(pos);
            final Vec3 traceEndPos = new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
            final BlockHitResult traceCtx = state.isAir() ? BlockHitResult.miss(traceEndPos, side, pos) : new BlockHitResult(traceEndPos, side, pos, false);
            if (item != null && AgentPlatform.onItemUseFirst(stack, useOnContext(level(), this, InteractionHand.OFF_HAND, stack, traceCtx)).consumesAction()) {
                return ActivationType.ItemUsed;
            }

            final boolean canActivate = !state.isAir() && Settings.get().allowActivateBlocks;
            final boolean shouldActivate = canActivate && (!isCrouching() || (item == null || AgentPlatform.doesSneakBypassUse(stack, level(), pos, this)));
            if (shouldActivate && state.use(level(), this, InteractionHand.OFF_HAND, new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos, false)).consumesAction())
                return ActivationType.BlockActivated;
            else if (duration <= Double.MIN_VALUE && isItemUseAllowed(stack) && tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ))
                return ActivationType.ItemPlaced;
            else if (useEquippedItem(duration, Optional.of(stack)))
                return ActivationType.ItemUsed;
            else
                return ActivationType.None;
        });
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        if (agent == null) {
            super.setItemSlot(slotIn, stack);
            return;
        }
        if (slotIn == EquipmentSlot.MAINHAND) {
            agent.equipmentInventory().setItem(0, stack);
            final int slot = inventory.selected;
            // So, if we're not in the main inventory, selected is set to -1
            // for compatibility with mods that try accessing the inv directly
            // using inventory.selected. See li.cil.oc.server.agent.Inventory
            if (inventory.selected < 0) inventory.selected = ~inventory.selected;
            try {
                super.setItemSlot(slotIn, stack);
            } finally {
                inventory.selected = slot;
            }
            return;
        } else if (slotIn == EquipmentSlot.OFFHAND) {
            inventory.offhand.set(0, stack);
        }
        super.setItemSlot(slotIn, stack);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slotIn) {
        // Called from ServerPlayer's constructor (spawn position collision check) before agent is set.
        if (agent == null) return super.getItemBySlot(slotIn);
        if (slotIn == EquipmentSlot.MAINHAND)
            return agent.equipmentInventory().getItem(0);
        else if (slotIn == EquipmentSlot.OFFHAND)
            return inventory.offhand.get(0);
        else return super.getItemBySlot(slotIn);
    }

    /**
     * Fires the platform's right-click-block event; returns true if canceled/denied.
     */
    public boolean fireRightClickBlock(BlockPos pos, Direction side) {
        final Vec3 hitVec = new Vec3(0.5 + side.getStepX() * 0.5, 0.5 + side.getStepY() * 0.5, 0.5 + side.getStepZ() * 0.5);
        return AgentPlatform.fireRightClickBlock(this, InteractionHand.OFF_HAND, pos, new BlockHitResult(hitVec, side, pos, false));
    }

    /**
     * Fires the platform's left-click-block event; returns true if canceled/denied.
     */
    public boolean fireLeftClickBlock(BlockPos pos, Direction side) {
        return AgentPlatform.fireLeftClickBlock(this, pos, side);
    }

    /**
     * Fires the platform's right-click-item event; returns true if canceled/denied.
     */
    public boolean fireRightClickAir() {
        return AgentPlatform.fireRightClickItem(this, InteractionHand.OFF_HAND);
    }

    private boolean trySetActiveHand(double duration) {
        releaseUsingItem();
        try {
            startUsingItem(InteractionHand.OFF_HAND);
            if (isUsingItem()) {
                // Formerly done in a LivingEntityUseItemEvent.Start listener.
                this.useItemRemaining = (int) duration;
            }
            return isUsingItem();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean useItemWithHand(double duration, ItemStack stack) {
        if (!trySetActiveHand(duration)) {
            if (duration > 0) {
                return false;
            }
        }

        final ItemStack oldStack = stack.copy();
        if (!isItemUseAllowed(stack)) {
            return false;
        }

        final int maxDuration = stack.getUseDuration();
        final int heldTicks = Math.max(0, Math.min(maxDuration, (int) (duration * 20)));
        agent.machine().pause(heldTicks / 20.0);

        // setting the active hand will also set its initial duration
        final InteractionResultHolder<ItemStack> useItemResult = stack.use(level(), this, InteractionHand.OFF_HAND);
        releaseUsingItem();

        if (!useItemResult.getResult().consumesAction()) {
            return false;
        }

        final ItemStack newStack = useItemResult.getObject();
        final boolean stackChanged =
                !ItemStack.matches(oldStack, newStack) ||
                        !ItemStack.matches(oldStack, stack);

        if (stackChanged) {
            inventory.offhand.set(0, newStack);
        }
        return stackChanged;
    }

    public boolean useEquippedItem(double duration) {
        return useEquippedItem(duration, Optional.empty());
    }

    public boolean useEquippedItem(double duration, Optional<ItemStack> stackOption) {
        if (stackOption.isEmpty()) {
            return callUsingItemInSlot(agent.equipmentInventory(), 0, item ->
                    item != null && useEquippedItem(duration, Optional.of(item)));
        }

        if (shouldCancel(this::fireRightClickAir)) {
            return false;
        }

        // Change the offset at which items are used, to avoid hitting
        // the robot itself (e.g. with bows, potions, mining laser, ...).
        setPos(getX() + facing.getStepX() / 2.0, getY(), getZ() + facing.getStepZ() / 2.0);

        try {
            return useItemWithHand(duration, stackOption.get());
        } finally {
            setPos(getX() - facing.getStepX() / 2.0, getY(), getZ() - facing.getStepZ() / 2.0);
        }
    }

    public boolean placeBlock(int slot, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        return callUsingItemInSlot(agent.mainInventory(), slot, stack -> {
            if (shouldCancel(() -> fireRightClickBlock(pos, side))) {
                return false;
            }

            return tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ);
        }, false);
    }

    public double clickBlock(BlockPos pos, Direction side) {
        return callUsingItemInSlot(agent.equipmentInventory(), 0, stack -> {
            final BlockState state = level().getBlockState(pos);
            final Block block = state.getBlock();

            if (!AgentPlatform.canHarvestBlock(state, level(), pos, this)) return 0.0;

            final float hardness = state.getDestroySpeed(level(), pos);
            final boolean cobwebOverride = block == Blocks.COBWEB && Settings.get().screwCobwebs;

            final float strength = AgentPlatform.getDigSpeed(this, state, pos);
            final double breakTime =
                    cobwebOverride ? Settings.get().swingDelay
                            : hardness * 1.5 / strength;

            if (Double.isInfinite(breakTime)) return 0.0;
            if (breakTime < 0) return breakTime;

            final RobotBreakBlockEvent.Pre preEvent = new RobotBreakBlockEvent.Pre(agent, level(), pos, breakTime * Settings.get().harvestRatio);
            EventBus.INSTANCE.post(preEvent);
            if (preEvent.isCanceled()) return 0.0;
            final double adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime());

            if (!PlayerInteractionManagerHelper.onBlockClicked(this, pos, side)) {
                if (level().isEmptyBlock(pos)) {
                    return 1.0 / 20.0;
                }
                return 0.0;
            }

            final DamageOverTime damageOverTime = new DamageOverTime(this, pos, side, (int) (adjustedBreakTime * 20));
            EventHandler.scheduleServer(damageOverTime::tick);

            return adjustedBreakTime;
        });
    }

    private boolean isItemUseAllowed(ItemStack stack) {
        return stack.isEmpty() ||
                ((Settings.get().allowUseItemsWithDuration || stack.getUseDuration() <= 0) && !ItemStack.isSameItem(stack, new ItemStack(Items.LEAD)));
    }

    @Override
    public ItemEntity drop(ItemStack stack, boolean dropAround, boolean traceItem) {
        return InventoryUtils.spawnStackInWorld(BlockPosition.apply(agent), stack, dropAround ? Optional.empty() : Optional.of(facing));
    }

    private boolean shouldCancel(Supplier<Boolean> f) {
        try {
            return f.get();
        } catch (Throwable t) {
            OpenComputers.log.warn("Some event handler screwed up!", t);
            return false;
        }
    }

    private <T> T callUsingItemInSlot(Container inv, int slot, Function<ItemStack, T> f) {
        return callUsingItemInSlot(inv, slot, f, true);
    }

    private <T> T callUsingItemInSlot(Container inv, int slot, Function<ItemStack, T> f, boolean repair) {
        final List<ItemEntity> itemsBefore = adjacentItems();
        final ItemStack stack = inv.getItem(slot);
        final ItemStack oldStack = stack.copy();
        this.inventory.selected = inv == agent.mainInventory() ? slot : ~slot;
        this.inventory.offhand.set(0, inv.getItem(slot));
        try {
            return f.apply(stack);
        } finally {
            this.inventory.selected = 0;
            inv.setItem(slot, this.inventory.offhand.get(0));
            this.inventory.offhand.set(0, ItemStack.EMPTY);
            final ItemStack newStack = inv.getItem(slot);
            // this is only possible if f() modified the stack object in-place
            // looking at you, ic2
            if (ItemStack.matches(oldStack, newStack) &&
                    !ItemStack.matches(oldStack, stack)) {
                inv.setItem(slot, stack);
            }
            if (!newStack.isEmpty()) {
                if (newStack.getCount() <= 0) {
                    inv.setItem(slot, ItemStack.EMPTY);
                }
                if (repair) {
                    if (newStack.getCount() > 0) tryRepair(newStack, oldStack);
                    else AgentPlatform.onPlayerDestroyItem(this, newStack, InteractionHand.OFF_HAND);
                }
            }
            collectDroppedItems(itemsBefore);
        }
    }

    private void tryRepair(ItemStack stack, ItemStack oldStack) {
        // Only if the underlying type didn't change.
        if (!stack.isEmpty() && !oldStack.isEmpty() && stack.getItem() == oldStack.getItem()) {
            final RobotUsedToolEvent.ComputeDamageRate damageRate = new RobotUsedToolEvent.ComputeDamageRate(agent, oldStack, stack, Settings.get().itemDamageRate);
            EventBus.INSTANCE.post(damageRate);
            if (damageRate.getDamageRate() < 1) {
                EventBus.INSTANCE.post(new RobotUsedToolEvent.ApplyDamageRate(agent, oldStack, stack, damageRate.getDamageRate()));
            }
        }
    }

    private boolean tryPlaceBlockWhileHandlingFunnySpecialCases(ItemStack stack, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (stack.isEmpty() || stack.getCount() <= 0) return false;
        final RobotPlaceBlockEvent.Pre event = new RobotPlaceBlockEvent.Pre(agent, stack, level(), pos);
        EventBus.INSTANCE.post(event);
        if (event.isCanceled()) return false;

        final double fakeEyeHeight = getXRot() < 0 && isSomeKindOfPiston(stack) ? 1.82 : 0;
        setPos(getX(), getY() - fakeEyeHeight, getZ());
        Player.setPlayerInventoryItems(this);
        final BlockState state = level().getBlockState(pos);
        final Vec3 traceEndPos = new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
        final BlockHitResult traceCtx = state.isAir() ? BlockHitResult.miss(traceEndPos, side, pos) : new BlockHitResult(traceEndPos, side, pos, false);
        final InteractionResult didPlace = stack.useOn(useOnContext(level(), this, InteractionHand.OFF_HAND, stack, traceCtx));
        Player.detectPlayerInventoryChanges(this);
        setPos(getX(), getY() + fakeEyeHeight, getZ());
        if (didPlace.consumesAction()) {
            EventBus.INSTANCE.post(new RobotPlaceBlockEvent.Post(agent, stack, level(), pos));
        }
        return didPlace.consumesAction();
    }

    private static boolean isSomeKindOfPiston(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem itemBlock) {
            final Block block = itemBlock.getBlock();
            return block instanceof PistonBaseBlock;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public float getDestroySpeed(BlockState state) {
        // Used to force the block to break when the agent's timer is up
        // (formerly a Forge PlayerEvent.BreakSpeed listener; on Forge the
        // equivalent is registered by AgentPlatform.ensureInstantBreakSupport).
        if (instantBreak) return Float.MAX_VALUE;
        return super.getDestroySpeed(state);
    }

    @Override
    public void causeFoodExhaustion(float amount) {
        if (Settings.get().robotExhaustionCost > 0) {
            if (agent.machine().node() instanceof Connector connector) {
                connector.changeBuffer(-Settings.get().robotExhaustionCost * amount);
            }
            // else: This shouldn't happen... oh well.
        }
        EventBus.INSTANCE.post(new RobotExhaustionEvent(agent, amount));
    }

    @Override
    public void closeContainer() {
    }

    @Override
    public void swing(InteractionHand hand) {
    }

    @Override
    protected int getPermissionLevel() {
        final PlayerList config = server.getPlayerList();
        if (config.isOp(getGameProfile())) {
            final ServerOpListEntry opEntry = config.getOps().get(getGameProfile());
            if (opEntry != null) return opEntry.getLevel();
            return server.getOperatorUserPermissionLevel();
        }
        return 0;
    }

    @Override
    public boolean canHarmPlayer(net.minecraft.world.entity.player.Player player) {
        return Settings.get().canAttackPlayers;
    }

    @Override
    public boolean canEat(boolean value) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    public void heal(float amount) {
    }

    @Override
    public void setHealth(float value) {
    }

    @Override
    public void aiStep() {
    }

    @Override
    public void take(Entity entity, int count) {
    }

    @Override
    public void setLastHurtByMob(@Nullable LivingEntity entity) {
    }

    @Override
    public void setLastHurtMob(Entity entity) {
    }

    @Override
    public boolean startRiding(Entity entityIn, boolean force) {
        return false;
    }

    @Override
    public Either<BedSleepingProblem, net.minecraft.util.Unit> startSleepInBed(BlockPos bedLocation) {
        return Either.left(BedSleepingProblem.OTHER_PROBLEM);
    }

    @Override
    public void sendSystemMessage(Component message, boolean overlay) {
    }

    @Override
    public void openCommandBlock(CommandBlockEntity commandBlock) {
    }

    @Override
    public void sendMerchantOffers(int containerId, MerchantOffers offers, int villagerLevel, int villagerXP, boolean showProgress, boolean canRestock) {
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider guiOwner) {
        return OptionalInt.empty();
    }

    @Override
    public void openMinecartCommandBlock(BaseCommandBlock thing) {
    }

    @Override
    public void openTextEdit(SignBlockEntity signTile, boolean isFrontText) {
    }

    // ----------------------------------------------------------------------- //

    public class DamageOverTime {
        public final Player player;
        public final BlockPos pos;
        public final Direction side;
        public final int ticksTotal;
        public final Level level;
        public int ticks = 0;
        public int lastDamageSent = 0;

        public DamageOverTime(Player player, BlockPos pos, Direction side, int ticksTotal) {
            this.player = player;
            this.pos = pos;
            this.side = side;
            this.ticksTotal = ticksTotal;
            this.level = player.level();
        }

        public void tick() {
            // Cancel if the agent stopped or our action is invalidated some other way.
            if (level != player.level() || !level.isLoaded(pos) || level.isEmptyBlock(pos) || !player.agent.machine().isRunning()) {
                player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side, level.getMaxBuildHeight(), 0);
                return;
            }

            final int damage = 10 * ticks / Math.max(ticksTotal, 1);
            if (damage < 10) {
                ticks += 1;
                if (damage != lastDamageSent) {
                    lastDamageSent = damage;
                    if (!PlayerInteractionManagerHelper.updateBlockRemoving(player))
                        return;
                }
                EventHandler.scheduleServer(this::tick);
            } else {
                callUsingItemInSlot(player.agent.equipmentInventory(), 0, unused -> {
                    this.player.setPos(this.player.getX() - side.getStepX() / 2.0, this.player.getY(), this.player.getZ() - side.getStepZ() / 2.0);
                    final int expGained = PlayerInteractionManagerHelper.blockRemoving(player, pos);
                    this.player.setPos(this.player.getX() + side.getStepX() / 2.0, this.player.getY(), this.player.getZ() + side.getStepZ() / 2.0);
                    if (expGained >= 0) {
                        EventBus.INSTANCE.post(new RobotBreakBlockEvent.Post(agent, expGained));
                    }
                    return null;
                });
            }
        }
    }
}
