package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.block.property.PropertyRunning;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.traits.Colored;
import li.cil.oc.common.tileentity.traits.Computer;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.util.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

public class Case extends TileEntity implements PowerAcceptor, Computer, Colored, li.cil.oc.api.internal.Case, DeviceInfo, MenuProvider {
    public int tier;

    // Used on client side to check whether to render disk activity/network indicators.
    public long lastFileSystemAccess = 0L;
    public long lastNetworkActivity = 0L;

    private Map<String, String> deviceInfo;

    private static final String TierTag = Settings.namespace + "tier";

    public Case(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier) {
        super(type, pos, state);
        this.tier = tier;
        setColor(Color.rgbValues.get(Color.byTier[tier]));
    }

    public Case(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 0);
        // If no tier was defined when constructing this case, then we don't yet know the inventory size
        // this is set back to true when the nbt data is loaded
        setIsSizeInventoryReady(false);
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Computer",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Blocker",
                DeviceAttribute.Capacity, String.valueOf(getContainerSize())
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasConnector(Direction side) {
        return side != facing();
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return Optional.ofNullable(side != facing() && machine() != null ? (Connector) machine().node() : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().caseRate[tier];
    }

    public boolean isCreative() {
        return tier == Tier.Four;
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

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        if (isServer() && isCreative() && getLevel().getGameTime() % Settings.get().tickFrequency == 0) {
            // Creative case, make it generate power.
            ((Connector) node()).changeBuffer(Double.POSITIVE_INFINITY);
        }
        super.updateEntity();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onRunningChanged() {
        Computer.super.onRunningChanged();
        if (getBlockState().getBlock() instanceof li.cil.oc.common.block.Case block) {
            final Level level = getLevel();
            final BlockState state = level.getBlockState(getBlockPos());
            // race condition that the world no longer has this block at the position (e.g. it was broken)
            if (block == state.getBlock()) {
                level.setBlockAndUpdate(getBlockPos(), state.setValue(PropertyRunning.Running, isRunning()));
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        tier = Math.min(Math.max(nbt.getByte(TierTag), 0), 3);
        setColor(Color.rgbValues.get(Color.byTier[tier]));
        super.loadForServer(nbt);
        setIsSizeInventoryReady(true);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        nbt.putByte(TierTag, (byte) tier);
        super.saveForServer(nbt);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        Computer.super.onItemAdded(slot, stack);
        if (isServer()) {
            if (Slot.Floppy.equals(InventorySlots.computer[tier][slot].slot)) {
                li.cil.oc.common.Sound.playDiskInsert(this);
            }
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        Computer.super.onItemRemoved(slot, stack);
        if (isServer()) {
            final String slotType = InventorySlots.computer[tier][slot].slot;
            if (Slot.Floppy.equals(slotType)) {
                li.cil.oc.common.Sound.playDiskEject(this);
            }
            if (Slot.CPU.equals(slotType)) {
                machine().stop();
            }
        }
    }

    @Override
    public int getContainerSize() {
        return tier < 0 || tier >= InventorySlots.computer.length ? 0 : InventorySlots.computer[tier].length;
    }

    @Override
    public boolean stillValid(Player player) {
        return Computer.super.stillValid(player) && (!isCreative() || player.isCreative());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        if (driver == null) return false;
        final InventorySlots.InventorySlot provided = InventorySlots.computer[tier][slot];
        return driver.slot(stack).equals(provided.slot) && driver.tier(stack) <= provided.tier;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Case(ContainerTypes.CASE.get(), id, playerInventory, this, tier);
    }
}
