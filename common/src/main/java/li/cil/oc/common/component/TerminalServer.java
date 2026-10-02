package li.cil.oc.common.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.Lifecycle;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.Tier;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TerminalServer implements Environment, EnvironmentHost, Analyzable, RackMountable, Lifecycle, DeviceInfo {
    public final li.cil.oc.api.internal.Rack rack;
    public final int slot;

    public final Node node = Network.newNode(this, Visibility.None).create();

    private li.cil.oc.api.internal.TextBuffer buffer;
    private Keyboard keyboard;

    public double range = Settings.get().maxWirelessRange[Tier.Two];
    public final List<String> keys = new ArrayList<>();

    public TerminalServer(li.cil.oc.api.internal.Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
    }

    @Override
    public Node node() {
        return node;
    }

    public li.cil.oc.api.internal.TextBuffer buffer() {
        if (buffer == null) {
            final ItemStack screenItem = Items.get(Constants.BlockName.ScreenTier1).createItemStack(1);
            final li.cil.oc.api.internal.TextBuffer created = (li.cil.oc.api.internal.TextBuffer) Driver.driverFor(screenItem, getClass()).createEnvironment(screenItem, this);
            final Pair<Integer, Integer> maxResolution = Settings.screenResolutionsByTier[Tier.Three];
            created.setMaximumResolution(maxResolution.getLeft(), maxResolution.getRight());
            created.setMaximumColorDepth(Settings.screenDepthsByTier[Tier.Three]);
            buffer = created;
        }
        return buffer;
    }

    public Keyboard keyboard() {
        if (keyboard == null) {
            final ItemStack keyboardItem = Items.get(Constants.BlockName.Keyboard).createItemStack(1);
            final Keyboard created = (Keyboard) Driver.driverFor(keyboardItem, getClass()).createEnvironment(keyboardItem, this);
            created.setUsableOverride((kb, player) -> {
                final ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (stack.getItem() instanceof li.cil.oc.common.item.Terminal && stack.hasTag()) {
                    return sidedKeys().contains(stack.getTag().getString(Settings.namespace + "key"));
                }
                return false;
            });
            keyboard = created;
        }
        return keyboard;
    }

    public boolean hasAddress() {
        if (rack != null) {
            final CompoundTag data = rack.getMountableData(slot);
            if (data != null) {
                return data.contains("terminalAddress");
            }
        }
        return false;
    }

    public String address() {
        return rack.getMountableData(slot).getString("terminalAddress");
    }

    public List<String> sidedKeys() {
        if (!rack.world().isClientSide) return keys;
        else {
            final List<String> result = new ArrayList<>();
            final ListTag list = rack.getMountableData(slot).getList("keys", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                result.add(list.getString(i));
            }
            return result;
        }
    }

    // ----------------------------------------------------------------------- //
    // DeviceInfo

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic);
            info.put(DeviceInfo.DeviceAttribute.Description, "Terminal server");
            info.put(DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceInfo.DeviceAttribute.Product, "RemoteViewing EX");
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //
    // Environment

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            node.connect(buffer().node());
            node.connect(keyboard().node());
            buffer().node().connect(keyboard().node());
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == this.node) {
            buffer().node().remove();
            keyboard().node().remove();
        }
    }

    @Override
    public void onMessage(Message message) {
    }

    // ----------------------------------------------------------------------- //
    // EnvironmentHost

    @Override
    public Level world() {
        return rack.world();
    }

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
    public void markChanged() {
        rack.markChanged();
    }

    // ----------------------------------------------------------------------- //
    // RackMountable

    @Override
    public CompoundTag getData() {
        if (node.address() == null) Network.joinNewNetwork(node);

        final CompoundTag nbt = new CompoundTag();
        ExtendedNBT.setNewTagList(nbt, "keys", ExtendedNBT.stringIterableToNbt(keys));
        nbt.putString("terminalAddress", node.address());
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
        if (Items.get(heldItem) == Items.get(Constants.ItemName.Terminal)) {
            if (!world().isClientSide) {
                final String key = UUID.randomUUID().toString();
                keys.remove(heldItem.getOrCreateTag().getString(Settings.namespace + "key"));
                final int maxSize = Settings.get().terminalsPerServer;
                while (keys.size() >= maxSize) {
                    keys.remove(0);
                }
                keys.add(key);
                heldItem.getTag().putString(Settings.namespace + "key", key);
                heldItem.getTag().putString(Settings.namespace + "server", node.address());
                rack.markChanged(slot);
                player.getInventory().setChanged();
            }
            return true;
        } else return false;
    }

    // ----------------------------------------------------------------------- //
    // Persistable

    private static final String BufferTag = Settings.namespace + "buffer";
    private static final String KeyboardTag = Settings.namespace + "keyboard";
    private static final String KeysTag = Settings.namespace + "keys";

    @Override
    public void loadData(CompoundTag nbt) {
        if (!rack.world().isClientSide) {
            node.loadData(nbt);
        }
        buffer().loadData(nbt.getCompound(BufferTag));
        keyboard().loadData(nbt.getCompound(KeyboardTag));
        keys.clear();
        final ListTag list = nbt.getList(KeysTag, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            keys.add(list.getString(i));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        node.saveData(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, BufferTag, buffer()::saveData);
        ExtendedNBT.setNewCompoundTag(nbt, KeyboardTag, keyboard()::saveData);
        ExtendedNBT.setNewTagList(nbt, KeysTag, ExtendedNBT.stringIterableToNbt(keys));
    }

    // ----------------------------------------------------------------------- //
    // ManagedEnvironment

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (world().isClientSide || (node.address() != null && node.network() != null)) {
            buffer().update();
        }
    }

    // ----------------------------------------------------------------------- //
    // StateAware

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        return EnumSet.noneOf(StateAware.State.class);
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{buffer().node(), keyboard().node()};
    }

    // ----------------------------------------------------------------------- //
    // LifeCycle

    @Override
    public void onLifecycleStateChange(Lifecycle.LifecycleState state) {
        if (rack.world().isClientSide) {
            if (state == Lifecycle.LifecycleState.Initialized) {
                TerminalServer.loaded.add(this);
            } else if (state == Lifecycle.LifecycleState.Disposed) {
                TerminalServer.loaded.remove(this);
            }
            // else: Ignore.
        }
    }

    // ----------------------------------------------------------------------- //
    // Companion object.

    public static final TerminalServerCache loaded = new TerminalServerCache();

    // we need a smart cache because nodes are loaded in before they have addresses
    // and we need a unique set of terminal servers based on address
    // This cache acts as a Map[address: String, term: TerminalServer]
    // But it can store terminals before they have an address
    // Null-address terminals are not available for binding
    // As an address loads, repeated addresses are dropped from the list
    public static class TerminalServerCache {
        private final Map<String, TerminalServer> ready = new HashMap<>();
        private final List<TerminalServer> pending = new ArrayList<>();

        private void completePending() {
            final List<TerminalServer> promoted = new ArrayList<>();
            for (TerminalServer term : pending) {
                if (term.hasAddress())
                    promoted.add(term);
            }
            for (TerminalServer term : promoted) {
                pending.remove(term);
                final String address = term.address();
                if (!ready.containsKey(address)) {
                    ready.put(address, term);
                }
            }
        }

        public boolean add(TerminalServer terminal) {
            completePending();
            if (terminal.hasAddress()) {
                final String newAddress = terminal.address();
                if (ready.containsKey(newAddress)) {
                    return false;
                } else {
                    ready.put(newAddress, terminal);
                    return true;
                }
            } else {
                pending.add(terminal);
                return true;
            }
        }

        public boolean remove(TerminalServer terminal) {
            completePending();
            if (terminal.hasAddress())
                return ready.remove(terminal.address()) != null;
            else {
                final int before = pending.size();
                pending.remove(terminal);
                // Note: same (always false) comparison as on 1.16.5.
                return pending.size() > before;
            }
        }

        public void clear() {
            ready.clear();
            pending.clear();
        }

        public Optional<TerminalServer> find(String address) {
            completePending();
            return Optional.ofNullable(ready.get(address));
        }
    }
}
