package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.common.item.data.NodeData;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.component.FileSystem;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class Raid extends TileEntity implements Environment, li.cil.oc.common.tileentity.traits.Inventory, Rotatable, Analyzable, MenuProvider {
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.None).create();

    public Optional<FileSystem> filesystem = Optional.empty();

    public final RaidLabel label = new RaidLabel();

    // Used on client side to check whether to render disk activity indicators.
    public long lastAccess = 0L;

    // For client side rendering.
    public final boolean[] presence = new boolean[getContainerSize()];

    private static final String FileSystemTag = Settings.namespace + "fs";
    private static final String PresenceTag = Settings.namespace + "presence";
    private static final String LabelTag = Settings.namespace + "label";

    public Raid(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Node node() {
        return node;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{filesystem.map(FileSystem::node).orElse(null)};
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return driver != null && Slot.HDD.equals(driver.slot(stack));
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        li.cil.oc.common.tileentity.traits.Inventory.super.onItemAdded(slot, stack);
        if (isServer()) {
            synchronized (this) {
                PacketSender.sendRaidChange(this);
                tryCreateRaid(UUID.randomUUID().toString());
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // Makes the implementation of the comparator output easier.
        final ItemStack[] items = items();
        for (int i = 0; i < items.length && i < presence.length; i++) {
            presence[i] = !items[i].isEmpty();
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        li.cil.oc.common.tileentity.traits.Inventory.super.onItemRemoved(slot, stack);
        if (isServer()) {
            synchronized (this) {
                PacketSender.sendRaidChange(this);
                filesystem.ifPresent(fs -> {
                    fs.fileSystem.close();
                    for (String file : fs.fileSystem.list("/")) {
                        fs.fileSystem.delete(file);
                    }
                    fs.saveData(new CompoundTag()); // Flush buffered fs.
                    fs.node().remove();
                    filesystem = Optional.empty();
                });
            }
        }
    }

    // Uses the loot system, so nope.
    @Override
    public void forAllLoot(Consumer<ItemStack> dst) {
    }

    @Override
    public boolean dropSlot(int slot, int count, Optional<Direction> direction) {
        return false;
    }

    @Override
    public void dropAllSlots() {
    }

    public void tryCreateRaid(String id) {
        final ItemStack[] items = items();
        int count = 0;
        for (ItemStack stack : items) if (!stack.isEmpty()) count++;
        if (count == items.length && filesystem.map(fs -> fs.node() == null || !id.equals(fs.node().address())).orElse(true)) {
            filesystem.ifPresent(fs -> {
                if (fs.node() != null) fs.node().remove();
            });
            for (ItemStack fsStack : items) {
                final DriveData drive = new DriveData(fsStack);
                drive.lockInfo = "";
                drive.isUnmanaged = false;
                drive.saveData(fsStack);
            }
            final FileSystem fs = (FileSystem) li.cil.oc.api.FileSystem.asManagedEnvironment(
                li.cil.oc.api.FileSystem.fromSaveDirectory(id, wipeDisksAndComputeSpace(), Settings.get().bufferChanges),
                label, this, Settings.resourceDomain + ":hdd_access", 6);
            final CompoundTag nbtToSetAddress = new CompoundTag();
            nbtToSetAddress.putString(NodeData.AddressTag, id);
            fs.node().loadData(nbtToSetAddress);
            ((li.cil.oc.api.network.Component) fs.node()).setVisibility(Visibility.Network);
            // Ensure we're in a network before connecting the raid fs.
            li.cil.oc.api.Network.joinNewNetwork(node);
            node.connect(fs.node());
            filesystem = Optional.of(fs);
        }
    }

    private long wipeDisksAndComputeSpace() {
        long acc = 0L;
        for (ItemStack hdd : items()) {
            if (hdd.isEmpty()) continue;
            final DriverItem driver = Driver.driverFor(hdd);
            if (driver == null) continue;
            final ManagedEnvironment environment = driver.createEnvironment(hdd, this);
            if (environment instanceof FileSystem fs) {
                final CompoundTag nbt = driver.dataTag(hdd);
                fs.loadData(nbt);
                fs.fileSystem.close();
                for (String file : fs.fileSystem.list("/")) {
                    fs.fileSystem.delete(file);
                }
                fs.saveData(nbt);
                acc += (int) fs.fileSystem.spaceTotal();
            }
        }
        return acc;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Raid(ContainerTypes.RAID.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        if (nbt.contains(FileSystemTag)) {
            final CompoundTag tag = nbt.getCompound(FileSystemTag);
            tryCreateRaid(tag.getCompound(NodeData.NodeTag).getString(NodeData.AddressTag));
            filesystem.ifPresent(fs -> fs.loadData(tag));
        }
        label.loadData(nbt);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        filesystem.ifPresent(fs -> ExtendedNBT.setNewCompoundTag(nbt, FileSystemTag, fs::saveData));
        label.saveData(nbt);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        final byte[] data = nbt.getByteArray(PresenceTag);
        for (int i = 0; i < data.length && i < presence.length; i++) {
            presence[i] = data[i] != 0;
        }
        label.setLabel(nbt.getString(LabelTag));
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        final ItemStack[] items = items();
        final boolean[] present = new boolean[items.length];
        for (int i = 0; i < items.length; i++) present[i] = !items[i].isEmpty();
        nbt.put(PresenceTag, ExtendedNBT.toNbt(present));
        if (label.getLabel() != null)
            nbt.putString(LabelTag, label.getLabel());
    }

    // ----------------------------------------------------------------------- //

    public static class RaidLabel implements Label {
        public String label = "raid";

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public void setLabel(String value) {
            label = value == null ? null : (value.length() > 16 ? value.substring(0, 16) : value);
        }

        @Override
        public void loadData(CompoundTag nbt) {
            if (nbt.contains(Settings.namespace + "label")) {
                label = nbt.getString(Settings.namespace + "label");
            }
        }

        @Override
        public void saveData(CompoundTag nbt) {
            nbt.putString(Settings.namespace + "label", label);
        }
    }
}
