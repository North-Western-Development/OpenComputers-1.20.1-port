package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.OpenSides;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class NetSplitter extends TileEntity implements Environment, OpenSides, RedstoneAware, SidedEnvironment, DeviceInfo {
    private Map<String, String> deviceInfo;

    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("net_splitter", Visibility.Network).
        create();

    public boolean isInverted = false;

    private static final String IsInvertedTag = Settings.namespace + "isInverted";

    public NetSplitter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        redstoneAwareState().isOutputEnabled = true;
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Network,
                DeviceAttribute.Description, "Ethernet controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "NetSplits",
                DeviceAttribute.Version, "1.0",
                DeviceAttribute.Width, "6"
            );
        }
        return deviceInfo;
    }

    @Override
    public boolean isSideOpen(Direction side) {
        return isInverted != OpenSides.super.isSideOpen(side);
    }

    @Override
    public void setSideOpen(Direction side, boolean value) {
        final boolean previous = isSideOpen(side);
        OpenSides.super.setSideOpen(side, value);
        if (previous != isSideOpen(side)) {
            final Level level = getLevel();
            if (isServer()) {
                node.remove();
                li.cil.oc.api.Network.joinOrCreateNetwork(this);
                PacketSender.sendNetSplitterState(this);
                level.playSound(null, getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.25f + 0.7f);
                level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            } else {
                final BlockState state = level.getBlockState(getBlockPos());
                level.sendBlockUpdated(getBlockPos(), state, state, 3);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node sidedNode(Direction side) {
        return isSideOpen(side) ? node : null;
    }

    /** Client side only. */
    @Override
    public boolean canConnect(Direction side) {
        return isSideOpen(side);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void initialize() {
        super.initialize();
        EventHandler.scheduleServer(this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        RedstoneAware.super.onRedstoneInputChanged(args);
        final boolean oldIsInverted = isInverted;
        isInverted = args.newValue > 0;
        if (isInverted != oldIsInverted) {
            final Level level = getLevel();
            if (isServer()) {
                node.remove();
                li.cil.oc.api.Network.joinOrCreateNetwork(this);
                PacketSender.sendNetSplitterState(this);
                level.playSound(null, getBlockPos(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.25f + 0.7f);
            } else {
                final BlockState state = level.getBlockState(getBlockPos());
                level.sendBlockUpdated(getBlockPos(), state, state, 3);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        isInverted = nbt.getBoolean(IsInvertedTag);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        nbt.putBoolean(IsInvertedTag, isInverted);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        isInverted = nbt.getBoolean(IsInvertedTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        nbt.putBoolean(IsInvertedTag, isInverted);
    }

    // component api
    public Map<Integer, Boolean> currentStatus() {
        final Map<Integer, Boolean> openStatus = new HashMap<>();
        for (Direction side : Direction.values()) {
            openStatus.put(side.ordinal(), isSideOpen(side));
        }
        return openStatus;
    }

    public boolean setSide(Direction side, boolean state) {
        final boolean previous = isSideOpen(side); // isSideOpen uses inverter
        setSideOpen(side, isInverted != state); // but setSideOpen does not
        return previous != state;
    }

    @Callback(doc = "function(settings:table):table -- set open state (true/false) of all sides in an array; index by direction. Returns previous states")
    public Object[] setSides(Context context, Arguments args) {
        final Map<?, ?> settings = args.checkTable(0);
        final Map<Integer, Boolean> previous = currentStatus();
        for (Direction side : Direction.values()) {
            final int ordinal = side.ordinal();
            final boolean value = settings.containsKey(ordinal) && settings.get(ordinal) instanceof Boolean v && v;
            setSide(side, value);
        }
        return result(previous);
    }

    @Callback(direct = true, doc = "function():table -- Returns current open/close state of all sides in an array, indexed by direction.")
    public Object[] getSides(Context context, Arguments args) {
        return result(currentStatus());
    }

    public Object[] setSideHelper(Arguments args, boolean value) {
        final int sideIndex = args.checkInteger(0);
        if (sideIndex < 0 || sideIndex > 5)
            return result(ResultWrapper.unit, "invalid direction");
        final Direction side = Direction.from3DDataValue(sideIndex);
        return result(setSide(side, value));
    }

    @Callback(doc = "function(side: number):boolean -- Open the side, returns true if it changed to open.")
    public Object[] open(Context context, Arguments args) {
        return setSideHelper(args, true);
    }

    @Callback(doc = "function(side: number):boolean -- Close the side, returns true if it changed to close.")
    public Object[] close(Context context, Arguments args) {
        return setSideHelper(args, false);
    }
}
