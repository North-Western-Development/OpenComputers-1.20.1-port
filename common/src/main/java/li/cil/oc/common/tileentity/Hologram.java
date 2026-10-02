package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.RotatableTile;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Hologram extends TileEntity implements Environment, SidedEnvironment, Analyzable, RotatableTile, Tickable, DeviceInfo {
    public int tier;

    public final ComponentConnector node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("hologram").
        withConnector().
        create();

    public final int width = 3 * 16;

    public final int height = 2 * 16; // 32 bit in an int

    private Map<String, String> deviceInfo;

    // ----------------------------------------------------------------------- //

    // Layout is: first half is lower bit, second half is higher bit for the
    // voxels in the cube. This is to retain compatibility with pre 1.3 saves.
    public final int[] volume = new int[width * width * 2];

    // Render scale.
    public double scale = 1.0;

    // Projection Y position offset - consider adding X,Z later perhaps
    public Vec3 translation = new Vec3(0, 0, 0);

    // Relative number of lit columns (for energy cost).
    public double litRatio = -1.0;

    // Whether we need to recompile our display list.
    public boolean needsRendering = false;

    // Store it here for convenience, this is the number of visible voxel faces
    // as determined in the last VBO index update. See HologramRenderer.
    public int visibleQuads = 0;

    // What parts of the hologram changed and need an update packet.
    public Set<Short> dirty = new HashSet<>();

    // Interval of dirty columns.
    public int dirtyFromX = Integer.MAX_VALUE;
    public int dirtyUntilX = -1;
    public int dirtyFromZ = Integer.MAX_VALUE;
    public int dirtyUntilZ = -1;

    public boolean hasPower = true;

    // Rotation base state. Current rotation is based on world time. See HologramRenderer.
    public float rotationAngle = 0f;
    public float rotationX = 0f;
    public float rotationY = 0f;
    public float rotationZ = 0f;
    public float rotationSpeed = 0f;
    public float rotationSpeedX = 0f;
    public float rotationSpeedY = 0f;
    public float rotationSpeedZ = 0f;

    public final int[][] colorsByTier = {{0x00FF00}, {0x0000FF, 0x00FF00, 0xFF0000}}; // 0xBBGGRR for rendering convenience

    private static final double Sqrt2 = Math.sqrt(2);

    private static final String TierTag = Settings.namespace + "tier";
    private static final String VolumeTag = "volume";
    private static final String ColorsTag = "colors";
    private static final String ScaleTag = Settings.namespace + "scale";
    private static final String OffsetXTag = Settings.namespace + "offsetX";
    private static final String OffsetYTag = Settings.namespace + "offsetY";
    private static final String OffsetZTag = Settings.namespace + "offsetZ";
    private static final String RotationAngleTag = Settings.namespace + "rotationAngle";
    private static final String RotationXTag = Settings.namespace + "rotationX";
    private static final String RotationYTag = Settings.namespace + "rotationY";
    private static final String RotationZTag = Settings.namespace + "rotationZ";
    private static final String RotationSpeedTag = Settings.namespace + "rotationSpeed";
    private static final String RotationSpeedXTag = Settings.namespace + "rotationSpeedX";
    private static final String RotationSpeedYTag = Settings.namespace + "rotationSpeedY";
    private static final String RotationSpeedZTag = Settings.namespace + "rotationSpeedZ";
    private static final String HasPowerTag = Settings.namespace + "hasPower";

    public Hologram(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier) {
        super(type, pos, state);
        this.tier = tier;
    }

    public Hologram(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 0);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Display,
                DeviceAttribute.Description, "Holographic projector",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "VirtualViewer H1-" + (tier + 1),
                DeviceAttribute.Capacity, String.valueOf(width * width * height),
                DeviceAttribute.Width, String.valueOf(colors().length)
            );
        }
        return deviceInfo;
    }

    // This is a def and not a val for loading (where the tier comes from the nbt and is always 0 here).
    public int[] colors() {
        return colorsByTier[tier];
    }

    public int getColor(int x, int y, int z) {
        final int lbit = (volume[x + z * width] >>> y) & 1;
        final int hbit = (volume[x + z * width + width * width] >>> y) & 1;
        return lbit | (hbit << 1);
    }

    public void setColor(int x, int y, int z, int value) {
        if ((value & 3) != getColor(x, y, z)) {
            final int lbit = value & 1;
            final int hbit = (value >>> 1) & 1;
            volume[x + z * width] = (volume[x + z * width] & ~(1 << y)) | (lbit << y);
            volume[x + z * width + width * width] = (volume[x + z * width + width * width] & ~(1 << y)) | (hbit << y);
            setDirty(x, z);
        }
    }

    private void setDirty(int x, int z) {
        dirty.add((short) (((byte) x << 8) | (byte) z));
        dirtyFromX = Math.min(dirtyFromX, x);
        dirtyUntilX = Math.max(dirtyUntilX, x + 1);
        dirtyFromZ = Math.min(dirtyFromZ, z);
        dirtyUntilZ = Math.max(dirtyUntilZ, z + 1);
        litRatio = -1;
    }

    private void resetDirtyFlag() {
        dirty.clear();
        dirtyFromX = Integer.MAX_VALUE;
        dirtyUntilX = -1;
        dirtyFromZ = Integer.MAX_VALUE;
        dirtyUntilZ = -1;
    }

    // ----------------------------------------------------------------------- //

    /** Client side only. */
    @Override
    public boolean canConnect(Direction side) {
        return toLocal(side) == Direction.DOWN;
    }

    @Override
    public Node sidedNode(Direction side) {
        return toLocal(side) == Direction.DOWN ? node : null;
    }

    // Override automatic analyzer implementation for sided environments.
    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{node};
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function() -- Clears the hologram.")
    public synchronized Object[] clear(Context context, Arguments args) {
        java.util.Arrays.fill(volume, 0);
        PacketSender.sendHologramClear(this);
        resetDirtyFlag();
        litRatio = 0;
        return null;
    }

    @Callback(direct = true, doc = "function(x:number, y:number, z:number):number -- Returns the value for the specified voxel.")
    public synchronized Object[] get(Context context, Arguments args) {
        final int[] c = checkCoordinates(args, 0, 1, 2);
        return result(getColor(c[0], c[1], c[2]));
    }

    @Callback(direct = true, limit = 256, doc = "function(x:number, y:number, z:number, value:number or boolean) -- Set the value for the specified voxel.")
    public synchronized Object[] set(Context context, Arguments args) {
        final int[] c = checkCoordinates(args, 0, 1, 2);
        final int value = checkColor(args, 3);
        setColor(c[0], c[1], c[2], value);
        return null;
    }

    @Callback(direct = true, limit = 128, doc = "function(x:number, z:number[, minY:number], maxY:number, value:number or boolean) -- Fills an interval of a column with the specified value.")
    public synchronized Object[] fill(Context context, Arguments args) {
        final int[] c = checkCoordinates(args, 0, -1, 1);
        final int x = c[0];
        final int z = c[2];
        final int minY;
        final int maxY;
        final int value;
        if (args.count() > 4) {
            minY = Math.min(32, Math.max(1, args.checkInteger(2)));
            maxY = Math.min(32, Math.max(1, args.checkInteger(3)));
            value = checkColor(args, 4);
        } else {
            minY = 1;
            maxY = Math.min(32, Math.max(1, args.checkInteger(2)));
            value = checkColor(args, 3);
        }
        if (minY > maxY) throw new IllegalArgumentException("interval is empty");

        final int mask = (0xFFFFFFFF >>> (31 - (maxY - minY))) << (minY - 1);
        final int lbit = value & 1;
        final int hbit = (value >>> 1) & 1;
        if (lbit == 0 || height == 0) volume[x + z * width] &= ~mask;
        else volume[x + z * width] |= mask;
        if (hbit == 0 || height == 0) volume[x + z * width + width * width] &= ~mask;
        else volume[x + z * width + width * width] |= mask;

        setDirty(x, z);
        return null;
    }

    @Callback(doc = "function(data:string) -- Set the raw buffer to the specified byte array, where each byte represents a voxel color. Nesting is x,z,y.")
    public synchronized Object[] setRaw(Context context, Arguments args) {
        final byte[] data = args.checkByteArray(0);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                final int offset = z * height + x * height * width;
                if (data.length >= offset + height) {
                    int lbit = 0;
                    int hbit = 0;
                    for (int y = height - 1; y >= 0; y--) {
                        final byte color = data[offset + y];
                        lbit |= (color & 1) << y;
                        hbit |= ((color & 3) >>> 1) << y;
                    }
                    final int index = x + z * width;
                    if (volume[index] != lbit || volume[index + width * width] != hbit) {
                        volume[index] = lbit;
                        volume[index + width * width] = hbit;
                        setDirty(x, z);
                    }
                }
            }
        }
        context.pause(Settings.get().hologramSetRawDelay);
        return null;
    }

    @Callback(doc = "function(x:number, z:number, sx:number, sz:number, tx:number, tz:number) -- Copies an area of columns by the specified translation.")
    public synchronized Object[] copy(Context context, Arguments args) {
        final int[] c = checkCoordinates(args, 0, -1, 1);
        final int x = c[0];
        final int z = c[2];
        final int w = args.checkInteger(2);
        final int h = args.checkInteger(3);
        final int tx = args.checkInteger(4);
        final int tz = args.checkInteger(5);

        // Anything to do at all?
        if (w <= 0 || h <= 0) return null;
        if (tx == 0 && tz == 0) return null;
        // Loop over the target rectangle, starting from the directions away from
        // the source rectangle and copy the data. This way we ensure we don't
        // overwrite anything we still need to copy.
        int dx0 = Math.max(0, Math.min(width - 1, x + tx + w - 1));
        int dx1 = Math.max(0, Math.min(width, x + tx));
        if (tx <= 0) {
            final int tmp = dx0;
            dx0 = dx1;
            dx1 = tmp;
        }
        int dz0 = Math.max(0, Math.min(width - 1, z + tz + h - 1));
        int dz1 = Math.max(0, Math.min(width, z + tz));
        if (tz <= 0) {
            final int tmp = dz0;
            dz0 = dz1;
            dz1 = tmp;
        }
        final int sx = tx > 0 ? -1 : 1;
        final int sz = tz > 0 ? -1 : 1;
        // Copy values to destination rectangle if there source is valid.
        for (int nz = dz0; sz > 0 ? nz <= dz1 : nz >= dz1; nz += sz) {
            final int oz = nz - tz;
            if (oz >= 0 && oz < width) {
                for (int nx = dx0; sx > 0 ? nx <= dx1 : nx >= dx1; nx += sx) {
                    final int ox = nx - tx;
                    if (ox >= 0 && ox < width) {
                        volume[nz * width + nx] = volume[oz * width + ox];
                        volume[nz * width + nx + width * width] = volume[oz * width + ox + width * width];
                        // previous, we set the volume area as dirty. but this is error prone
                        // in case the update sends the values - we only send the corners of the copied arae
                        // it is FAR better to mark each bit as dirty, and let the optimized update do what
                        // it was designed to do
                        setDirty(nx, nz);
                    } /* else: Got no source column. */
                }
            } /* else: Got no source row. */
        }

        // The reasoning here is: it'd take 18 ticks to do the whole are with fills,
        // so make this slightly more efficient (15 ticks - 0.75 seconds). Make it
        // 'free' if it's less than 0.25 seconds, i.e. for small copies.
        final int area = (Math.max(dx0, dx1) - Math.min(dx0, dx1)) * (Math.max(dz0, dz1) - Math.min(dz0, dz1));
        final double relativeArea = Math.max(0, area / (float) (width * width) - 0.25);
        context.pause(relativeArea);

        return null;
    }

    @Callback(direct = true, doc = "function():number -- Returns the render scale of the hologram.")
    public Object[] getScale(Context context, Arguments args) {
        return result(scale);
    }

    @Callback(doc = "function(value:number) -- Set the render scale. A larger scale consumes more energy.")
    public Object[] setScale(Context context, Arguments args) {
        scale = Math.max(0.333333, Math.min(Settings.get().hologramMaxScaleByTier[tier], args.checkDouble(0)));
        PacketSender.sendHologramScale(this);
        return null;
    }

    @Callback(direct = true, doc = "function():number, number, number -- Returns the relative render projection offsets of the hologram.")
    public Object[] getTranslation(Context context, Arguments args) {
        return result(translation.x, translation.y, translation.z);
    }

    @Callback(doc = "function(tx:number, ty:number, tz:number) -- Sets the relative render projection offsets of the hologram.")
    public Object[] setTranslation(Context context, Arguments args) {
        // Validate all axes before setting the values.
        final double maxTranslation = Settings.get().hologramMaxTranslationByTier[tier];
        final double tx = Math.max(-maxTranslation, Math.min(maxTranslation, args.checkDouble(0)));
        final double ty = Math.max(0, Math.min(maxTranslation * 2, args.checkDouble(1)));
        final double tz = Math.max(-maxTranslation, Math.min(maxTranslation, args.checkDouble(2)));

        translation = new Vec3(tx, ty, tz);

        PacketSender.sendHologramOffset(this);
        return null;
    }

    @Callback(direct = true, doc = "function():number -- The color depth supported by the hologram.")
    public Object[] maxDepth(Context context, Arguments args) {
        return result(tier + 1);
    }

    @Callback(doc = "function(index:number):number -- Get the color defined for the specified value.")
    public Object[] getPaletteColor(Context context, Arguments args) {
        final int index = args.checkInteger(0);
        if (index < 1 || index > colors().length) throw new ArrayIndexOutOfBoundsException();
        // Colors are stored as 0xAABBGGRR for rendering convenience, so convert them.
        return result(convertColor(colors()[index - 1]));
    }

    @Callback(doc = "function(index:number, value:number):number -- Set the color defined for the specified value.")
    public Object[] setPaletteColor(Context context, Arguments args) {
        final int index = args.checkInteger(0);
        if (index < 1 || index > colors().length) throw new ArrayIndexOutOfBoundsException();
        final int value = args.checkInteger(1);
        final int oldValue = colors()[index - 1];
        // Change byte order here to allow passing stored color to OpenGL "as-is"
        // (as whole Int, i.e. 0xAABBGGRR, alpha is unused but present for alignment)
        colors()[index - 1] = convertColor(value);
        PacketSender.sendHologramColor(this, index - 1, colors()[index - 1]);
        return result(oldValue);
    }

    @Callback(doc = "function(angle:number, x:number, y:number, z:number):boolean -- Set the base rotation of the displayed hologram.")
    public Object[] setRotation(Context context, Arguments args) {
        if (tier > 0) {
            final double r = args.checkDouble(0) % 360;
            final double x = args.checkDouble(1);
            final double y = args.checkDouble(2);
            final double z = args.checkDouble(3);

            rotationAngle = (float) r;
            rotationX = (float) x;
            rotationY = (float) y;
            rotationZ = (float) z;
            PacketSender.sendHologramRotation(this);

            return result(true);
        } else return result(ResultWrapper.unit, "not supported");
    }

    @Callback(doc = "function(speed:number, x:number, y:number, z:number):boolean -- Set the rotation speed of the displayed hologram.")
    public Object[] setRotationSpeed(Context context, Arguments args) {
        if (tier > 0) {
            final double v = Math.min(Math.max(args.checkDouble(0), -360 * 4), 360 * 4);
            final double x = args.checkDouble(1);
            final double y = args.checkDouble(2);
            final double z = args.checkDouble(3);

            rotationSpeed = (float) v;
            rotationSpeedX = (float) x;
            rotationSpeedY = (float) y;
            rotationSpeedZ = (float) z;
            PacketSender.sendHologramRotationSpeed(this);

            return result(true);
        } else return result(ResultWrapper.unit, "not supported");
    }

    @Callback(direct = true, doc = "function():number, number, number -- Get the dimension of the x,y,z axes.")
    public Object[] getDimensions(Context context, Arguments args) {
        return result(width, height, width);
    }

    private int[] checkCoordinates(Arguments args, int idxX, int idxY, int idxZ) {
        final int x = idxX >= 0 ? args.checkInteger(idxX) - 1 : 0;
        if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException("x");
        final int y = idxY >= 0 ? args.checkInteger(idxY) - 1 : 0;
        if (y < 0 || y >= height) throw new ArrayIndexOutOfBoundsException("y");
        final int z = idxZ >= 0 ? args.checkInteger(idxZ) - 1 : 0;
        if (z < 0 || z >= width) throw new ArrayIndexOutOfBoundsException("z");
        return new int[]{x, y, z};
    }

    private int checkColor(Arguments args, int index) {
        final int value;
        if (args.isBoolean(index))
            value = args.checkBoolean(index) ? 1 : 0;
        else
            value = args.checkInteger(index);
        if (value < 0 || value > colors().length) throw new IllegalArgumentException("invalid value");
        return value;
    }

    private static int convertColor(int color) {
        return ((color & 0x0000FF) << 16) | (color & 0x00FF00) | ((color & 0xFF0000) >>> 16);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            if (!dirty.isEmpty()) {
                synchronized (this) {
                    final int dirtySizeX = dirtyUntilX - dirtyFromX;
                    final int dirtySizeZ = dirtyUntilZ - dirtyFromZ;
                    // Sending the dirty area requires
                    //   dirtySizeX * dirtySizeZ * (4 + 4)
                    // bytes (2 = low + high byte).
                    // Sending a single changes requires
                    //   changes * (4 + 4 + 2)
                    // bytes (other 2 byte = coords).
                    // So at some point it'll be cheaper to just send the area:
                    // changes * (4 + 4 + 2) = dirtySizeX * dirtySizeZ * (4 + 4)
                    // changes = dirtySizeX * dirtySizeZ * (4 + 4) / (4 + 4 + 2) = dirtySizeX * dirtySizeZ * 0.8
                    // So if changes are larger than that, just send the full hologram.
                    if (dirty.size() > dirtySizeX * dirtySizeZ * 0.8)
                        PacketSender.sendHologramArea(this);
                    else
                        PacketSender.sendHologramValues(this);
                    resetDirtyFlag();
                }
            }
            if (getLevel().getGameTime() % Settings.get().tickFrequency == 0) {
                if (litRatio < 0) {
                    synchronized (this) {
                        litRatio = 0;
                        for (int value : volume) {
                            if (value != 0) litRatio += 1;
                        }
                        litRatio /= volume.length;
                    }
                }

                final boolean hadPower = hasPower;
                final double neededPower = Settings.get().hologramCost * litRatio * scale * Settings.get().tickFrequency;
                hasPower = node.tryChangeBuffer(-neededPower);
                if (hasPower != hadPower) {
                    PacketSender.sendHologramPowerChange(this);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private static double maxScale() {
        double max = 0;
        for (double value : Settings.get().hologramMaxScaleByTier) max = Math.max(max, value);
        return max;
    }

    /** Client side only: maximum render distance for the block entity renderer (was a TileEntity override). */
    public double getViewDistance() {
        return scale / maxScale() * Settings.get().hologramRenderDistance;
    }

    public double getFadeStartDistanceSquared() {
        return scale / maxScale() * Settings.get().hologramFadeStartDistance * Settings.get().hologramFadeStartDistance;
    }

    /**
     * Client side only. On Forge this overrides {@code IForgeBlockEntity#getRenderBoundingBox} by
     * signature; elsewhere renderers may call it.
     */
    public AABB getRenderBoundingBox() {
        final double cx = x() + 0.5;
        final double cy = y() + 0.5;
        final double cz = z() + 0.5;
        final double sh = width / 16 * scale * Sqrt2;
        // overscale to take into account 45 degree rotation
        final double sv = height / 16 * scale * Sqrt2;
        return new AABB(
            cx + (-0.5 + translation.x) * sh,
            cy + translation.y * sv,
            cz + (-0.5 + translation.z) * sh,
            cx + (0.5 + translation.x) * sh,
            cy + (1 + translation.y) * sv,
            cz + (0.5 + translation.x) * sh);
    }

    // ----------------------------------------------------------------------- //

    private String dataPath() {
        return node.address() + "_data";
    }

    @Override
    public void loadForServer(CompoundTag nbt) {
        tier = Math.min(Math.max(nbt.getByte(TierTag), 0), 1);
        super.loadForServer(nbt);
        final CompoundTag tag = SaveHandler.loadNBT(nbt, dataPath());
        final int[] savedVolume = tag.getIntArray(VolumeTag);
        System.arraycopy(savedVolume, 0, volume, 0, Math.min(savedVolume.length, volume.length));
        final int[] savedColors = tag.getIntArray(ColorsTag);
        final int[] colors = colors();
        for (int i = 0; i < savedColors.length && i < colors.length; i++) colors[i] = convertColor(savedColors[i]);
        scale = nbt.getDouble(ScaleTag);
        final double tx = nbt.getDouble(OffsetXTag);
        final double ty = nbt.getDouble(OffsetYTag);
        final double tz = nbt.getDouble(OffsetZTag);
        translation = new Vec3(tx, ty, tz);
        rotationAngle = nbt.getFloat(RotationAngleTag);
        rotationX = nbt.getFloat(RotationXTag);
        rotationY = nbt.getFloat(RotationYTag);
        rotationZ = nbt.getFloat(RotationZTag);
        rotationSpeed = nbt.getFloat(RotationSpeedTag);
        rotationSpeedX = nbt.getFloat(RotationSpeedXTag);
        rotationSpeedY = nbt.getFloat(RotationSpeedYTag);
        rotationSpeedZ = nbt.getFloat(RotationSpeedZTag);
    }

    @Override
    public synchronized void saveForServer(CompoundTag nbt) {
        nbt.putByte(TierTag, (byte) tier);
        super.saveForServer(nbt);
        SaveHandler.scheduleSave(getLevel(), x(), z(), nbt, dataPath(), tag -> {
            tag.putIntArray(VolumeTag, volume);
            final int[] colors = colors();
            final int[] converted = new int[colors.length];
            for (int i = 0; i < colors.length; i++) converted[i] = convertColor(colors[i]);
            tag.putIntArray(ColorsTag, converted);
        });
        nbt.putDouble(ScaleTag, scale);
        nbt.putDouble(OffsetXTag, translation.x);
        nbt.putDouble(OffsetYTag, translation.y);
        nbt.putDouble(OffsetZTag, translation.z);
        nbt.putFloat(RotationAngleTag, rotationAngle);
        nbt.putFloat(RotationXTag, rotationX);
        nbt.putFloat(RotationYTag, rotationY);
        nbt.putFloat(RotationZTag, rotationZ);
        nbt.putFloat(RotationSpeedTag, rotationSpeed);
        nbt.putFloat(RotationSpeedXTag, rotationSpeedX);
        nbt.putFloat(RotationSpeedYTag, rotationSpeedY);
        nbt.putFloat(RotationSpeedZTag, rotationSpeedZ);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        final int[] savedVolume = nbt.getIntArray(VolumeTag);
        System.arraycopy(savedVolume, 0, volume, 0, Math.min(savedVolume.length, volume.length));
        final int[] savedColors = nbt.getIntArray(ColorsTag);
        final int[] colors = colors();
        System.arraycopy(savedColors, 0, colors, 0, Math.min(savedColors.length, colors.length));
        scale = nbt.getDouble(ScaleTag);
        hasPower = nbt.getBoolean(HasPowerTag);
        final double tx = nbt.getDouble(OffsetXTag);
        final double ty = nbt.getDouble(OffsetYTag);
        final double tz = nbt.getDouble(OffsetZTag);
        translation = new Vec3(tx, ty, tz);
        rotationAngle = nbt.getFloat(RotationAngleTag);
        rotationX = nbt.getFloat(RotationXTag);
        rotationY = nbt.getFloat(RotationYTag);
        rotationZ = nbt.getFloat(RotationZTag);
        rotationSpeed = nbt.getFloat(RotationSpeedTag);
        rotationSpeedX = nbt.getFloat(RotationSpeedXTag);
        rotationSpeedY = nbt.getFloat(RotationSpeedYTag);
        rotationSpeedZ = nbt.getFloat(RotationSpeedZTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        nbt.putIntArray(VolumeTag, volume);
        nbt.putIntArray(ColorsTag, colors());
        nbt.putDouble(ScaleTag, scale);
        nbt.putBoolean(HasPowerTag, hasPower);
        nbt.putDouble(OffsetXTag, translation.x);
        nbt.putDouble(OffsetYTag, translation.y);
        nbt.putDouble(OffsetZTag, translation.z);
        nbt.putFloat(RotationAngleTag, rotationAngle);
        nbt.putFloat(RotationXTag, rotationX);
        nbt.putFloat(RotationYTag, rotationY);
        nbt.putFloat(RotationZTag, rotationZ);
        nbt.putFloat(RotationSpeedTag, rotationSpeed);
        nbt.putFloat(RotationSpeedXTag, rotationSpeedX);
        nbt.putFloat(RotationSpeedYTag, rotationSpeedY);
        nbt.putFloat(RotationSpeedZTag, rotationSpeedZ);
    }
}
