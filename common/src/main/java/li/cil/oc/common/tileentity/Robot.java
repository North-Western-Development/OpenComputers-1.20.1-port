package li.cil.oc.common.tileentity;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotAnalyzeEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.InventoryProxy;
import li.cil.oc.common.inventory.InventorySelection;
import li.cil.oc.common.inventory.TankSelection;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.tileentity.traits.Computer;
import li.cil.oc.common.tileentity.traits.PowerInformation;
import li.cil.oc.common.tileentity.traits.RotatableTile;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.integration.opencomputers.DriverKeyboard;
import li.cil.oc.integration.opencomputers.DriverRedstoneCard;
import li.cil.oc.integration.opencomputers.DriverScreen;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.agent.Player;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
//
// Port note: BlockEntity#worldPosition is final in 1.20, so the robot keeps its
// own position (see setLevelAndPosition / getBlockPos).
public class Robot extends TileEntity implements Computer, PowerInformation, RotatableTile, FluidHandler, li.cil.oc.api.internal.Robot, InventorySelection, TankSelection, MenuProvider {
    public RobotProxy proxy;

    public final RobotData info = new RobotData();

    public final li.cil.oc.server.component.Robot bot;

    private BlockPos robotPosition;

    // ----------------------------------------------------------------------- //

    public final InventoryProxy equipmentInventory = new InventoryProxy() {
        @Override
        public Container inventory() {
            return Robot.this;
        }

        @Override
        public int getContainerSize() {
            return 4;
        }
    };

    // Wrapper for the part of the inventory that is mutable.
    public final InventoryProxy mainInventory = new InventoryProxy() {
        @Override
        public Container inventory() {
            return Robot.this;
        }

        @Override
        public int getContainerSize() {
            return Robot.this.inventorySize;
        }

        @Override
        public int offset() {
            return equipmentInventory.getContainerSize();
        }
    };

    public final int actualInventorySize = 100;

    public int inventorySize = -1;

    public int selectedSlot = 0;

    public final MultiTank tank = new MultiTank() {
        @Override
        public int tankCount() {
            return Robot.this.tankCount();
        }

        @Override
        public FluidHandler getFluidTank(int index) {
            return Robot.this.getFluidTank(index);
        }
    };

    public int selectedTank = 0;

    // For client.
    public boolean renderingErrored = false;

    public double globalBuffer = 0.0;
    public double globalBufferSize = 0.0;

    public final int maxComponents = 32;

    public String ownerName = Settings.get().fakePlayerName;

    public UUID ownerUUID = Settings.get().fakePlayerProfile.getId();

    public int animationTicksLeft = 0;

    public int animationTicksTotal = 0;

    public Optional<BlockPos> moveFrom = Optional.empty();

    public boolean swingingTool = false;

    public int turnAxis = 0;

    public boolean appliedToolEnchantments = false;

    private Player player_;

    private boolean updatingInventorySize = false;

    private int containerSize = actualInventorySize;

    private static final String RobotTag = Settings.namespace + "robot";
    private static final String OwnerTag = Settings.namespace + "owner";
    private static final String OwnerUUIDTag = Settings.namespace + "ownerUuid";
    private static final String SelectedSlotTag = Settings.namespace + "selectedSlot";
    private static final String SelectedTankTag = Settings.namespace + "selectedTank";
    private static final String AnimationTicksTotalTag = Settings.namespace + "animationTicksTotal";
    private static final String AnimationTicksLeftTag = Settings.namespace + "animationTicksLeft";
    private static final String MoveFromXTag = Settings.namespace + "moveFromX";
    private static final String MoveFromYTag = Settings.namespace + "moveFromY";
    private static final String MoveFromZTag = Settings.namespace + "moveFromZ";
    private static final String SwingingToolTag = Settings.namespace + "swingingTool";
    private static final String TurnAxisTag = Settings.namespace + "turnAxis";

    public Robot(BlockPos pos, BlockState state) {
        super(TileEntityTypes.ROBOT.get(), pos, state);
        bot = isServer() ? new li.cil.oc.server.component.Robot(this) : null;
        if (isServer()) {
            machine().setCostPerTick(Settings.get().robotCost);
        }
    }

    // ----------------------------------------------------------------------- //
    // Position (the robot is not part of the world, see class comment).

    /** Replaces 1.16's TileEntity#setLevelAndPosition. */
    public void setLevelAndPosition(Level level, BlockPos pos) {
        setLevel(level);
        robotPosition = pos.immutable();
    }

