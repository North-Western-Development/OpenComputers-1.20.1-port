package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.ComponentInventory;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.integration.opencomputers.DriverScreen;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.RotationHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TabletWrapper implements ComponentInventory, MachineHost, li.cil.oc.api.internal.Tablet, MenuProvider {
    public ItemStack stack;

    public Player player;

    // Remember our *original* world, so we know which tablets to clear on dimension
    // changes of players holding tablets - since the player entity instance may be
    // kept the same and components are not required to properly handle world changes.
    public final Level world;

    private li.cil.oc.api.machine.Machine machine;
    private boolean machineInitialized;

    public final TabletData data = new TabletData();

    public final li.cil.oc.server.component.Tablet tablet;

    //// Client side only
    private boolean isInitialized;

    public int timesChanged = 0;

    public boolean isDirty = true;
    ////

    // Server side only
    private boolean lastRunning = false;

    public boolean autoSave = true;
    ////

    private final ComponentInventory.ComponentState componentState = new ComponentInventory.ComponentState();

    public TabletWrapper(ItemStack stack, Player player) {
        this.stack = stack;
        this.player = player;
        this.world = player.level();
        this.tablet = world.isClientSide ? null : new li.cil.oc.server.component.Tablet(this);
        this.isInitialized = !world.isClientSide;

        readFromNBT();
        if (!world.isClientSide) {
            li.cil.oc.api.Network.joinNewNetwork(machine().node());
            final double charge = Math.max(0, this.data.energy - ((Connector) tablet.node()).globalBuffer());
            ((Connector) tablet.node()).changeBuffer(charge);
            writeToNBT();
        }
    }

    @Override
    public li.cil.oc.api.machine.Machine machine() {
        if (!machineInitialized) {
            machineInitialized = true;
            machine = world.isClientSide ? null : Machine.create(this);
        }
        return machine;
    }

    @Override
    public ComponentInventory.ComponentState componentInventoryState() {
        return componentState;
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public Level world() {
        return world;
    }

    public boolean isCreative() {
        return data.tier == Tier.Four;
    }

    @Override
    public ItemStack[] items() {
        return data.items;
    }

    @Override
    public Direction facing() {
        return RotationHelper.fromYaw(player.getYRot());
    }

    @Override
    public Direction toLocal(Direction value) {
        return RotationHelper.toLocal(Direction.NORTH, facing(), value);
    }

    @Override
    public Direction toGlobal(Direction value) {
        return RotationHelper.toGlobal(Direction.NORTH, facing(), value);
    }

    public void readFromNBT() {
        if (stack.hasTag()) {
            final CompoundTag data = stack.getTag();
            loadData(data);
            if (!world.isClientSide) {
                tablet.loadData(data.getCompound(Settings.namespace + "component"));
                machine().loadData(data.getCompound(Settings.namespace + "data"));
            }
        }
    }

    public void writeToNBT() {
        writeToNBT(true);
    }

    public void writeToNBT(boolean clearState) {
        final CompoundTag data = stack.getOrCreateTag();
        if (!world.isClientSide) {
            if (!data.contains(Settings.namespace + "data")) {
                data.put(Settings.namespace + "data", new CompoundTag());
            }
            ExtendedNBT.setNewCompoundTag(data, Settings.namespace + "component", tablet::saveData);
            ExtendedNBT.setNewCompoundTag(data, Settings.namespace + "data", machine()::saveData);

            if (clearState) {
                // Force tablets into stopped state to avoid errors when trying to
                // load deleted machine states.
                data.getCompound(Settings.namespace + "data").remove("state");
            }
        }
        saveData(data);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Tablet(ContainerTypes.TABLET.get(), id, playerInventory, stack, this, containerSlotType(), containerSlotTier());
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onConnect(Node node) {
        if (node == this.node()) {
            connectComponents();
            node.connect(tablet.node());
        } else if (node.host() instanceof TextBuffer buffer) {
            buffer.setMaximumColorDepth(TextBuffer.ColorDepth.FourBit);
            buffer.setMaximumResolution(80, 25);
        }
    }

    @Override
    public void connectItemNode(Node node) {
        ComponentInventory.super.connectItemNode(node);
        if (node != null) {
            if (node.host() instanceof TextBuffer buffer) {
                for (Optional<ManagedEnvironment> component : components()) {
                    if (component.isPresent() && component.get() instanceof Keyboard keyboard) {
                        buffer.node().connect(keyboard.node());
                    }
                }
            } else if (node.host() instanceof Keyboard keyboard) {
                for (Optional<ManagedEnvironment> component : components()) {
                    if (component.isPresent() && component.get() instanceof TextBuffer buffer) {
                        keyboard.node().connect(buffer.node());
                    }
                }
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == this.node()) {
            disconnectComponents();
            tablet.node().remove();
        }
    }

    @Override
    public void onMessage(Message message) {
    }

    @Override
    public EnvironmentHost host() {
        return this;
    }

    @Override
    public int getContainerSize() {
        return items().length;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != getContainerSize() - 1) return false;
        final DriverItem driver = Driver.driverFor(stack, getClass());
        // Same special cases, similar as in robot, but allow keyboards,
        // because clip-on keyboards kinda seem to make sense, I guess.
        return driver != null &&
            driver != DriverScreen.INSTANCE &&
            driver.slot(stack).equals(containerSlotType()) &&
            driver.tier(stack) <= containerSlotTier();
    }

    @Override
    public boolean stillValid(Player player) {
        return machine() != null && machine().canInteract(player.getName().getString());
    }

    @Override
    public void setChanged() {
        data.saveData(stack);
        player.getInventory().setChanged();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double xPosition() {
        return player.getX();
    }

    @Override
    public double yPosition() {
        return player.getY() + player.getEyeHeight();
    }

    @Override
    public double zPosition() {
        return player.getZ();
    }

    @Override
    public void markChanged() {
    }

    // ----------------------------------------------------------------------- //

    public String containerSlotType() {
        if (data.container.isEmpty()) return Slot.None;
        final DriverItem driver = Driver.driverFor(data.container, getClass());
        if (driver instanceof Container container) return container.providedSlot(data.container);
        return Slot.None;
    }

    public int containerSlotTier() {
        if (data.container.isEmpty()) return Tier.None;
        final DriverItem driver = Driver.driverFor(data.container, getClass());
        if (driver instanceof Container container) return container.providedTier(data.container);
        return Tier.None;
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        final List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && isComponentSlot(slot, stack)) result.add(stack);
        }
        return result;
    }

    @Override
    public int componentSlot(String address) {
        final Optional<ManagedEnvironment>[] components = components();
        for (int i = 0; i < components.length; i++) {
            if (components[i].isPresent()) {
                final ManagedEnvironment env = components[i].get();
                if (env.node() != null && address.equals(env.node().address())) return i;
            }
        }
        return -1;
    }

    @Override
    public void onMachineConnect(Node node) {
        onConnect(node);
    }

    @Override
    public void onMachineDisconnect(Node node) {
        onDisconnect(node);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node node() {
        final li.cil.oc.api.machine.Machine m = machine();
        return m != null ? m.node() : null;
    }

    // ----------------------------------------------------------------------- //

    public void update(Level world, Player player, int slot, boolean selected) {
        this.player = player;
        if (!isInitialized) {
            isInitialized = true;
            // This delayed initialization on the client side is required to allow
            // the server to set up the tablet wrapper first (since packets generated
            // in the component setup would otherwise be queued before the events that
            // caused this wrapper's initialization).
            connectComponents();
            for (Optional<ManagedEnvironment> component : components()) {
                if (component.isPresent() && component.get() instanceof TextBuffer buffer) {
                    buffer.setMaximumColorDepth(TextBuffer.ColorDepth.FourBit);
                    buffer.setMaximumResolution(80, 25);
                }
            }

            li.cil.oc.client.PacketSender.sendMachineItemStateRequest(stack);
        }
        if (!world.isClientSide) {
            if (isCreative() && world.getGameTime() % Settings.get().tickFrequency == 0) {
                ((Connector) machine().node()).changeBuffer(Double.POSITIVE_INFINITY);
            }
            machine().update();
            updateComponents();
            data.isRunning = machine().isRunning();
            data.energy = ((Connector) tablet.node()).globalBuffer();
            data.maxEnergy = ((Connector) tablet.node()).globalBufferSize();

            if (lastRunning != machine().isRunning()) {
                lastRunning = machine().isRunning();
                setChanged();

                if (player instanceof ServerPlayer mp) {
                    li.cil.oc.server.PacketSender.sendMachineItemState(mp, stack, machine().isRunning());
                }

                if (machine().isRunning()) {
                    for (Optional<ManagedEnvironment> component : components()) {
                        if (component.isPresent() && component.get() instanceof TextBuffer buffer) {
                            buffer.setPowerState(true);
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadData(CompoundTag nbt) {
        data.loadData(nbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        saveComponents();
        data.saveData(nbt);
    }
}
