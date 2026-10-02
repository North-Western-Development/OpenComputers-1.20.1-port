package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.traits.ComponentInventory;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.OpenSides;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Adapter extends TileEntity implements Environment, ComponentInventory, Tickable, OpenSides, Analyzable, li.cil.oc.api.internal.Adapter, DeviceInfo, MenuProvider {
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network).create();

    @SuppressWarnings("unchecked")
    private final Optional<Pair<ManagedEnvironment, DriverBlock>>[] blocks = new Optional[6];

    private final List<ManagedEnvironment> updatingBlocks = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final Optional<BlockData>[] blocksData = new Optional[6];

    private Map<String, String> deviceInfo;

    private static final String BlocksTag = Settings.namespace + "adapter.blocks";
    private static final String BlockNameTag = "name";
    private static final String BlockDataTag = "data";

    public Adapter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < 6; i++) {
            blocks[i] = Optional.empty();
            blocksData[i] = Optional.empty();
        }
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Bus,
                DeviceAttribute.Description, "Adapter",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Multiplug Ext.1"
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean defaultState() {
        return true;
    }

    @Override
    public void setSideOpen(Direction side, boolean value) {
        OpenSides.super.setSideOpen(side, value);
        final Level level = getLevel();
        if (isServer()) {
            PacketSender.sendAdapterState(this);
            level.playSound(null, getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.25f + 0.7f);
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            neighborChanged(side);
        } else {
            final BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        final List<Node> result = new ArrayList<>();
        for (Optional<Pair<ManagedEnvironment, DriverBlock>> block : blocks) {
            block.ifPresent(b -> result.add(b.getLeft().node()));
        }
        for (Optional<ManagedEnvironment> component : components()) {
            component.ifPresent(c -> result.add(c.node()));
        }
        return result.toArray(new Node[0]);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer() && !updatingBlocks.isEmpty()) {
            for (ManagedEnvironment block : new ArrayList<>(updatingBlocks)) {
                block.update();
            }
        }
    }

    public void neighborChanged(Direction d) {
        if (node != null && node.network() != null) {
            final Level level = getLevel();
            final BlockPos blockPos = getBlockPos().relative(d);
            if (level.getBlockEntity(blockPos) instanceof Environment) {
                // Don't provide adaption for our stuffs. This is mostly to avoid
                // cables and other non-functional stuff popping up in the adapter
                // due to having a power interface. Might revisit this at some point,
                // but the only 'downside' is that it can't be used to manipulate
                // inventories, which I actually consider a plus :P
                return;
            }
            final DriverBlock newDriver = Driver.driverFor(level, blockPos, d);
            final int index = d.ordinal();
            if (newDriver != null && isSideOpen(d)) {
                final Optional<Pair<ManagedEnvironment, DriverBlock>> existing = blocks[index];
                if (existing.isPresent()) {
                    final ManagedEnvironment oldEnvironment = existing.get().getLeft();
                    final DriverBlock driver = existing.get().getRight();
                    if (newDriver != driver) {
                        // This is... odd. Maybe moved by some other mod? First, clean up.
                        blocks[index] = Optional.empty();
                        updatingBlocks.remove(oldEnvironment);
                        blocksData[index] = Optional.empty();
                        node.disconnect(oldEnvironment.node());

                        // Then rebuild - if we have something.
                        final ManagedEnvironment environment = newDriver.createEnvironment(level, blockPos, d);
                        if (environment != null) {
                            blocks[index] = Optional.of(Pair.of(environment, newDriver));
                            if (environment.canUpdate()) {
                                updatingBlocks.add(environment);
                            }
                            blocksData[index] = Optional.of(new BlockData(environment.getClass().getName(), new CompoundTag()));
                            node.connect(environment.node());
                        }
                    } // else: the more things change, the more they stay the same.
                } else {
                    if (!isSideOpen(d)) {
                        return;
                    }
                    // A challenger appears. Maybe.
                    final ManagedEnvironment environment = newDriver.createEnvironment(level, blockPos, d);
                    if (environment != null) {
                        blocks[index] = Optional.of(Pair.of(environment, newDriver));
                        if (environment.canUpdate()) {
                            updatingBlocks.add(environment);
                        }
                        final Optional<BlockData> data = blocksData[index];
                        if (data.isPresent() && data.get().name.equals(environment.getClass().getName())) {
                            environment.loadData(data.get().data);
                        }
                        blocksData[index] = Optional.of(new BlockData(environment.getClass().getName(), new CompoundTag()));
                        node.connect(environment.node());
                    }
                }
            } else {
                final Optional<Pair<ManagedEnvironment, DriverBlock>> existing = blocks[index];
                if (existing.isPresent()) {
                    final ManagedEnvironment environment = existing.get().getLeft();
                    // We had something there, but it's gone now...
                    node.disconnect(environment.node());
                    environment.saveData(blocksData[index].get().data);
                    if (environment.node() != null) environment.node().remove();
                    blocks[index] = Optional.empty();
                    updatingBlocks.remove(environment);
                } // else: Nothing before, nothing now.
            }
        }
    }

    public void neighborChanged() {
        if (node != null && node.network() != null) {
            for (Direction d : Direction.values()) {
                neighborChanged(d);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onConnect(Node node) {
        ComponentInventory.super.onConnect(node);
        if (node == this.node) {
            neighborChanged();
        }
    }

    @Override
    public void onDisconnect(Node node) {
        ComponentInventory.super.onDisconnect(node);
        if (node == this.node) {
            updatingBlocks.clear();
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return slot == 0 && driver != null && Slot.Upgrade.equals(driver.slot(stack));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Adapter(ContainerTypes.ADAPTER.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);

        final ListTag blocksNbt = nbt.getList(BlocksTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(blocksNbt.size(), blocksData.length); i++) {
            final CompoundTag blockNbt = blocksNbt.getCompound(i);
            if (blockNbt.contains(BlockNameTag) && blockNbt.contains(BlockDataTag)) {
                blocksData[i] = Optional.of(new BlockData(blockNbt.getString(BlockNameTag), blockNbt.getCompound(BlockDataTag)));
            }
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);

        final ListTag blocksNbt = new ListTag();
        for (int i = 0; i < blocks.length; i++) {
            final CompoundTag blockNbt = new CompoundTag();
            final Optional<BlockData> data = blocksData[i];
            if (data.isPresent()) {
                blocks[i].ifPresent(block -> block.getLeft().saveData(data.get().data));
                blockNbt.putString(BlockNameTag, data.get().name);
                blockNbt.put(BlockDataTag, data.get().data);
            }
            blocksNbt.add(blockNbt);
        }
        nbt.put(BlocksTag, blocksNbt);
    }

    // ----------------------------------------------------------------------- //

    private static final class BlockData {
        final String name;
        final CompoundTag data;

        BlockData(String name, CompoundTag data) {
            this.name = name;
            this.data = data;
        }
    }
}
