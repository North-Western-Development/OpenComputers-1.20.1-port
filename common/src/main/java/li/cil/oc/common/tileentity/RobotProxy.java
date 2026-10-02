package li.cil.oc.common.tileentity;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.inventory.InventoryProxy;
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.common.tileentity.traits.Computer;
import li.cil.oc.common.tileentity.traits.PowerInformation;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.RotatableTile;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.agent.Player;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The block entity actually placed in the world for robots; forwards to its {@link #robot}.
 * <p>
 * Port note: instead of overriding the Scala {@code _input/_output/_bundled*} arrays, the proxy
 * returns its robot's {@link #redstoneAwareState()}, {@link #bundledRedstoneAwareState()} and
 * {@link #rotatableTileState()}.
 */
public class RobotProxy extends TileEntity implements Computer, PowerInformation, RotatableTile, WorldlyContainer, FluidHandler, li.cil.oc.api.internal.Robot {
    public final Robot robot;

    public final Component node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("robot", Visibility.Neighbors).
        create();

    public RobotProxy(BlockEntityType<?> type, BlockPos pos, BlockState state, Robot robot) {
        super(type, pos, state);
        this.robot = robot;
    }

    public RobotProxy(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, new Robot(pos, state));
    }

    // ----------------------------------------------------------------------- //

    // TODO(port): the Forge FLUID_HANDLER capability of the proxy is not exposed (see Robot).

    @Override
    public Component node() {
        return node;
    }

    @Override
    public Machine machine() {
        return robot.machine();
    }

    @Override
    public int tier() {
        return robot.tier();
    }

    @Override
    public InventoryProxy equipmentInventory() {
        return robot.equipmentInventory;
    }

    @Override
    public InventoryProxy mainInventory() {
        return robot.mainInventory;
    }

    @Override
    public MultiTank tank() {
        return robot.tank;
    }

    @Override
    public int selectedSlot() {
        return robot.selectedSlot;
    }

    @Override
    public void setSelectedSlot(int index) {
        robot.setSelectedSlot(index);
    }

    @Override
    public int selectedTank() {
        return robot.selectedTank;
    }

    @Override
    public void setSelectedTank(int index) {
        robot.setSelectedTank(index);
    }

    @Override
    public Player player() {
        return robot.player();
    }

    @Override
    public String name() {
        return robot.name();
    }

    @Override
    public void setName(String name) {
        robot.setName(name);
    }

    @Override
    public String ownerName() {
        return robot.ownerName;
    }

    @Override
    public UUID ownerUUID() {
        return robot.ownerUUID;
    }

    // ----------------------------------------------------------------------- //
    // Shared trait state.

    @Override
    public RedstoneAware.State redstoneAwareState() {
        return robot.redstoneAwareState();
    }

    @Override
    public BundledRedstoneAware.State bundledRedstoneAwareState() {
        return robot.bundledRedstoneAwareState();
    }

    @Override
    public RotatableTile.State rotatableTileState() {
        return robot.rotatableTileState();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void connectComponents() {
    }

    @Override
    public void disconnectComponents() {
    }

    @Override
    public boolean isRunning() {
        return robot.isRunning();
    }

    @Override
    public void setRunning(boolean value) {
        robot.setRunning(value);
    }

    @Override
    public boolean shouldAnimate() {
        return robot.shouldAnimate();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int componentCount() {
        return robot.componentCount();
    }

    @Override
    public ManagedEnvironment getComponentInSlot(int index) {
        return robot.getComponentInSlot(index);
    }

    @Override
    public void synchronizeSlot(int slot) {
        robot.synchronizeSlot(slot);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():boolean -- Starts the robot. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) {
        return result(!machine().isPaused() && machine().start());
    }

    @Callback(doc = "function():boolean -- Stops the robot. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) {
        return result(machine().stop());
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the robot is running.")
    public Object[] isRunning(Context context, Arguments args) {
        return result(machine().isRunning());
    }

    @Callback(doc = "function(name: string):string -- Sets a new name and returns the old name. Robot must not be running")
    public Object[] setName(Context context, Arguments args) {
        final String oldName = robot.name();
        final String newName = args.checkString(0);
        if (machine().isRunning()) return result(ResultWrapper.unit, "is running");
        setName(newName);
        PacketSender.sendRobotNameChange(robot);
        return result(oldName);
    }

    @Callback(doc = "function():string -- Returns the robot name.")
    public Object[] getName(Context context, Arguments args) {
        return result(robot.name());
    }

    @Override
    public void onMessage(Message message) {
        Computer.super.onMessage(message);
        if ("network.message".equals(message.name()) && message.source() != this.node) {
            final Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof Packet packet) {
                robot.node().sendToReachable(message.name(), packet);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        robot.updateEntity();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        final boolean firstProxy = robot.proxy == null;
        robot.proxy = this;
        robot.setLevelAndPosition(getLevel(), getBlockPos());
        if (firstProxy) {
            robot.clearRemoved();
        }
        if (isServer()) {
            // Use the same address we use internally on the outside.
            final CompoundTag nbt = new CompoundTag();
            nbt.putString("address", robot.node().address());
            node.loadData(nbt);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (robot.proxy == this) {
            robot.dispose();
        }
    }

    @Override
    public void loadForServer(CompoundTag nbt) {
        robot.info.loadData(nbt);
        super.loadForServer(nbt);
        robot.loadForServer(nbt);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        robot.saveForServer(nbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        robot.saveData(nbt);
    }

    @Override
    public void loadData(CompoundTag nbt) {
        robot.loadData(nbt);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        robot.loadForClient(nbt);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        robot.saveForClient(nbt);
    }

    @Override
    public void setChanged() {
        robot.setChanged();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node[] onAnalyze(net.minecraft.world.entity.player.Player player, Direction side, float hitX, float hitY, float hitZ) {
        return robot.onAnalyze(player, side, hitX, hitY, hitZ);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isOutputEnabled() {
        return robot.isOutputEnabled();
    }

    @Override
    public RedstoneAware setOutputEnabled(boolean value) {
        return robot.setOutputEnabled(value);
    }

    @Override
    public void checkRedstoneInputChanged() {
        robot.checkRedstoneInputChanged();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Direction pitch() {
        return robot.pitch();
    }

    @Override
    public void setPitch(Direction value) {
        robot.setPitch(value);
    }

    @Override
    public Direction yaw() {
        return robot.yaw();
    }

    @Override
    public void setYaw(Direction value) {
        robot.setYaw(value);
    }

    @Override
    public boolean setFromEntityPitchAndYaw(Entity entity) {
        return robot.setFromEntityPitchAndYaw(entity);
    }

    @Override
    public boolean setFromFacing(Direction value) {
        return robot.setFromFacing(value);
    }

    @Override
    public boolean invertRotation() {
        return robot.invertRotation();
    }

    @Override
    public Direction facing() {
        return robot.facing();
    }

    @Override
    public boolean rotate(Direction axis) {
        return robot.rotate(axis);
    }

    @Override
    public Direction toLocal(Direction value) {
        return robot.toLocal(value);
    }

    @Override
    public Direction toGlobal(Direction value) {
        return robot.toGlobal(value);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public ItemStack getItem(int i) {
        return robot.getItem(i);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return robot.removeItem(slot, amount);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        robot.setItem(slot, stack);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return robot.removeItemNoUpdate(slot);
    }

    @Override
    public void startOpen(net.minecraft.world.entity.player.Player player) {
        robot.startOpen(player);
    }

    @Override
    public void stopOpen(net.minecraft.world.entity.player.Player player) {
        robot.stopOpen(player);
    }

    @Override
    public boolean hasCustomName() {
        return robot.hasCustomName();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return robot.stillValid(player);
    }

    @Override
    public boolean isEmpty() {
        return robot.isEmpty();
    }

    @Override
    public void forAllLoot(Consumer<ItemStack> dst) {
        robot.forAllLoot(dst);
    }

    @Override
    public boolean dropSlot(int slot, int count, Optional<Direction> direction) {
        return robot.dropSlot(slot, count, direction);
    }

    @Override
    public void dropAllSlots() {
        robot.dropAllSlots();
    }

    @Override
    public int getMaxStackSize() {
        return robot.getMaxStackSize();
    }

    @Override
    public int componentSlot(String address) {
        return robot.componentSlot(address);
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        return robot.getName();
    }

    @Override
    public int getContainerSize() {
        return robot.getContainerSize();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return robot.canPlaceItem(slot, stack);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return robot.canTakeItemThroughFace(slot, stack, side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return robot.canPlaceItemThroughFace(slot, stack, side);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return robot.getSlotsForFace(side);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasRedstoneCard() {
        return robot.hasRedstoneCard();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double globalBuffer() {
        return robot.globalBuffer;
    }

    @Override
    public void setGlobalBuffer(double value) {
        robot.globalBuffer = value;
    }

    @Override
    public double globalBufferSize() {
        return robot.globalBufferSize;
    }

    @Override
    public void setGlobalBufferSize(double value) {
        robot.globalBufferSize = value;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getTanks() {
        return robot.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return robot.getFluidInTank(tank);
    }

    @Override
    public long getTankCapacity(int tank) {
        return robot.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack resource) {
        return robot.isFluidValid(tank, resource);
    }

    @Override
    public long fill(FluidStack resource, boolean simulate) {
        return robot.fill(resource, simulate);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        return robot.drain(resource, simulate);
    }

    @Override
    public FluidStack drain(long maxDrain, boolean simulate) {
        return robot.drain(maxDrain, simulate);
    }
}
