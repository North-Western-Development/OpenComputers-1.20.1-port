package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Sound;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.traits.ComponentInventory;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

public class DiskDrive extends TileEntity implements Environment, ComponentInventory, Rotatable, Analyzable, DeviceInfo, MenuProvider {
    // Used on client side to check whether to render disk activity indicators.
    public long lastAccess = 0L;

    private Map<String, String> deviceInfo;

    private static final String DiskTag = Settings.namespace + "disk";

    // ----------------------------------------------------------------------- //
    // Environment

    public final Component node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("disk_drive").
        create();

    public DiskDrive(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Component node() {
        return node;
    }

    public Optional<Node> filesystemNode() {
        final Optional<ManagedEnvironment>[] components = components();
        if (components.length > 0 && components[0].isPresent()) return Optional.ofNullable(components[0].get().node());
        return Optional.empty();
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Disk,
                DeviceAttribute.Description, "Floppy disk drive",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Spinner 520p1"
            );
        }
        return deviceInfo;
    }

    @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
    public Object[] isEmpty(Context context, Arguments args) {
        return result(filesystemNode().isEmpty());
    }

    @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
    public Object[] eject(Context context, Arguments args) {
        final double velocity = Math.min(Math.max(args.optDouble(0, 0), 0), 1);
        final ItemStack ejected = removeItem(0, 1);
        if (!ejected.isEmpty()) {
            final Direction facing = facing();
            final ItemEntity entity = InventoryUtils.spawnStackInWorld(position(), ejected, Optional.ofNullable(facing));
            if (entity != null) {
                final double vx = facing.getStepX() * velocity;
                final double vy = facing.getStepY() * velocity;
                final double vz = facing.getStepZ() * velocity;
                entity.push(vx, vy, vz);
            }
            return result(true);
        } else return result(false);
    }

    @Callback(doc = "function(): string -- Return the internal floppy disk address")
    public Object[] media(Context context, Arguments args) {
        final Optional<Node> node = filesystemNode();
        if (node.isEmpty())
            return result(ResultWrapper.unit, "drive is empty");
        else
            return result(node.get().address());
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return filesystemNode().map(node -> new Node[]{node}).orElse(null);
    }

    // ----------------------------------------------------------------------- //
    // Container

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return slot == 0 && driver != null && Slot.Floppy.equals(driver.slot(stack));
    }

    // ----------------------------------------------------------------------- //
    // MenuProvider

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.DiskDrive(ContainerTypes.DISK_DRIVE.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //
    // ComponentInventory

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        ComponentInventory.super.onItemAdded(slot, stack);
        final Optional<ManagedEnvironment>[] components = components();
        if (slot < components.length && components[slot].isPresent()) {
            if (components[slot].get().node() instanceof Component component) {
                component.setVisibility(Visibility.Network);
            }
        }
        if (isServer()) {
            PacketSender.sendFloppyChange(this, stack);
            Sound.playDiskInsert(this);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        ComponentInventory.super.onItemRemoved(slot, stack);
        if (isServer()) {
            PacketSender.sendFloppyChange(this);
            Sound.playDiskEject(this);
        }
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        if (nbt.contains(DiskTag)) {
            setItem(0, ItemStack.of(nbt.getCompound(DiskTag)));
        }
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        if (!items()[0].isEmpty()) ExtendedNBT.setNewCompoundTag(nbt, DiskTag, items()[0]::save);
    }
}