    @Override
    public BlockPos getBlockPos() {
        return robotPosition != null ? robotPosition : super.getBlockPos();
    }

    // ----------------------------------------------------------------------- //

    // TODO(port): the robot's fluid handler (Forge FLUID_HANDLER capability) is not exposed to other
    //  mods yet; the loader modules would need to expose FluidHandler block entities.

    @Override
    public int tier() {
        return info.tier;
    }

    public boolean isCreative() {
        return tier() == Tier.Four;
    }

    @Override
    public InventoryProxy equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public InventoryProxy mainInventory() {
        return mainInventory;
    }

    @Override
    public MultiTank tank() {
        return tank;
    }

    public int maxInventorySize() {
        return actualInventorySize - equipmentInventory.getContainerSize() - componentCount();
    }

    @Override
    public int selectedSlot() {
        return selectedSlot;
    }

    @Override
    public void setSelectedSlot(int index) {
        selectedSlot = Math.max(0, Math.min(index, mainInventory.getContainerSize() - 1));
        if (getLevel() != null) {
            PacketSender.sendRobotSelectedSlotChange(this);
        }
    }

    @Override
    public int selectedTank() {
        return selectedTank;
    }

    @Override
    public void setSelectedTank(int index) {
        selectedTank = index;
    }

    @Override
    public int componentCount() {
        return info.components.length;
    }

    @Override
    public ManagedEnvironment getComponentInSlot(int index) {
        final Optional<ManagedEnvironment>[] components = components();
        return index >= 0 && components.length > index ? components[index].orElse(null) : null;
    }

    private Player player_() {
        if (player_ == null) player_ = new Player(this);
        return player_;
    }

    @Override
    public Player player() {
        Player.updatePositionAndRotation(player_(), facing(), facing());
        Player.setPlayerInventoryItems(player_());
        return player_();
    }

    @Override
    public void synchronizeSlot(int slot) {
        if (slot >= 0 && slot < getContainerSize()) {
            synchronized (this) {
                final ItemStack stack = getItem(slot);
                final Optional<ManagedEnvironment>[] components = components();
                if (slot < components.length && components[slot].isPresent()) {
                    // We're guaranteed to have a driver for entries.
                    save(components[slot].get(), Driver.driverFor(stack, getClass()), stack);
                }
                PacketSender.sendRobotInventory(this, slot, stack);
            }
        }
    }

    /** Scala {@code 1 to info.containers.length}. */
    public List<Integer> containerSlots() {
        final List<Integer> result = new ArrayList<>();
        for (int slot = 1; slot <= info.containers.length; slot++) result.add(slot);
        return result;
    }

    /** Scala {@code getContainerSize - componentCount until getContainerSize}. */
    public List<Integer> componentSlots() {
        final List<Integer> result = new ArrayList<>();
        for (int slot = getContainerSize() - componentCount(); slot < getContainerSize(); slot++) result.add(slot);
        return result;
    }

    /** Scala {@code equipmentInventory.getContainerSize until (equipmentInventory.getContainerSize + mainInventory.getContainerSize)}. */
    public List<Integer> inventorySlots() {
        final List<Integer> result = new ArrayList<>();
        final int start = equipmentInventory.getContainerSize();
        for (int slot = start; slot < start + mainInventory.getContainerSize(); slot++) result.add(slot);
        return result;
    }

    public void setLightColor(int value) {
        info.lightColor = value;
        PacketSender.sendRobotLightChange(this);
    }

