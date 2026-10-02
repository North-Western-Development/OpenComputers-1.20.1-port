package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.traits.ComponentInventory;
import li.cil.oc.common.tileentity.traits.Hub;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.integration.opencomputers.DriverLinkedCard;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.network.QuantumNetwork;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoublePredicate;

public class Relay extends TileEntity implements Hub, ComponentInventory, PowerAcceptor, Analyzable, WirelessEndpoint, QuantumNetwork.QuantumNode, MenuProvider {
    private ItemInfo wirelessNetworkCardTier1;
    private ItemInfo wirelessNetworkCardTier2;
    private ItemInfo linkedCard;

    public ItemInfo WirelessNetworkCardTier1() {
        if (wirelessNetworkCardTier1 == null) wirelessNetworkCardTier1 = li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier1);
        return wirelessNetworkCardTier1;
    }

    public ItemInfo WirelessNetworkCardTier2() {
        if (wirelessNetworkCardTier2 == null) wirelessNetworkCardTier2 = li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier2);
        return wirelessNetworkCardTier2;
    }

    public ItemInfo LinkedCard() {
        if (linkedCard == null) linkedCard = li.cil.oc.api.Items.get(Constants.ItemName.LinkedCard);
        return linkedCard;
    }

    public int wirelessTier = -1;

    public double strength = maxWirelessRange();

    public boolean isRepeater = true;

    public boolean isLinkedEnabled = false;

    public String tunnel = "creative";

    public final Component[] componentNodes = new Component[6];

    public final Map<Object, Set<Integer>> openPorts = new HashMap<>();

    public long lastMessage = 0L;

    // TODO(port): integration - filled by the ComputerCraft integration (parked).
    public final List<Object> computers = new ArrayList<>();

    private static final String StrengthTag = Settings.namespace + "strength";
    private static final String IsRepeaterTag = Settings.namespace + "isRepeater";
    private static final String ComponentNodesTag = Settings.namespace + "componentNodes";

    public Relay(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < componentNodes.length; i++) {
            componentNodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.Network).
                withComponent("relay").
                create();
        }
    }

    public boolean isWirelessEnabled() {
        return wirelessTier >= Tier.One;
    }

    public double maxWirelessRange() {
        return wirelessTier == Tier.One || wirelessTier == Tier.Two ? Settings.get().maxWirelessRange[wirelessTier] : 0;
    }

    public double wirelessCostPerRange() {
        return wirelessTier == Tier.One || wirelessTier == Tier.Two ? Settings.get().wirelessCostPerRange[wirelessTier] : 0;
    }

    @Override
    public String tunnel() {
        return tunnel;
    }

    public void onSwitchActivity() {
        final long now = System.currentTimeMillis();
        if (now - lastMessage >= (relayDelay() - 1) * 50L) {
            lastMessage = now;
            PacketSender.sendSwitchActivity(this);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasConnector(Direction side) {
        return true;
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return sidedNode(side) instanceof Connector connector ? Optional.of(connector) : Optional.empty();
    }

    @Override
    public double energyThroughput() {
        return Settings.get().accessPointRate;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        if (isWirelessEnabled()) {
            player.sendSystemMessage(Localization.Analyzer.WirelessStrength(strength));
            return new Node[]{componentNodes[side.get3DDataValue()]};
        } else return null;
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when relaying messages.")
    public synchronized Object[] getStrength(Context context, Arguments args) {
        return result(strength);
    }

    @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when relaying messages.")
    public synchronized Object[] setStrength(Context context, Arguments args) {
        strength = Math.max(args.checkDouble(0), Math.min(0, maxWirelessRange()));
        return result(strength);
    }

    @Callback(direct = true, doc = "function():boolean -- Get whether the access point currently acts as a repeater (resend received wireless packets wirelessly).")
    public synchronized Object[] isRepeater(Context context, Arguments args) {
        return result(isRepeater);
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Set whether the access point should act as a repeater.")
    public synchronized Object[] setRepeater(Context context, Arguments args) {
        isRepeater = args.checkBoolean(0);
        return result(isRepeater);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void receivePacket(Packet packet, WirelessEndpoint source) {
        if (isWirelessEnabled()) {
            tryEnqueuePacket(Optional.empty(), packet);
        }
    }

    @Override
    public void receivePacket(Packet packet) {
        if (isLinkedEnabled) {
            tryEnqueuePacket(Optional.empty(), packet);
        }
    }

    @Override
    public boolean tryEnqueuePacket(Optional<Direction> sourceSide, Packet packet) {
        // TODO(port): integration - ComputerCraft modem message forwarding (RelayCCAdapter) was dropped.
        return Hub.super.tryEnqueuePacket(sourceSide, packet);
    }

    @Override
    public void relayPacket(Optional<Direction> sourceSide, Packet packet) {
        Hub.super.relayPacket(sourceSide, packet);

        final DoublePredicate tryChangeBuffer;
        if (sourceSide.isPresent()) {
            final Direction side = sourceSide.get();
            tryChangeBuffer = amount -> ((Connector) plugs()[side.ordinal()].node).tryChangeBuffer(amount);
        } else {
            tryChangeBuffer = amount -> {
                for (Hub.Plug plug : plugs()) {
                    if (((Connector) plug.node).tryChangeBuffer(amount)) return true;
                }
                return false;
            };
        }

        if (isWirelessEnabled() && strength > 0 && (sourceSide.isPresent() || isRepeater)) {
            final double cost = wirelessCostPerRange();
            if (tryChangeBuffer.test(-strength * cost)) {
                li.cil.oc.api.Network.sendWirelessPacket(this, strength, packet);
            }
        }

        if (isLinkedEnabled && sourceSide.isPresent()) {
            final double cost = packet.size() / 32.0 + wirelessCostPerRange() * maxWirelessRange() * 5;
            if (tryChangeBuffer.test(-cost)) {
                for (QuantumNetwork.QuantumNode endpoint : QuantumNetwork.getEndpoints(tunnel)) {
                    if (endpoint != this) {
                        endpoint.receivePacket(packet);
                    }
                }
            }
        }

        onSwitchActivity();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Connector createNode(Hub.Plug plug) {
        return li.cil.oc.api.Network.newNode(plug, Visibility.Network).
            withConnector(Math.round(Settings.get().bufferAccessPoint)).
            create();
    }

    @Override
    public void onPlugConnect(Hub.Plug plug, Node node) {
        Hub.super.onPlugConnect(plug, node);
        if (node == plug.node) {
            li.cil.oc.api.Network.joinWirelessNetwork(this);
        }
        if (plug.isPrimary())
            plug.node.connect(componentNodes[plug.side.ordinal()]);
        else
            componentNodes[plug.side.ordinal()].remove();
    }

    @Override
    public void onPlugDisconnect(Hub.Plug plug, Node node) {
        Hub.super.onPlugDisconnect(plug, node);
        if (node == plug.node) {
            li.cil.oc.api.Network.leaveWirelessNetwork(this);
        }
        if (plug.isPrimary() && node != plug.node)
            plug.node.connect(componentNodes[plug.side.ordinal()]);
        else
            componentNodes[plug.side.ordinal()].remove();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        ComponentInventory.super.onItemAdded(slot, stack);
        updateLimits(slot, stack);
    }

    private void updateLimits(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        if (driver == null) return; // Dafuq u doin.
        final String slotType = driver.slot(stack);
        if (Slot.CPU.equals(slotType)) {
            setRelayDelay(Math.max(1, relayBaseDelay() - (int) ((driver.tier(stack) + 1) * relayDelayPerUpgrade())));
        } else if (Slot.Memory.equals(slotType)) {
            final int amount = stack.getItem() instanceof li.cil.oc.common.item.Memory ram
                ? (ram.tier + 1) * relayAmountPerUpgrade()
                : (driver.tier(stack) + 1) * (relayAmountPerUpgrade() * 2);
            setRelayAmount(Math.max(1, relayBaseAmount() + amount));
        } else if (Slot.HDD.equals(slotType)) {
            setMaxQueueSize(Math.max(1, queueBaseSize() + (driver.tier(stack) + 1) * queueSizePerUpgrade()));
        } else if (Slot.Card.equals(slotType)) {
            final ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
            if (descriptor == WirelessNetworkCardTier1() || descriptor == WirelessNetworkCardTier2())
                wirelessTier = descriptor == WirelessNetworkCardTier1() ? Tier.One : Tier.Two;
            if (descriptor == LinkedCard()) {
                final CompoundTag data = DriverLinkedCard.INSTANCE.dataTag(stack);
                if (data.contains(Settings.namespace + "tunnel")) {
                    tunnel = data.getString(Settings.namespace + "tunnel");
                    isLinkedEnabled = true;
                    QuantumNetwork.add(this);
                }
            }
        } // else: Dafuq u doin.
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        ComponentInventory.super.onItemRemoved(slot, stack);
        final DriverItem driver = Driver.driverFor(stack, getClass());
        final String slotType = driver.slot(stack);
        if (Slot.CPU.equals(slotType)) setRelayDelay(relayBaseDelay());
        else if (Slot.Memory.equals(slotType)) setRelayAmount(relayBaseAmount());
        else if (Slot.HDD.equals(slotType)) setMaxQueueSize(queueBaseSize());
        else if (Slot.Card.equals(slotType)) {
            wirelessTier = -1;
            isLinkedEnabled = false;
            QuantumNetwork.remove(this);
        }
    }

    @Override
    public int getContainerSize() {
        return InventorySlots.relay.length;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        if (driver == null) return false;
        final InventorySlots.InventorySlot provided = InventorySlots.relay[slot];
        final boolean tierSatisfied = driver.slot(stack).equals(provided.slot) && driver.tier(stack) <= provided.tier;
        final ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
        final boolean cardTypeSatisfied = !Slot.Card.equals(provided.slot) || descriptor == WirelessNetworkCardTier1() ||
            descriptor == WirelessNetworkCardTier2() || descriptor == LinkedCard();
        return tierSatisfied && cardTypeSatisfied;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Level world() {
        return getLevel();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Relay(ContainerTypes.RELAY.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        final ItemStack[] items = items();
        for (int slot = 0; slot < items.length; slot++) {
            if (!items[slot].isEmpty()) {
                updateLimits(slot, items[slot]);
            }
        }

        if (nbt.contains(StrengthTag)) {
            strength = Math.min(Math.max(nbt.getDouble(StrengthTag), 0), maxWirelessRange());
        }
        if (nbt.contains(IsRepeaterTag)) {
            isRepeater = nbt.getBoolean(IsRepeaterTag);
        }
        final CompoundTag[] tags = ExtendedNBT.toTagArray(nbt.getList(ComponentNodesTag, Tag.TAG_COMPOUND), CompoundTag.class);
        for (int index = 0; index < tags.length && index < componentNodes.length; index++) {
            componentNodes[index].loadData(tags[index]);
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        nbt.putDouble(StrengthTag, strength);
        nbt.putBoolean(IsRepeaterTag, isRepeater);
        final List<CompoundTag> tags = new ArrayList<>();
        for (Node node : componentNodes) {
            final CompoundTag tag = new CompoundTag();
            if (node != null) node.saveData(tag);
            tags.add(tag);
        }
        ExtendedNBT.setNewTagList(nbt, ComponentNodesTag, tags);
    }
}
