package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.tileentity.RobotProxy;
import li.cil.oc.integration.opencomputers.DriverRedstoneCard;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Scala {@code var hasErrored} is {@link #hasErrored()} / {@link #setHasErrored(boolean)}.
 */
public interface Computer extends Environment, ComponentInventory, Rotatable, BundledRedstoneAware, Analyzable, MachineHost, StateAware, Tickable {
    final class State {
        public volatile Machine machine;
        public volatile boolean machineCreated = false;
        public volatile boolean isRunning = false;
        // For client side rendering of error LED indicator.
        public volatile boolean hasErrored = false;
        public final Set<String> users = Collections.synchronizedSet(new HashSet<>());
    }

    String ComputerTag = Settings.namespace + "computer";
    String HasErroredTag = Settings.namespace + "hasErrored";
    String IsRunningTag = Settings.namespace + "isRunning";
    String UsersTag = Settings.namespace + "users";

    /** Provided by {@link TileEntity}. */
    State computerState();

    /** Scala {@code private lazy val _machine = if (isServer) api.Machine.create(this) else null}. */
    @Override
    default Machine machine() {
        final State state = computerState();
        if (!state.machineCreated) {
            synchronized (state) {
                if (!state.machineCreated) {
                    state.machine = isServer() ? li.cil.oc.api.Machine.create(this) : null;
                    state.machineCreated = true;
                }
            }
        }
        return state.machine;
    }

    @Override
    default Node node() {
        return isServer() ? machine().node() : null;
    }

    default boolean hasErrored() {
        return computerState().hasErrored;
    }

    default void setHasErrored(boolean value) {
        computerState().hasErrored = value;
    }

    default Optional<String> runSound() {
        return Optional.of("computer_running");
    }

    // ----------------------------------------------------------------------- //

    default boolean canInteract(String player) {
        if (isServer()) return machine().canInteract(player);
        final Set<String> users = computerState().users;
        return !Settings.get().canComputersBeOwned || users.isEmpty() || users.contains(player);
    }

    default boolean isRunning() {
        return computerState().isRunning;
    }

    default void setRunning(boolean value) {
        final State state = computerState();
        if (value != state.isRunning) {
            state.isRunning = value;
            if (value) {
                state.hasErrored = false;
            }
            final Level level = getLevel();
            if (level != null) {
                final BlockState blockState = level.getBlockState(getBlockPos());
                level.sendBlockUpdated(getBlockPos(), blockState, blockState, 3);
                if (level.isClientSide) {
                    runSound().ifPresent(sound -> {
                        if (state.isRunning) li.cil.oc.client.Sound.startLoop((TileEntity) this, sound, 0.5f, 50 + level.random.nextInt(50));
                        else li.cil.oc.client.Sound.stopLoop((TileEntity) this);
                    });
                }
            }
        }
    }

    /** Client side only. */
    default void setUsers(Iterable<String> list) {
        final Set<String> users = computerState().users;
        synchronized (users) {
            users.clear();
            for (String user : list) users.add(user);
        }
    }

    @Override
    default EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (isRunning()) return EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        else return EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    // ----------------------------------------------------------------------- //

    @Override
    default Iterable<ItemStack> internalComponents() {
        final List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && isComponentSlot(slot, stack)) result.add(stack);
        }
        return result;
    }

    @Override
    default void onMachineConnect(Node node) {
        this.onConnect(node);
    }

    @Override
    default void onMachineDisconnect(Node node) {
        this.onDisconnect(node);
    }

    default boolean hasRedstoneCard() {
        for (ItemStack item : items()) {
            if (!item.isEmpty() && machine().isRunning() && DriverRedstoneCard.worksWith(item, getClass())) return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    /** Runs *before* the other trait updates (the Scala override called super last). */
    static void onUpdateEntity(Computer self) {
        // If we're not yet in a network we might have just been loaded from disk,
        // meaning there may be other tile entities that also have not re-joined
        // the network. We skip the update this round to allow other tile entities
        // to join the network, too, avoiding issues of missing nodes (e.g. in the
        // GPU which would otherwise loose track of its screen).
        if (self.isServer() && self.isConnected()) {
            self.updateComputer();

            final State state = self.computerState();
            final boolean running = self.machine().isRunning();
            final boolean errored = self.machine().lastError() != null;
            if (state.isRunning != running || state.hasErrored != errored) {
                state.isRunning = running;
                state.hasErrored = errored;
                self.onRunningChanged();
            }

            self.updateComponents();
        }
    }

    default void updateComputer() {
        machine().update();
    }

    default void onRunningChanged() {
        setChanged();
        PacketSender.sendComputerState(this);
    }

    static void onDispose(Computer self) {
        if (self.machine() != null && !(self instanceof RobotProxy)) {
            self.machine().stop();
        }
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(Computer self, CompoundTag nbt) {
        // God, this is so ugly... will need to rework the robot architecture.
        // This is required for loading auxiliary data (kernel state), because the
        // coordinates in the actual robot won't be set properly, otherwise.
        if (self instanceof RobotProxy proxy) {
            proxy.robot.setLevelAndPosition(self.getLevel(), self.getBlockPos());
        }
        self.machine().loadData(nbt.getCompound(ComputerTag));

        // Kickstart initialization to avoid values getting overwritten by
        // loadForClient if that packet is handled after a manual
        // initialization / state change packet.
        self.setRunning(self.machine().isRunning());
        self.redstoneAwareState().isOutputEnabled = self.hasRedstoneCard();
    }

    static void onSaveForServer(Computer self, CompoundTag nbt) {
        final Machine machine = self.machine();
        if (machine != null) {
            ExtendedNBT.setNewCompoundTag(nbt, ComputerTag, machine::saveData);
        }
    }

    static void onLoadForClient(Computer self, CompoundTag nbt) {
        final State state = self.computerState();
        state.hasErrored = nbt.getBoolean(HasErroredTag);
        self.setRunning(nbt.getBoolean(IsRunningTag));
        final List<String> users = ExtendedNBT.<StringTag, String>map(nbt.getList(UsersTag, Tag.TAG_STRING), StringTag::getAsString);
        self.setUsers(users);
        final Level level = self.getLevel();
        if (state.isRunning && level != null) {
            self.runSound().ifPresent(sound -> li.cil.oc.client.Sound.startLoop((TileEntity) self, sound, 0.5f, 1000 + level.random.nextInt(2000)));
        }
    }

    static void onSaveForClient(Computer self, CompoundTag nbt) {
        final Machine machine = self.machine();
        nbt.putBoolean(HasErroredTag, machine != null && machine.lastError() != null);
        nbt.putBoolean(IsRunningTag, self.isRunning());
        final List<StringTag> users = new ArrayList<>();
        if (machine != null) {
            for (String user : machine.users()) users.add(StringTag.valueOf(user));
        }
        ExtendedNBT.setNewTagList(nbt, UsersTag, users);
    }

    // ----------------------------------------------------------------------- //

    static void onSetChanged(Computer self) {
        if (self.isServer()) {
            self.machine().onHostChanged();
            self.setOutputEnabled(self.hasRedstoneCard());
        }
    }

    @Override
    default boolean stillValid(Player player) {
        final boolean canInteract;
        if (player instanceof li.cil.oc.server.agent.Player fakePlayer) {
            canInteract = canInteract(fakePlayer.agent.ownerName());
        } else {
            canInteract = canInteract(player.getName().getString());
        }
        return ComponentInventory.super.stillValid(player) && canInteract;
    }

    @Override
    default void onRotationChanged() {
        Rotatable.super.onRotationChanged();
        checkRedstoneInputChanged();
    }

    @Override
    default void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        BundledRedstoneAware.super.onRedstoneInputChanged(args);
        final RedstoneChangedEventArgs toLocalArgs = new RedstoneChangedEventArgs(toLocal(args.side), args.oldValue, args.newValue, args.color);
        machine().node().sendToNeighbors("redstone.changed", toLocalArgs);
    }

    // ----------------------------------------------------------------------- //

    @Override
    default Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{machine().node()};
    }
}