    @Override
    public boolean shouldAnimate() {
        return isRunning();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double globalBuffer() {
        return globalBuffer;
    }

    @Override
    public void setGlobalBuffer(double value) {
        globalBuffer = value;
    }

    @Override
    public double globalBufferSize() {
        return globalBufferSize;
    }

    @Override
    public void setGlobalBufferSize(double value) {
        globalBufferSize = value;
    }

    @Override
    public String ownerName() {
        return ownerName;
    }

    @Override
    public UUID ownerUUID() {
        return ownerUUID;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public String name() {
        return info.name;
    }

    @Override
    public void setName(String name) {
        info.name = name;
    }

    @Override
    public Node[] onAnalyze(net.minecraft.world.entity.player.Player player, Direction side, float hitX, float hitY, float hitZ) {
        player.sendSystemMessage(Localization.Analyzer.RobotOwner(ownerName));
        player.sendSystemMessage(Localization.Analyzer.RobotName(player_().getName().getString()));
        EventBus.INSTANCE.post(new RobotAnalyzeEvent(this, player));
        return Computer.super.onAnalyze(player, side, hitX, hitY, hitZ);
    }

    public boolean move(Direction direction) {
        final Level level = getLevel();
        final BlockPos oldPosition = getBlockPos();
        final BlockPos newPosition = oldPosition.relative(direction);
        if (!level.isLoaded(newPosition)) {
            return false; // Don't fall off the earth.
        }

        if (isServer()) {
            final RobotMoveEvent.Pre event = new RobotMoveEvent.Pre(this, direction);
            if (EventBus.INSTANCE.post(event)) return false;
        }

        final li.cil.oc.common.block.RobotProxy blockRobotProxy = (li.cil.oc.common.block.RobotProxy) li.cil.oc.api.Items.get(Constants.BlockName.Robot).block();
        final li.cil.oc.common.block.RobotAfterimage blockRobotAfterImage = (li.cil.oc.common.block.RobotAfterimage) li.cil.oc.api.Items.get(Constants.BlockName.RobotAfterimage).block();
        final boolean wasAir = level.isEmptyBlock(newPosition);
        final BlockState state = level.getBlockState(newPosition);
        final Block block = state.getBlock();
        try {
            // Setting this will make the tile entity created via the following call
            // to setBlock to re-use our "real" instance as the inner object, instead
            // of creating a new one.
            blockRobotProxy.moving.set(Optional.of(this));
            // Do *not* immediately send the change to clients to allow checking if it
            // worked before the client is notified so that we can use the same trick on
            // the client by sending a corresponding packet. This also saves us from
            // having to send the complete state again (e.g. screen buffer) each move.
            level.setBlockAndUpdate(newPosition, Blocks.AIR.defaultBlockState());
            // In some cases (though I couldn't quite figure out which one) setBlock
            // will return true, even though the block was not created / adjusted.
            final boolean created = level.setBlock(newPosition, level.getBlockState(oldPosition), 1) &&
                level.getBlockEntity(newPosition) == proxy;
            if (created) {
                assert getBlockPos().equals(newPosition);
                level.setBlock(oldPosition, Blocks.AIR.defaultBlockState(), 1);
                level.setBlock(oldPosition, blockRobotAfterImage.defaultBlockState(), 1);
                assert level.getBlockState(oldPosition).getBlock() == blockRobotAfterImage;
                // Here instead of Lua callback so that it gets called on client, too.
                final int moveTicks = Math.max((int) (Settings.get().moveDelay * 20), 1);
                setAnimateMove(oldPosition, moveTicks);
                if (isServer()) {
                    PacketSender.sendRobotMove(this, oldPosition, direction);
                    checkRedstoneInputChanged();
                    EventBus.INSTANCE.post(new RobotMoveEvent.Post(this, direction));
                } else {
                    // If we broke some replaceable block (like grass) play its break sound.
                    if (!wasAir) {
                        if (block != Blocks.AIR && block != blockRobotAfterImage) {
                            if (!state.getFluidState().isEmpty()) {
                                level.playLocalSound(newPosition.getX() + 0.5, newPosition.getY() + 0.5, newPosition.getZ() + 0.5, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS,
                                    level.random.nextFloat() * 0.25f + 0.75f, level.random.nextFloat() * 1.0f + 0.5f, false);
                            }
                            if (!(block instanceof LiquidBlock)) {
                                level.levelEvent(2001, newPosition, Block.getId(state));
                            }
                        }
                    }
                    ExtendedWorld.notifyBlockUpdate(level, oldPosition);
                    ExtendedWorld.notifyBlockUpdate(level, newPosition);
                }
                assert !isRemoved();
            } else {
                level.setBlockAndUpdate(newPosition, Blocks.AIR.defaultBlockState());
            }
            return created && getBlockPos().equals(newPosition);
        } finally {
            blockRobotProxy.moving.set(Optional.empty());
        }
    }

    // ----------------------------------------------------------------------- //

    public boolean isAnimatingMove() {
        return animationTicksLeft > 0 && moveFrom.isPresent();
    }

    public boolean isAnimatingSwing() {
        return animationTicksLeft > 0 && swingingTool;
    }

    public boolean isAnimatingTurn() {
        return animationTicksLeft > 0 && turnAxis != 0;
    }

    public void animateSwing(double duration) {
        if (!items()[0].isEmpty()) {
            setAnimateSwing((int) (duration * 20));
            PacketSender.sendRobotAnimateSwing(this);
        }
    }

    public void animateTurn(boolean clockwise, double duration) {
        setAnimateTurn(clockwise ? 1 : -1, (int) (duration * 20));
        PacketSender.sendRobotAnimateTurn(this);
    }

    public void setAnimateMove(BlockPos fromPosition, int ticks) {
        animationTicksTotal = ticks + 2;
        prepareForAnimation();
        moveFrom = Optional.of(fromPosition);
    }

    public void setAnimateSwing(int ticks) {
        animationTicksTotal = Math.max(ticks, 5);
        prepareForAnimation();
        swingingTool = true;
    }

    public void setAnimateTurn(int axis, int ticks) {
        animationTicksTotal = ticks;
        prepareForAnimation();
        turnAxis = axis;
    }

    private void prepareForAnimation() {
        animationTicksLeft = animationTicksTotal;
        moveFrom = Optional.empty();
        swingingTool = false;
        turnAxis = 0;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        if (animationTicksLeft > 0) {
            animationTicksLeft -= 1;
            if (animationTicksLeft == 0) {
                moveFrom = Optional.empty();
                swingingTool = false;
                turnAxis = 0;
            }
        }
        super.updateEntity();
        final Level level = getLevel();
        if (isServer()) {
            if (level.getGameTime() % Settings.get().tickFrequency == 0) {
                final Connector botNode = bot.node();
                if (info.tier == 3) {
                    botNode.changeBuffer(Double.POSITIVE_INFINITY);
                }
                globalBuffer = botNode.globalBuffer();
                globalBufferSize = botNode.globalBufferSize();
                info.totalEnergy = (int) globalBuffer;
                info.robotEnergy = (int) botNode.localBuffer();
                updatePowerInformation();
            }
            if (!appliedToolEnchantments) {
                appliedToolEnchantments = true;
                final ItemStack item = getItem(0);
                if (!item.isEmpty()) {
                    player_().getAttributes().addTransientAttributeModifiers(item.getAttributeModifiers(EquipmentSlot.MAINHAND));
                }
            }
        } else if (isRunning() && isAnimatingMove()) {
            li.cil.oc.client.Sound.updatePosition(this);
        }

        for (int slot = 0; slot < equipmentInventory.getContainerSize() + mainInventory.getContainerSize(); slot++) {
            final ItemStack stack = getItem(slot);
            if (stack != null && !stack.isEmpty()) {
                try {
                    stack.inventoryTick(level, !level.isClientSide ? player_() : null, slot, slot == 0);
                } catch (NullPointerException ignored) {
                    // Client side item updates that need a player instance...
                }
            }
        }
    }

    // The robot's machine is updated in a tick handler, to avoid delayed tile
    // entity creation when moving, which would screw over all the things...
    @Override
    public void updateComputer() {
    }

    @Override
    public void onRunningChanged() {
        Computer.super.onRunningChanged();
        if (isRunning()) EventHandler.onRobotStart(this);
        else EventHandler.onRobotStopped(this);
    }

    @Override
    public void initialize() {
        if (isServer()) {
            // Ensure we have a node address, because the proxy needs this to initialize
            // its own node to the same address ours has.
            li.cil.oc.api.Network.joinNewNetwork(node());
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isClient()) {
            ClientHooks.closeGuiFor(this);
        } else EventHandler.onRobotStopped(this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        updateInventorySize();
        machine().onHostChanged();

        bot.loadData(nbt.getCompound(RobotTag));
        if (nbt.contains(OwnerTag)) {
            ownerName = nbt.getString(OwnerTag);
        }
        if (nbt.contains(OwnerUUIDTag)) {
            ownerUUID = UUID.fromString(nbt.getString(OwnerUUIDTag));
        }
        if (inventorySize > 0) {
            selectedSlot = Math.max(0, Math.min(nbt.getInt(SelectedSlotTag), mainInventory.getContainerSize() - 1));
        }
        selectedTank = nbt.getInt(SelectedTankTag);
        animationTicksTotal = nbt.getInt(AnimationTicksTotalTag);
        animationTicksLeft = nbt.getInt(AnimationTicksLeftTag);
        if (animationTicksLeft > 0) {
            if (nbt.contains(MoveFromXTag)) {
                final int moveFromX = nbt.getInt(MoveFromXTag);
                final int moveFromY = nbt.getInt(MoveFromYTag);
                final int moveFromZ = nbt.getInt(MoveFromZTag);
                moveFrom = Optional.of(new BlockPos(moveFromX, moveFromY, moveFromZ));
            }
            swingingTool = nbt.getBoolean(SwingingToolTag);
            turnAxis = nbt.getByte(TurnAxisTag);
        }

        // Normally set in superclass, but that's not called directly, only in the
        // robot's proxy instance.
        redstoneAwareState().isOutputEnabled = hasRedstoneCard();
        if (isRunning()) EventHandler.onRobotStart(this);
    }

    // Side check for Waila (and other mods that may call this client side).
    @Override
    public void saveForServer(CompoundTag nbt) {
        if (isServer()) {
            synchronized (this) {
                info.saveData(nbt);

                // Note: computer is saved when proxy is saved (in proxy's super save)
                // which is a bit ugly, and may be refactored some day, but it works.
                li.cil.oc.util.ExtendedNBT.setNewCompoundTag(nbt, RobotTag, bot::saveData);
                nbt.putString(OwnerTag, ownerName);
                nbt.putString(OwnerUUIDTag, ownerUUID.toString());
                nbt.putInt(SelectedSlotTag, selectedSlot);
                nbt.putInt(SelectedTankTag, selectedTank);
                if (isAnimatingMove() || isAnimatingSwing() || isAnimatingTurn()) {
                    nbt.putInt(AnimationTicksTotalTag, animationTicksTotal);
                    nbt.putInt(AnimationTicksLeftTag, animationTicksLeft);
                    moveFrom.ifPresent(blockPos -> {
                        nbt.putInt(MoveFromXTag, blockPos.getX());
                        nbt.putInt(MoveFromYTag, blockPos.getY());
                        nbt.putInt(MoveFromZTag, blockPos.getZ());
                    });
                    nbt.putBoolean(SwingingToolTag, swingingTool);
                    nbt.putByte(TurnAxisTag, (byte) turnAxis);
                }
            }
        }
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        loadData(nbt);
        info.loadData(nbt);

        updateInventorySize();

        selectedSlot = nbt.getInt(SelectedSlotTag);
        animationTicksTotal = nbt.getInt(AnimationTicksTotalTag);
        animationTicksLeft = nbt.getInt(AnimationTicksLeftTag);
        if (animationTicksLeft > 0) {
            if (nbt.contains(MoveFromXTag)) {
                final int moveFromX = nbt.getInt(MoveFromXTag);
                final int moveFromY = nbt.getInt(MoveFromYTag);
                final int moveFromZ = nbt.getInt(MoveFromZTag);
                moveFrom = Optional.of(new BlockPos(moveFromX, moveFromY, moveFromZ));
            }
            swingingTool = nbt.getBoolean(SwingingToolTag);
            turnAxis = nbt.getByte(TurnAxisTag);
        }
        connectComponents();
    }

    @Override
    public synchronized void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        saveData(nbt);
        info.saveData(nbt);

        nbt.putInt(SelectedSlotTag, selectedSlot);
        if (isAnimatingMove() || isAnimatingSwing() || isAnimatingTurn()) {
            nbt.putInt(AnimationTicksTotalTag, animationTicksTotal);
            nbt.putInt(AnimationTicksLeftTag, animationTicksLeft);
            moveFrom.ifPresent(blockPos -> {
                nbt.putInt(MoveFromXTag, blockPos.getX());
                nbt.putInt(MoveFromYTag, blockPos.getY());
                nbt.putInt(MoveFromZTag, blockPos.getZ());
            });
            nbt.putBoolean(SwingingToolTag, swingingTool);
            nbt.putByte(TurnAxisTag, (byte) turnAxis);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onMachineConnect(Node node) {
        Computer.super.onConnect(node);
        if (node == this.node()) {
            node.connect(bot.node());
            ((Connector) node).setLocalBufferSize(0);
        }
    }

    @Override
    public void onMachineDisconnect(Node node) {
        Computer.super.onDisconnect(node);
        if (node == this.node()) {
            node.remove();
            bot.node().remove();
            for (int slot : componentSlots()) {
                final ManagedEnvironment component = getComponentInSlot(slot);
                if (component != null) component.node().remove();
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) {
            if (isToolSlot(slot)) {
                player_().getAttributes().addTransientAttributeModifiers(stack.getAttributeModifiers(EquipmentSlot.MAINHAND));
                PacketSender.sendRobotInventory(this, slot, stack);
            }
            if (isUpgradeSlot(slot)) {
                PacketSender.sendRobotInventory(this, slot, stack);
            }
            if (isFloppySlot(slot)) {
                li.cil.oc.common.Sound.playDiskInsert(this);
            }
            if (isComponentSlot(slot, stack)) {
                Computer.super.onItemAdded(slot, stack);
                ExtendedWorld.notifyBlocksOfNeighborChange(getLevel(), position(), getBlockState().getBlock(), false);
            }
            if (isInventorySlot(slot)) {
                machine().signal("inventory_changed", slot - equipmentInventory.getContainerSize() + 1);
            }
        } else Computer.super.onItemAdded(slot, stack);
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        Computer.super.onItemRemoved(slot, stack);
        if (isServer()) {
            if (isToolSlot(slot)) {
                player_().getAttributes().removeAttributeModifiers(stack.getAttributeModifiers(EquipmentSlot.MAINHAND));
                PacketSender.sendRobotInventory(this, slot, ItemStack.EMPTY);
            }
            if (isUpgradeSlot(slot)) {
                PacketSender.sendRobotInventory(this, slot, ItemStack.EMPTY);
            }
            if (isFloppySlot(slot)) {
                li.cil.oc.common.Sound.playDiskEject(this);
            }
            if (isInventorySlot(slot)) {
                machine().signal("inventory_changed", slot - equipmentInventory.getContainerSize() + 1);
            }
            if (isComponentSlot(slot, stack)) {
                ExtendedWorld.notifyBlocksOfNeighborChange(getLevel(), position(), getBlockState().getBlock(), false);
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // Avoid getting into a bad state on the client when updating before we
        // got the descriptor packet from the server. If we manage to open the
        // GUI before the descriptor packet arrived, close it again because it is
        // invalid anyway.
        if (inventorySize >= 0) {
            updateInventorySize();
        } else if (isClient()) {
            ClientHooks.closeGuiFor(this);
        }
        renderingErrored = false;
    }

    @Override
    public void connectItemNode(Node node) {
        Computer.super.connectItemNode(node);
        if (node != null) {
            if (node.host() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                for (int slot : componentSlots()) {
                    final ManagedEnvironment component = getComponentInSlot(slot);
                    if (component instanceof li.cil.oc.api.internal.Keyboard keyboard) buffer.node().connect(keyboard.node());
                    else if (component instanceof li.cil.oc.server.component.GraphicsCard gpu) buffer.node().connect(gpu.node());
                }
            } else if (node.host() instanceof li.cil.oc.api.internal.Keyboard keyboard) {
                for (int slot : componentSlots()) {
                    if (getComponentInSlot(slot) instanceof li.cil.oc.api.internal.TextBuffer buffer) keyboard.node().connect(buffer.node());
                }
            }
        }
    }

    @Override
    public boolean isComponentSlot(int slot, ItemStack stack) {
        return isContainerSlot(slot) || (slot >= getContainerSize() - componentCount() && slot < getContainerSize());
    }

    public String containerSlotType(int slot) {
        if (isContainerSlot(slot)) {
            final ItemStack stack = info.containers[slot - 1];
            final DriverItem driver = Driver.driverFor(stack, getClass());
            if (driver instanceof li.cil.oc.api.driver.item.Container container) return container.providedSlot(stack);
        }
        return Slot.None;
    }

    public int containerSlotTier(int slot) {
        if (isContainerSlot(slot)) {
            final ItemStack stack = info.containers[slot - 1];
            final DriverItem driver = Driver.driverFor(stack, getClass());
            if (driver instanceof li.cil.oc.api.driver.item.Container container) return container.providedTier(stack);
        }
        return Tier.None;
    }

    public boolean isToolSlot(int slot) {
        return slot == 0;
    }

    public boolean isContainerSlot(int slot) {
        return slot >= 1 && slot <= info.containers.length;
    }

    public boolean isInventorySlot(int slot) {
        final int start = equipmentInventory.getContainerSize();
        return slot >= start && slot < start + mainInventory.getContainerSize();
    }

    public boolean isFloppySlot(int slot) {
        final ItemStack stack = getItem(slot);
        if (stack.isEmpty() || !isComponentSlot(slot, stack)) return false;
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return driver != null && Slot.Floppy.equals(driver.slot(stack));
    }

    public boolean isUpgradeSlot(int slot) {
        return Slot.Upgrade.equals(containerSlotType(slot));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int componentSlot(String address) {
        final Optional<ManagedEnvironment>[] components = components();
        for (int i = 0; i < components.length; i++) {
            final Optional<ManagedEnvironment> env = components[i];
            if (env.isPresent() && env.get().node() != null && address.equals(env.get().node().address())) return i;
        }
        return -1;
    }

    private List<Integer> containerAndComponentSlots() {
        final List<Integer> result = containerSlots();
        result.addAll(componentSlots());
        return result;
    }

    @Override
    public boolean hasRedstoneCard() {
        for (int slot : containerAndComponentSlots()) {
            final ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && DriverRedstoneCard.INSTANCE.worksWith(stack, getClass())) return true;
        }
        return false;
    }

    private int computeInventorySize() {
        int acc = 0;
        for (int slot : containerAndComponentSlots()) {
            final ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                final DriverItem driver = Driver.driverFor(stack, getClass());
                if (driver instanceof li.cil.oc.api.driver.item.Inventory inventory) {
                    acc += inventory.inventoryCapacity(stack);
                }
            }
        }
        return Math.min(maxInventorySize(), acc);
    }

    public void updateInventorySize() {
        synchronized (this) {
            if (!updatingInventorySize) {
                try {
                    updatingInventorySize = true;
                    final int newInventorySize = computeInventorySize();
                    if (newInventorySize != inventorySize) {
                        inventorySize = newInventorySize;
                        final int realSize = equipmentInventory.getContainerSize() + mainInventory.getContainerSize();
                        final int oldSelected = selectedSlot;
                        final List<ItemStack> removed = new ArrayList<>();
                        for (int slot = realSize; slot < getContainerSize() - componentCount(); slot++) {
                            final ItemStack stack = getItem(slot);
                            setItem(slot, ItemStack.EMPTY);
                            if (!stack.isEmpty()) removed.add(stack);
                        }
                        final Optional<ManagedEnvironment>[] components = components();
                        final int copyComponentCount = Math.min(getContainerSize(), componentCount());
                        System.arraycopy(components, getContainerSize() - copyComponentCount, components, realSize, copyComponentCount);
                        for (int slot = Math.max(0, getContainerSize() - componentCount()); slot < getContainerSize(); slot++) {
                            if (slot < realSize || slot >= realSize + componentCount()) {
                                components[slot] = Optional.empty();
                            }
                        }
                        containerSize = realSize + componentCount();
                        if (getLevel() != null && isServer()) {
                            for (ItemStack stack : removed) {
                                player().getInventory().add(stack);
                                spawnStackInWorld(stack, Optional.ofNullable(facing()));
                            }
                            setSelectedSlot(oldSelected);
                        } // else: save is screwed and we potentially lose items. Life is hard.
                    }
                } finally {
                    updatingInventorySize = false;
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getContainerSize() {
        return containerSize;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public ItemStack getItem(int slot) {
        // TODO(port): Scala returned null here ("Required to always show 16 inventory slots in GUI");
        //  1.20 code does not tolerate null stacks, so the container must handle slots beyond the size.
        if (slot >= getContainerSize()) return ItemStack.EMPTY;
        else if (slot >= getContainerSize() - componentCount()) {
            return info.components[slot - (getContainerSize() - componentCount())];
        } else return Computer.super.getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < getContainerSize() - componentCount() && (canPlaceItem(slot, stack) || stack.isEmpty())) {
            if (!stack.isEmpty() && stack.getCount() > 1 && isComponentSlot(slot, stack)) {
                Computer.super.setItem(slot, stack.split(1));
                if (stack.getCount() > 0 && isServer()) {
                    player().getInventory().add(stack);
                    spawnStackInWorld(stack, Optional.ofNullable(facing()));
                }
            } else Computer.super.setItem(slot, stack);
        } else if (!stack.isEmpty() && stack.getCount() > 0 && !getLevel().isClientSide) {
            spawnStackInWorld(stack, Optional.of(Direction.UP));
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return Computer.super.stillValid(player) && (!isCreative() || player.isCreative());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        if (slot == 0) return true; // Allow anything in the tool slot.
        if (driver != null && isContainerSlot(slot)) {
            // Yay special cases! Dynamic screens kind of work, but are pretty derpy
            // because the item gets send around on changes, including the screen
            // state, which leads to weird effects. Also, it's really illogical that
            // a screen (and keyboard) could be attached to the robot on the fly.
            // Since these are very special (as they have special behavior in the
            // GUI) I feel it's OK to handle it like this, instead of some extra API
            // logic making the differentiation of assembler and containers generic.
            return driver != DriverScreen.INSTANCE &&
                driver != DriverKeyboard.INSTANCE &&
                driver.slot(stack).equals(containerSlotType(slot)) &&
                driver.tier(stack) <= containerSlotTier(slot);
        }
        if (isInventorySlot(slot)) return true; // Normal inventory.
        return false; // Invalid slot.
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new li.cil.oc.common.container.Robot(ContainerTypes.ROBOT.get(), id, playerInventory, this, new li.cil.oc.common.container.RobotInfo(this));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void forAllLoot(Consumer<ItemStack> dst) {
        final ItemStack tool = getItem(0);
        if (tool != null && tool.getCount() > 0) dst.accept(tool);
        for (int slot : containerSlots()) {
            final ItemStack stack = getItem(slot);
            if (stack != null && stack.getCount() > 0) dst.accept(stack);
        }
        InventoryUtils.forAllSlots(mainInventory, dst);
    }

    @Override
    public boolean dropSlot(int slot, int count, Optional<Direction> direction) {
        return InventoryUtils.dropSlot(new BlockPosition(x(), y(), z(), getLevel()), mainInventory, slot, count, direction);
    }

    @Override
    public void dropAllSlots() {
        InventoryUtils.dropSlot(new BlockPosition(x(), y(), z(), getLevel()), this, 0, Integer.MAX_VALUE);
        for (int slot : containerSlots()) {
            InventoryUtils.dropSlot(new BlockPosition(x(), y(), z(), getLevel()), this, slot, Integer.MAX_VALUE);
        }
        InventoryUtils.dropAllSlots(new BlockPosition(x(), y(), z(), getLevel()), mainInventory);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        for (int s : getSlotsForFace(side)) if (s == slot) return true;
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canTakeItemThroughFace(slot, stack, side) && canPlaceItem(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        final Direction local = toLocal(side);
        final List<Integer> slots;
        if (local == Direction.WEST) slots = List.of(0); // Tool
        else if (local == Direction.EAST) slots = containerSlots();
        else slots = inventorySlots();
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    // ----------------------------------------------------------------------- //

    public Optional<FluidHandler> tryGetTank(int tank) {
        final List<FluidHandler> tanks = new ArrayList<>();
        for (Optional<ManagedEnvironment> component : components()) {
            if (component.isPresent() && component.get() instanceof FluidHandler fluidTank) tanks.add(fluidTank);
        }
        if (tank < 0 || tank >= tanks.size()) return Optional.empty();
        else return Optional.ofNullable(tanks.get(tank));
    }

    public int tankCount() {
        int count = 0;
        for (Optional<ManagedEnvironment> component : components()) {
            if (component.isPresent() && component.get() instanceof FluidHandler) count++;
        }
        return count;
    }

    public FluidHandler getFluidTank(int tank) {
        return tryGetTank(tank).orElse(null);
    }

    // ----------------------------------------------------------------------- //
    // FluidHandler (operates on the selected tank, amounts in mB).

    @Override
    public int getTanks() {
        return tankCount();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tryGetTank(selectedTank).map(t -> t.getFluidInTank(0)).orElse(FluidStack.empty());
    }

    @Override
    public long getTankCapacity(int tank) {
        return tryGetTank(selectedTank).map(t -> t.getTankCapacity(0)).orElse(0L);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public long fill(FluidStack resource, boolean simulate) {
        return tryGetTank(selectedTank).map(t -> t.fill(resource, simulate)).orElse(0L);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        final Optional<FluidHandler> t = tryGetTank(selectedTank);
        if (t.isPresent()) {
            final FluidStack fluid = t.get().getFluidInTank(0);
            if (fluid != null && !fluid.isEmpty() && fluid.isFluidEqual(resource)) {
                return t.get().drain(resource.getAmount(), simulate);
            }
        }
        return FluidStack.empty();
    }

    @Override
    public FluidStack drain(long maxDrain, boolean simulate) {
        return tryGetTank(selectedTank).map(t -> t.drain(maxDrain, simulate)).orElse(FluidStack.empty());
    }

    // ----------------------------------------------------------------------- //

    /** Client-only code, in a separate class so it is never loaded on a dedicated server. */
    private static final class ClientHooks {
        static void closeGuiFor(Robot robot) {
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Robot robotGui
                && robotGui.inventoryContainer.otherInventory == robot) {
                robotGui.onClose();
            }
        }
    }
}
