package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.event.GeolyzerEvent.Analyze;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.common.tileentity.Microcontroller;
import li.cil.oc.server.component.traits.WorldControl;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArguments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static li.cil.oc.util.ResultWrapper.result;

public class Geolyzer extends AbstractManagedEnvironment implements WorldControl, DeviceInfo {
    public final EnvironmentHost host;

    public final ComponentConnector node;

    public Geolyzer(EnvironmentHost host) {
        this.host = host;
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Network).
                withComponent("geolyzer").
                withConnector().
                create();
        setNode(node);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Generic);
            info.put(DeviceAttribute.Description, "Geolyzer");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "Terrain Analyzer MkII");
            info.put(DeviceAttribute.Capacity, Integer.toString(Settings.get().geolyzerRange));
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Direction checkSideForAction(Arguments args, int n) {
        final Direction side = ExtendedArguments.checkSideAny(args, n);
        if (host instanceof li.cil.oc.common.tileentity.Robot robot) return robot.proxy.toGlobal(side);
        else if (host instanceof li.cil.oc.common.entity.Drone drone) return drone.toGlobal(side);
        else if (host instanceof Microcontroller uc) return uc.toLocal(side); // not really sure what it is reversed for microcontrollers
        else if (host instanceof TabletWrapper tablet) return tablet.toGlobal(side);
        else return side;
    }

    @Override
    public BlockPosition position() {
        if (host instanceof li.cil.oc.common.tileentity.Robot robot) return robot.proxy.position();
        else if (host instanceof li.cil.oc.common.entity.Drone drone) return BlockPosition.apply(drone.blockPosition(), drone.level());
        else if (host instanceof Microcontroller uc) return uc.position();
        else if (host instanceof TabletWrapper tablet) return BlockPosition.apply(tablet.xPosition(), tablet.yPosition(), tablet.zPosition(), tablet.world());
        else return BlockPosition.apply(host);
    }

    private boolean canSeeSkyInternal() {
        final BlockPosition blockPos = position().offset(Direction.UP);
        return host.world().dimension() != Level.NETHER && host.world().canSeeSkyFromBelowWater(blockPos.toBlockPos());
    }

    @Callback(doc = "function():boolean -- Returns whether there is a clear line of sight to the sky directly above.")
    public Object[] canSeeSky(Context computer, Arguments args) {
        return result(canSeeSkyInternal());
    }

    @Callback(doc = "function():boolean -- Return whether the sun is currently visible directly above.")
    public Object[] isSunVisible(Context computer, Arguments args) {
        final BlockPos blockPos = BlockPosition.apply(host).offset(Direction.UP).toBlockPos();
        final Level world = host.world();
        return result(
                world.isDay() &&
                        canSeeSkyInternal() &&
                        (world.getBiome(blockPos).value().getPrecipitationAt(blockPos) == Biome.Precipitation.NONE || (!world.isRaining() && !world.isThundering())));
    }

    @Callback(doc = "function(x:number, z:number[, y:number, w:number, d:number, h:number][, ignoreReplaceable:boolean|options:table]):table -- Analyzes the density of the column at the specified relative coordinates.")
    public Object[] scan(Context computer, Arguments args) {
        final int[] scanArgs = getScanArgs(args);
        final int minX = scanArgs[0], minY = scanArgs[1], minZ = scanArgs[2], maxX = scanArgs[3], maxY = scanArgs[4], maxZ = scanArgs[5], optIndex = scanArgs[6];
        final int volume = (maxX - minX + 1) * (maxZ - minZ + 1) * (maxY - minY + 1);
        if (volume > 64) throw new IllegalArgumentException("volume too large (maximum is 64)");
        final Map<?, ?> options;
        if (args.isBoolean(optIndex)) {
            final Map<Object, Object> map = new HashMap<>();
            map.put("includeReplaceable", !args.checkBoolean(optIndex));
            options = map;
        } else options = args.optTable(optIndex, new HashMap<>());
        final int range = Settings.get().geolyzerRange;
        if (Math.abs(minX) > range || Math.abs(maxX) > range ||
                Math.abs(minY) > range || Math.abs(maxY) > range ||
                Math.abs(minZ) > range || Math.abs(maxZ) > range) {
            throw new IllegalArgumentException("location out of bounds");
        }

        if (!node.tryChangeBuffer(-Settings.get().geolyzerScanCost))
            return result(null, "not enough energy");

        final GeolyzerEvent.Scan event = new GeolyzerEvent.Scan(host, options, minX, minY, minZ, maxX, maxY, maxZ);
        EventBus.INSTANCE.post(event);
        if (event.isCanceled()) return result(null, "scan was canceled");
        else return result((Object) event.data);
    }

    /** @return {minX, minY, minZ, maxX, maxY, maxZ, optIndex} */
    private int[] getScanArgs(Arguments args) {
        final int minX = args.checkInteger(0);
        final int minZ = args.checkInteger(1);
        if (args.isInteger(2) && args.isInteger(3) && args.isInteger(4) && args.isInteger(5)) {
            final int minY = args.checkInteger(2);
            final int w = args.checkInteger(3);
            final int d = args.checkInteger(4);
            final int h = args.checkInteger(5);
            final int maxX = minX + w - 1;
            final int maxY = minY + h - 1;
            final int maxZ = minZ + d - 1;

            return new int[]{Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
                    Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ),
                    6};
        } else {
            return new int[]{minX, -32, minZ, minX, 31, minZ, 2};
        }
    }

    private Direction globalSide(Direction side) {
        if (host instanceof Rotatable rotatable) return rotatable.toGlobal(side);
        else return side;
    }

    @Callback(doc = "function(side:number[,options:table]):table -- Get some information on a directly adjacent block.")
    public Object[] analyze(Context computer, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            final Direction side = ExtendedArguments.checkSideAny(args, 0);
            final Direction globalSide = globalSide(side);
            final Map<?, ?> options = args.optTable(1, new HashMap<>());

            if (!node.tryChangeBuffer(-Settings.get().geolyzerScanCost))
                return result(null, "not enough energy");

            final BlockPosition globalPos = BlockPosition.apply(host).offset(globalSide);
            final Analyze event = new Analyze(host, options, globalPos.toBlockPos());
            EventBus.INSTANCE.post(event);
            if (event.isCanceled()) return result(null, "scan was canceled");
            else return result(event.data);
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack representation of the block on the specified side in a database component.")
    public Object[] store(Context computer, Arguments args) {
        final Direction side = ExtendedArguments.checkSideAny(args, 0);
        final Direction globalSide = globalSide(side);

        if (!node.tryChangeBuffer(-Settings.get().geolyzerScanCost))
            return result(null, "not enough energy");

        final BlockPos blockPos = BlockPosition.apply(host).offset(globalSide).toBlockPos();
        final BlockState blockState = host.world().getBlockState(blockPos);
        final Item item = blockState.getBlock().asItem();
        if (item == null || item == Items.AIR) return result(null, "block has no registered item representation");
        else {
            final List<ItemStack> stacks = Block.getDrops(blockState, (ServerLevel) host.world(), blockPos, host.world().getBlockEntity(blockPos));
            final ItemStack stack;
            if (!stacks.isEmpty()) {
                ItemStack drop = stacks.get(0);
                for (ItemStack s : stacks) {
                    if (s.getItem() == item) {
                        drop = s;
                        break;
                    }
                }
                drop.setCount(1);
                stack = drop;
            } else stack = new ItemStack(item, 1);
            return DatabaseAccess.withDatabase(node, args.checkString(1), database -> {
                final int toSlot = ExtendedArguments.checkSlot(args, database.data, 2);
                final boolean nonEmpty = database.getStackInSlot(toSlot) != ItemStack.EMPTY; // not the same as isEmpty! zero size stacks!
                database.setStackInSlot(toSlot, stack);
                return result(nonEmpty);
            });
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof li.cil.oc.api.machine.Machine machine) {
            final Object[] data = message.data();
            if (machine.host() instanceof Tablet && data.length == 8 &&
                    data[0] instanceof CompoundTag nbt && data[1] instanceof ItemStack &&
                    data[2] instanceof Player && data[3] instanceof BlockPosition blockPos &&
                    data[4] instanceof Direction && data[5] instanceof Float &&
                    data[6] instanceof Float && data[7] instanceof Float) {
                if (node.tryChangeBuffer(-Settings.get().geolyzerScanCost)) {
                    final Analyze event = new Analyze(host, new HashMap<>(), blockPos.toBlockPos());
                    EventBus.INSTANCE.post(event);
                    if (!event.isCanceled()) {
                        for (Map.Entry<String, Object> entry : event.data.entrySet()) {
                            final Object value = entry.getValue();
                            if (value instanceof Number number) nbt.putDouble(entry.getKey(), number.doubleValue());
                            else if (value instanceof String string && !string.isEmpty()) nbt.putString(entry.getKey(), string);
                            // else: Unsupported, ignore.
                        }
                    }
                }
            }
        }
    }
}
