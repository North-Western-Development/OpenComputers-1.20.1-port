package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Machine;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.ComponentInventory;
import li.cil.oc.common.inventory.ItemStackInventory;
import li.cil.oc.common.inventory.ServerInventory;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Server implements Environment, MachineHost, ServerInventory, ComponentInventory, Analyzable, li.cil.oc.api.internal.Server, DeviceInfo {
    public final Rack rack;

    public final int slot;

    private li.cil.oc.api.machine.Machine machine;

    public final Node node;

    public boolean wasRunning = false;
    public boolean hadErrored = false;
    public long lastFileSystemAccess = 0L;
    public long lastNetworkActivity = 0L;

    private final ItemStackInventory.ItemsHolder itemsHolder = new ItemStackInventory.ItemsHolder();

    private final ComponentInventory.ComponentState componentState = new ComponentInventory.ComponentState();

    public Server(Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
        this.node = !rack.world().isClientSide ? machine().node() : null;
    }

    @Override
    public li.cil.oc.api.machine.Machine machine() {
        if (machine == null) {
            machine = Machine.create(this);
        }
        return machine;
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Rack rack() {
        return rack;
    }

    @Override
    public int slot() {
        return slot;
    }

    @Override
    public ItemStack[] items() {
        return itemsHolder.get(this);
    }

    @Override
    public ComponentInventory.ComponentState componentState() {
        return componentState;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Server",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Blader",
                DeviceAttribute.Capacity, String.valueOf(getContainerSize())
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //
    // Environment

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            connectComponents();
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == this.node) {
            disconnectComponents();
        }
    }

    @Override
    public void onMessage(Message message) {
    }

    private static final String MachineTag = "machine";

    @Override
    public void loadData(CompoundTag nbt) {
        ComponentInventory.super.loadData(nbt);
        if (!rack.world().isClientSide) {
            machine().loadData(nbt.getCompound(MachineTag));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        ComponentInventory.super.saveData(nbt);
        if (!rack.world().isClientSide) {
            ExtendedNBT.setNewCompoundTag(nbt, MachineTag, machine()::saveData);
        }
    }

    // ----------------------------------------------------------------------- //
    // MachineHost

    @Override
    public Iterable<ItemStack> internalComponents() {
        final List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            final ItemStack stack = getItem(i);
            if (!stack.isEmpty() && isComponentSlot(i, stack)) result.add(stack);
        }
        return result;
    }

    @Override
    public int componentSlot(String address) {
        final Optional<ManagedEnvironment>[] components = components();
        for (int i = 0; i < components.length; i++) {
            final Optional<ManagedEnvironment> env = components[i];
            if (env.isPresent() && env.get().node() != null && address.equals(env.get().node().address())) return i;
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
    // EnvironmentHost

    @Override
    public double xPosition() {
        return rack.xPosition();
    }

    @Override
    public double yPosition() {
        return rack.yPosition();
    }

    @Override
    public double zPosition() {
        return rack.zPosition();
    }

    @Override
    public Level world() {
        return rack.world();
    }

    @Override
    public void markChanged() {
        rack.markChanged();
    }

    // ----------------------------------------------------------------------- //
    // ServerInventory

    @Override
    public int rackSlot() {
        return slot;
    }

    @Override
    public int tier() {
        if (container().getItem() instanceof li.cil.oc.common.item.Server server) return server.tier;
        return 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return rack.stillValid(player) && rack.indexOfMountable(this) >= 0;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    // ----------------------------------------------------------------------- //
    // ItemStackInventory

    @Override
    public Rack host() {
        return rack;
    }

    // ----------------------------------------------------------------------- //
    // ComponentInventory

    @Override
    public ItemStack container() {
        return rack.getItem(slot);
    }

    @Override
    public void connectItemNode(Node node) {
        if (node != null) {
            Network.joinNewNetwork(machine().node());
            machine().node().connect(node);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        ComponentInventory.super.onItemRemoved(slot, stack);
        if (!rack.world().isClientSide) {
            final String slotType = InventorySlots.server[tier()][slot].slot;
            if (Slot.CPU.equals(slotType)) {
                machine().stop();
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // RackMountable

    @Override
    public CompoundTag getData() {
        final CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isRunning", wasRunning);
        nbt.putBoolean("hasErrored", hadErrored);
        nbt.putLong("lastFileSystemAccess", lastFileSystemAccess);
        nbt.putLong("lastNetworkActivity", lastNetworkActivity);
        return nbt;
    }

    @Override
    public int getConnectableCount() {
        int count = 0;
        for (Optional<ManagedEnvironment> component : components()) {
            if (component.isPresent() && component.get() instanceof RackBusConnectable) count++;
        }
        return count;
    }

    @Override
    public RackBusConnectable getConnectableAt(int index) {
        final List<RackBusConnectable> connectables = new ArrayList<>();
        for (Optional<ManagedEnvironment> component : components()) {
            if (component.isPresent() && component.get() instanceof RackBusConnectable busConnectable) connectables.add(busConnectable);
        }
        return connectables.get(index);
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (!player.level().isClientSide) {
            if (player.isCrouching()) {
                if (!machine().isRunning() && stillValid(player)) {
                    wasRunning = false;
                    hadErrored = false;
                    machine().start();
                }
            }
            else if (player instanceof ServerPlayer srvPlr) {
                ContainerTypes.openServerGui(srvPlr, this, slot);
            }
        }
        return true;
    }

    // ----------------------------------------------------------------------- //
    // ManagedEnvironment

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (!rack.world().isClientSide) {
            machine().update();

            final boolean isRunning = machine().isRunning();
            final boolean hasErrored = machine().lastError() != null;
            if (isRunning != wasRunning || hasErrored != hadErrored) {
                rack.markChanged(slot);
            }
            wasRunning = isRunning;
            hadErrored = hasErrored;
            if (tier() == Tier.Four) ((Connector) node).changeBuffer(Double.POSITIVE_INFINITY);
        }

        updateComponents();
    }

    // ----------------------------------------------------------------------- //
    // StateAware

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (machine().isRunning()) return EnumSet.of(StateAware.State.IsWorking);
        else return EnumSet.noneOf(StateAware.State.class);
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{machine().node()};
    }

    // ----------------------------------------------------------------------- //
    // ICapabilityProvider

    // TODO(port): the 1.16 server forwarded Forge capability lookups (with the side
    //  converted via host.toLocal(facing)) to its components implementing
    //  ICapabilityProvider. There are no capabilities on 1.20/Architectury; if the rack
    //  needs this, it has to query the components directly.
}
