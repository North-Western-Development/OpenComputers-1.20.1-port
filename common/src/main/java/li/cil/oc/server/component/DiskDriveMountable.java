package li.cil.oc.server.component;

import dev.architectury.registry.menu.MenuRegistry;
import li.cil.oc.Constants;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Sound;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.ComponentInventory;
import li.cil.oc.common.inventory.ItemStackInventory;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static li.cil.oc.util.ResultWrapper.result;

public class DiskDriveMountable extends AbstractManagedEnvironment implements ItemStackInventory, ComponentInventory, RackMountable, Analyzable, DeviceInfo, MenuProvider {
    public final Rack rack;
    public final int slot;

    public final Component node;

    // State of the ItemStackInventory / ComponentInventory traits.
    private final ItemStackInventory.ItemsHolder itemsHolder = new ItemStackInventory.ItemsHolder();
    private final ComponentInventory.ComponentState componentState = new ComponentInventory.ComponentState();

    // Stored for filling data packet when queried.
    public long lastAccess = 0L;

    public DiskDriveMountable(Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
        this.node = (Component) Network.newNode(this, Visibility.Network).
                withComponent("disk_drive").
                create();
        setNode(node);
    }

    @Override
    public Component node() {
        return node;
    }

    @Override
    public ItemStack[] items() {
        return itemsHolder.get(this);
    }

    @Override
    public ComponentInventory.ComponentState componentState() {
        return componentState;
    }

    public Optional<Node> filesystemNode() {
        final Optional<ManagedEnvironment>[] components = components();
        if (components.length > 0 && components[0].isPresent()) return Optional.ofNullable(components[0].get().node());
        return Optional.empty();
    }

    // ----------------------------------------------------------------------- //
    // DeviceInfo

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Disk);
            info.put(DeviceAttribute.Description, "Floppy disk drive");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "RackDrive 100 Rev. 2");
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //
    // Environment

    @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
    public Object[] isEmpty(Context context, Arguments args) {
        return result(filesystemNode().isEmpty());
    }

    @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
    public Object[] eject(Context context, Arguments args) {
        final double velocity = Math.min(Math.max(args.optDouble(0, 0), 0), 1);
        final ItemStack ejected = removeItem(0, 1);
        if (!ejected.isEmpty()) {
            final ItemEntity entity = InventoryUtils.spawnStackInWorld(BlockPosition.apply(rack), ejected, Optional.ofNullable(rack.facing()));
            if (entity != null) {
                final double vx = rack.facing().getStepX() * velocity;
                final double vy = rack.facing().getStepY() * velocity;
                final double vz = rack.facing().getStepZ() * velocity;
                entity.push(vx, vy, vz);
            }
            return result(true);
        } else return result(false);
    }

    @Callback(doc = "function(): string -- Return the internal floppy disk address")
    public Object[] media(Context context, Arguments args) {
        final Optional<Node> fs = filesystemNode();
        if (fs.isEmpty()) return result(null, "drive is empty");
        else return result(fs.get().address());
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return filesystemNode().map(n -> new Node[]{n}).orElse(null);
    }

    // ----------------------------------------------------------------------- //
    // ItemStackInventory

    @Override
    public EnvironmentHost host() {
        return rack;
    }

    // ----------------------------------------------------------------------- //
    // Container

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0) return false;
        final DriverItem driver = Driver.driverFor(stack);
        return driver != null && Slot.Floppy.equals(driver.slot(stack));
    }

    @Override
    public boolean stillValid(Player player) {
        return rack.stillValid(player);
    }

    // ----------------------------------------------------------------------- //
    // ComponentInventory

    @Override
    public ItemStack container() {
        return rack.getItem(slot);
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        ComponentInventory.super.onItemAdded(slot, stack);
        final Optional<ManagedEnvironment>[] components = components();
        if (slot >= 0 && slot < components.length && components[slot].isPresent()) {
            if (components[slot].get().node() instanceof Component component) {
                component.setVisibility(Visibility.Network);
            }
        }
        if (!rack.world().isClientSide()) {
            rack.markChanged(this.slot);
            Sound.playDiskInsert(rack);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        ComponentInventory.super.onItemRemoved(slot, stack);
        if (!rack.world().isClientSide()) {
            rack.markChanged(this.slot);
            Sound.playDiskEject(rack);
        }
    }

    // ----------------------------------------------------------------------- //
    // ManagedEnvironment

    @Override
    public boolean canUpdate() {
        return false;
    }

    // ----------------------------------------------------------------------- //
    // Persistable

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        ComponentInventory.super.loadData(nbt);
        connectComponents();
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        ComponentInventory.super.saveData(nbt);
    }

    // ----------------------------------------------------------------------- //
    // RackMountable

    @Override
    public CompoundTag getData() {
        final CompoundTag nbt = new CompoundTag();
        nbt.putLong("lastAccess", lastAccess);
        nbt.put("disk", ExtendedNBT.toNbt(getItem(0)));
        return nbt;
    }

    @Override
    public int getConnectableCount() {
        return 0;
    }

    @Override
    public RackBusConnectable getConnectableAt(int index) {
        return null;
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (player.isCrouching()) {
            final boolean isDiskInDrive = !getItem(0).isEmpty();
            final boolean isHoldingDisk = canPlaceItem(0, heldItem);
            if (isDiskInDrive) {
                if (!rack.world().isClientSide()) {
                    InventoryUtils.dropSlot(BlockPosition.apply(rack), this, 0, 1, Optional.ofNullable(rack.facing()));
                }
            }
            if (isHoldingDisk) {
                // Insert the disk.
                setItem(0, player.getInventory().removeItem(player.getInventory().selected, 1));
            }
            return isDiskInDrive || isHoldingDisk;
        } else if (player instanceof ServerPlayer srvPlr) {
            MenuRegistry.openExtendedMenu(srvPlr, this, buf -> {
            });
            return true;
        } else return false;
    }

    // ----------------------------------------------------------------------- //
    // MenuProvider

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.DiskDrive(ContainerTypes.DISK_DRIVE.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //
    // StateAware

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        return EnumSet.noneOf(StateAware.State.class);
    }
}
