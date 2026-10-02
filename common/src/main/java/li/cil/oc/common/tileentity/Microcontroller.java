package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.tileentity.traits.Computer;
import li.cil.oc.common.tileentity.traits.Hub;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Microcontroller extends TileEntity implements PowerAcceptor, Hub, Computer, WorldlyContainer, li.cil.oc.api.internal.Microcontroller, DeviceInfo {
    public final MicrocontrollerData info = new MicrocontrollerData();

    public final boolean[] outputSides = new boolean[]{true, true, true, true, true, true};

    public final ComponentConnector snooperNode = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("microcontroller").
        withConnector(Settings.get().bufferMicrocontroller).
        create();

    public final Component[] componentNodes = new Component[6];

    private Map<String, String> deviceInfo;

    private static final String InfoTag = Settings.namespace + "info";
    private static final String OutputsTag = Settings.namespace + "outputs";
    private static final String ComponentNodesTag = Settings.namespace + "componentNodes";
    private static final String SnooperTag = Settings.namespace + "snooper";

    public Microcontroller(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < componentNodes.length; i++) {
            componentNodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.Network).
                withComponent("microcontroller").
                create();
        }
        if (machine() != null) {
            ((Connector) machine().node()).setLocalBufferSize(0);
            machine().setCostPerTick(Settings.get().microcontrollerCost);
        }
    }

    @Override
    public Node node() {
        return null;
    }

    @Override
    public int tier() {
        return info.tier;
    }

    @Override
    public Optional<String> runSound() {
        return Optional.empty(); // Microcontrollers are silent.
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Microcontroller",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Cubicle",
                DeviceAttribute.Capacity, String.valueOf(getContainerSize())
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    /** Client side only. */
    @Override
    public boolean canConnect(Direction side) {
        return side != facing();
    }

    @Override
    public Node sidedNode(Direction side) {
        return side != facing() ? Hub.super.sidedNode(side) : null;
    }

    @Override
    public boolean hasConnector(Direction side) {
        return side != facing();
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return Optional.ofNullable(side != facing() ? snooperNode : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().caseRate[Tier.One];
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        Computer.super.onAnalyze(player, side, hitX, hitY, hitZ);
        if (side != facing())
            return new Node[]{componentNodes[side.get3DDataValue()]};
        else
            return new Node[]{machine().node()};
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Iterable<ItemStack> internalComponents() {
        return Arrays.asList(info.components);
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

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():boolean -- Starts the microcontroller. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) {
        return result(!machine().isPaused() && machine().start());
    }

    @Callback(doc = "function():boolean -- Stops the microcontroller. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) {
        return result(machine().stop());
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the microcontroller is running.")
    public Object[] isRunning(Context context, Arguments args) {
        return result(machine().isRunning());
    }

    @Callback(direct = true, doc = "function():string -- Returns the reason the microcontroller crashed, if applicable.")
    public Object[] lastError(Context context, Arguments args) {
        return result(machine().lastError());
    }

    @Callback(direct = true, doc = "function(side:number):boolean -- Get whether network messages are sent via the specified side.")
    public Object[] isSideOpen(Context context, Arguments args) {
        final Direction side = ExtendedArguments.checkSideExcept(args, 0, facing());
        return result(outputSides[side.ordinal()]);
    }

    @Callback(doc = "function(side:number, open:boolean):boolean -- Set whether network messages are sent via the specified side.")
    public Object[] setSideOpen(Context context, Arguments args) {
        final Direction side = ExtendedArguments.checkSideExcept(args, 0, facing());
        final boolean oldValue = outputSides[side.ordinal()];
        outputSides[side.ordinal()] = args.checkBoolean(1);
        return result(oldValue);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();

        // Pump energy into the internal network.
        if (isServer() && getLevel().getGameTime() % Settings.get().tickFrequency == 0) {
            for (Direction side : Direction.values()) {
                if (side == facing()) continue;
                if (sidedNode(side) instanceof Connector connector) {
                    final double demand = snooperNode.globalBufferSize() - snooperNode.globalBuffer();
                    final double available = demand + connector.changeBuffer(-demand);
                    snooperNode.changeBuffer(available);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void connectItemNode(Node node) {
        if (machine() != null && machine().node() != null && node != null) {
            li.cil.oc.api.Network.joinNewNetwork(machine().node());
            machine().node().connect(node);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node createNode(Hub.Plug plug) {
        return li.cil.oc.api.Network.newNode(plug, Visibility.Network).
            withConnector().
            create();
    }

    @Override
    public void onPlugConnect(Hub.Plug plug, Node node) {
        Hub.super.onPlugConnect(plug, node);
        if (node == plug.node) {
            li.cil.oc.api.Network.joinNewNetwork(machine().node());
            machine().node().connect(snooperNode);
            connectComponents();
        }
        if (plug.isPrimary())
            plug.node.connect(componentNodes[plug.side.ordinal()]);
        else
            componentNodes[plug.side.ordinal()].remove();
    }

    @Override
    public void onPlugDisconnect(Hub.Plug plug, Node node) {
        Hub.super.onPlugDisconnect(plug, node);
        if (plug.isPrimary() && node != plug.node)
            plug.node.connect(componentNodes[plug.side.ordinal()]);
        else
            componentNodes[plug.side.ordinal()].remove();
        if (node == plug.node)
            disconnectComponents();
    }

    @Override
    public void onPlugMessage(Hub.Plug plug, Message message) {
        if ("network.message".equals(message.name()) && message.source().network() != snooperNode.network()) {
            snooperNode.sendToReachable(message.name(), message.data());
        }
    }

    @Override
    public void onMessage(Message message) {
        if ("network.message".equals(message.name()) && message.source().network() == snooperNode.network()) {
            for (Direction side : Direction.values()) {
                if (outputSides[side.ordinal()] && side != facing()) {
                    sidedNode(side).sendToReachable(message.name(), message.data());
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        // Load info before inventory and such, to avoid initializing components
        // to empty inventory.
        info.loadData(nbt.getCompound(InfoTag));
        // Port note: the result was discarded in the Scala code as well.
        ExtendedNBT.getBooleanArray(nbt, OutputsTag);
        final CompoundTag[] tags = ExtendedNBT.toTagArray(nbt.getList(ComponentNodesTag, Tag.TAG_COMPOUND), CompoundTag.class);
        for (int index = 0; index < tags.length && index < componentNodes.length; index++) {
            componentNodes[index].loadData(tags[index]);
        }
        snooperNode.loadData(nbt.getCompound(SnooperTag));
        super.loadForServer(nbt);
        li.cil.oc.api.Network.joinNewNetwork(machine().node());
        machine().node().connect(snooperNode);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, InfoTag, info::saveData);
        ExtendedNBT.setBooleanArray(nbt, OutputsTag, outputSides);
        final List<CompoundTag> tags = new ArrayList<>();
        for (Node node : componentNodes) {
            final CompoundTag tag = new CompoundTag();
            if (node != null) node.saveData(tag);
            tags.add(tag);
        }
        ExtendedNBT.setNewTagList(nbt, ComponentNodesTag, tags);
        ExtendedNBT.setNewCompoundTag(nbt, SnooperTag, snooperNode::saveData);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        info.loadData(nbt.getCompound(InfoTag));
        super.loadForClient(nbt);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, InfoTag, info::saveData);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public ItemStack[] items() {
        return info.components;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        info.components[slot] = stack;
    }

    @Override
    public int getContainerSize() {
        return info.components.length;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    // Nope.
    @Override
    public void setItem(int slot, ItemStack stack) {
    }

    // Nope.
    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    // Nope.
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    // Nope.
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    // For hotswapping EEPROMs.
    public ItemStack changeEEPROM(ItemStack newEeprom) {
        final li.cil.oc.api.detail.ItemInfo eeprom = li.cil.oc.api.Items.get(Constants.ItemName.EEPROM);
        int oldEepromIndex = -1;
        for (int i = 0; i < info.components.length; i++) {
            if (li.cil.oc.api.Items.get(info.components[i]) == eeprom) {
                oldEepromIndex = i;
                break;
            }
        }
        if (oldEepromIndex >= 0) {
            final ItemStack oldEeprom = info.components[oldEepromIndex];
            Computer.super.setItem(oldEepromIndex, newEeprom);
            return oldEeprom;
        } else {
            assert info.components[getContainerSize() - 1].isEmpty();
            Computer.super.setItem(getContainerSize() - 1, newEeprom);
            return ItemStack.EMPTY;
        }
    }

    // Uses the loot system, so still nope.
    @Override
    public void forAllLoot(Consumer<ItemStack> dst) {
    }

    // Nope.
    @Override
    public boolean dropSlot(int slot, int count, Optional<Direction> direction) {
        return false;
    }

    // Nope.
    @Override
    public void dropAllSlots() {
    }
}
