package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.IMC;
import li.cil.oc.util.ExtendedAABB;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PrintData extends ItemData {
    public PrintData() {
        super(Constants.BlockName.Print);
    }

    public PrintData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public Optional<String> label = Optional.empty();
    public Optional<String> tooltip = Optional.empty();
    public boolean isButtonMode = false;
    public int redstoneLevel = 0;
    public boolean pressurePlate = false;
    public final Set<Shape> stateOff = new HashSet<>();
    public final Set<Shape> stateOn = new HashSet<>();
    public boolean isBeaconBase = false;
    public int lightLevel = 0;
    public boolean noclipOff = false;
    public boolean noclipOn = false;

    public int complexity() {
        return Math.max(stateOn.size(), stateOff.size());
    }

    public boolean hasActiveState() {
        return !stateOn.isEmpty();
    }

    public boolean emitLight() {
        return lightLevel > 0;
    }

    public boolean emitRedstone() {
        return redstoneLevel > 0;
    }

    public boolean emitRedstone(boolean state) {
        return state ? emitRedstoneWhenOn() : emitRedstoneWhenOff();
    }

    public boolean emitRedstoneWhenOff() {
        return emitRedstone() && !hasActiveState();
    }

    public boolean emitRedstoneWhenOn() {
        return emitRedstone() && hasActiveState();
    }

    public float opacity() {
        if (opacityDirty) {
            opacityDirty = false;
            opacity_ = Math.min(computeApproximateOpacity(stateOn), computeApproximateOpacity(stateOff));
        }
        return opacity_;
    }

    // lazily computed and stored, because potentially slow
    private float opacity_ = 0f;
    private boolean opacityDirty = true;

    private static final String LabelTag = "label";
    private static final String TooltipTag = "tooltip";
    private static final String IsButtonModeTag = "isButtonMode";
    private static final String RedstoneLevelTag = "redstoneLevel";
    private static final String RedstoneLevelTagCompat = "emitRedstone";
    private static final String PressurePlateTag = "pressurePlate";
    private static final String StateOffTag = "stateOff";
    private static final String StateOnTag = "stateOn";
    private static final String IsBeaconBaseTag = "isBeaconBase";
    private static final String LightLevelTag = "lightLevel";
    private static final String NoclipOffTag = "noclipOff";
    private static final String NoclipOnTag = "noclipOn";

    @Override
    public void loadData(CompoundTag nbt) {
        label = nbt.contains(LabelTag) ? Optional.of(nbt.getString(LabelTag)) : Optional.empty();
        tooltip = nbt.contains(TooltipTag) ? Optional.of(nbt.getString(TooltipTag)) : Optional.empty();
        isButtonMode = nbt.getBoolean(IsButtonModeTag);
        redstoneLevel = Math.min(Math.max(nbt.getInt(RedstoneLevelTag), 0), 15);
        if (nbt.getBoolean(RedstoneLevelTagCompat)) redstoneLevel = 15;
        pressurePlate = nbt.getBoolean(PressurePlateTag);
        stateOff.clear();
        stateOff.addAll(ExtendedNBT.map(nbt.getList(StateOffTag, Tag.TAG_COMPOUND), PrintData::nbtToShape));
        stateOn.clear();
        stateOn.addAll(ExtendedNBT.map(nbt.getList(StateOnTag, Tag.TAG_COMPOUND), PrintData::nbtToShape));
        isBeaconBase = nbt.getBoolean(IsBeaconBaseTag);
        lightLevel = Math.min(Math.max(nbt.getByte(LightLevelTag) & 0xFF, 0), 15);
        noclipOff = nbt.getBoolean(NoclipOffTag);
        noclipOn = nbt.getBoolean(NoclipOnTag);
        opacityDirty = true;
    }

    @Override
    public void saveData(CompoundTag nbt) {
        label.ifPresent(v -> nbt.putString(LabelTag, v));
        tooltip.ifPresent(v -> nbt.putString(TooltipTag, v));
        nbt.putBoolean(IsButtonModeTag, isButtonMode);
        nbt.putInt(RedstoneLevelTag, redstoneLevel);
        nbt.putBoolean(PressurePlateTag, pressurePlate);
        // Shapes are kept in (unordered) sets, but NBT list comparison considers the order: sort them,
        // so identical prints get identical NBT and stack (KosmosPrime fork).
        ExtendedNBT.setNewTagList(nbt, StateOffTag, stateOff.stream().sorted(ShapeOrder).map(PrintData::shapeToNBT).toList());
        ExtendedNBT.setNewTagList(nbt, StateOnTag, stateOn.stream().sorted(ShapeOrder).map(PrintData::shapeToNBT).toList());
        nbt.putBoolean(IsBeaconBaseTag, isBeaconBase);
        nbt.putByte(LightLevelTag, (byte) lightLevel);
        nbt.putBoolean(NoclipOffTag, noclipOff);
        nbt.putBoolean(NoclipOnTag, noclipOn);
    }

    // ----------------------------------------------------------------------- //

    // The following logic is used to approximate the opacity of a print, for
    // which we use the volume as a heuristic. Because computing the actual
    // volume is a) expensive b) not necessarily a good heuristic (e.g. a
    // "dotted grid") we take a shortcut and divide the space into a few
    // sub-sections, for each of which we check if there's anything in it.
    // If so, we consider that area "opaque". To compensate, prints can never
    // be fully light-opaque. This gives a little bit of shading as a nice
    // effect, but avoid it looking derpy when there are only a few sparse
    // shapes in the model.
    private static final int stepping = 4;
    private static final float step = stepping / 16f;
    private static final float invMaxVolume = 1f / (stepping * stepping * stepping);

    private static final Set<Method> inkProviders = new LinkedHashSet<>();

    public static void addInkProvider(Method provider) {
        inkProviders.add(provider);
    }

    public static float computeApproximateOpacity(Iterable<Shape> shapes) {
        float volume = 1f;
        if (shapes.iterator().hasNext()) {
            for (int x = 0; x < 16 / stepping; x++) {
                for (int y = 0; y < 16 / stepping; y++) {
                    for (int z = 0; z < 16 / stepping; z++) {
                        final AABB bounds = new AABB(
                            x * step, y * step, z * step,
                            (x + 1) * step, (y + 1) * step, (z + 1) * step);
                        boolean any = false;
                        for (Shape shape : shapes) {
                            if (shape.bounds.intersects(bounds)) {
                                any = true;
                                break;
                            }
                        }
                        if (!any) {
                            volume -= invMaxVolume;
                        }
                    }
                }
            }
        }
        return volume;
    }

    /** @return (materialRequired, inkRequired), if the print has any volume. */
    public static Optional<Pair<Integer, Integer>> computeCosts(PrintData data) {
        int totalVolume = 0;
        int totalSurface = 0;
        for (Shape shape : data.stateOn) {
            totalVolume += ExtendedAABB.volume(shape.bounds);
            totalSurface += ExtendedAABB.surface(shape.bounds);
        }
        for (Shape shape : data.stateOff) {
            totalVolume += ExtendedAABB.volume(shape.bounds);
            totalSurface += ExtendedAABB.surface(shape.bounds);
        }
        final double multiplier = data.noclipOff || data.noclipOn ? Settings.get().noclipMultiplier : 1;

        if (totalVolume > 0) {
            final int baseMaterialRequired = Math.max(totalVolume / 2, 1);
            final int materialRequired =
                data.redstoneLevel > 0 && data.redstoneLevel < 15
                    ? baseMaterialRequired + Settings.get().printCustomRedstone
                    : baseMaterialRequired;
            final int inkRequired = Math.max(totalSurface / 6, 1);

            return Optional.of(Pair.of((int) (materialRequired * multiplier), inkRequired));
        } else return Optional.empty();
    }

    public static int materialValue(ItemStack stack) {
        final int materialPerItem = Settings.get().printMaterialValue;
        if (li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.Chamelium)) {
            return materialPerItem;
        } else if (li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.Print)) {
            final PrintData data = new PrintData(stack);
            return computeCosts(data).map(costs -> (int) (costs.getLeft() * Settings.get().printRecycleRate)).orElse(0);
        } else return 0;
    }

    public static int inkValue(ItemStack stack) {
        for (Method provider : inkProviders) {
            final int value = IMC.tryInvokeStatic(provider, 0, stack);
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    public static Shape nbtToShape(CompoundTag nbt) {
        final AABB aabb;
        if (nbt.contains("minX")) {
            // Compatibility with shapes created with earlier dev-builds.
            final float minX = nbt.getByte("minX") / 16f;
            final float minY = nbt.getByte("minY") / 16f;
            final float minZ = nbt.getByte("minZ") / 16f;
            final float maxX = nbt.getByte("maxX") / 16f;
            final float maxY = nbt.getByte("maxY") / 16f;
            final float maxZ = nbt.getByte("maxZ") / 16f;
            aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            final byte[] raw = nbt.getByteArray("bounds");
            final byte[] bounds = new byte[Math.max(6, raw.length)];
            System.arraycopy(raw, 0, bounds, 0, raw.length);
            final float minX = bounds[0] / 16f;
            final float minY = bounds[1] / 16f;
            final float minZ = bounds[2] / 16f;
            final float maxX = bounds[3] / 16f;
            final float maxY = bounds[4] / 16f;
            final float maxZ = bounds[5] / 16f;
            aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        final String texture = nbt.getString("texture");
        final Optional<Integer> tint = nbt.contains("tint") ? Optional.of(nbt.getInt("tint")) : Optional.empty();
        return new Shape(aabb, texture, tint);
    }

    private static final java.util.Comparator<Shape> ShapeOrder = java.util.Comparator
            .<Shape>comparingDouble(shape -> shape.bounds.minX)
            .thenComparingDouble(shape -> shape.bounds.minY)
            .thenComparingDouble(shape -> shape.bounds.minZ)
            .thenComparingDouble(shape -> shape.bounds.maxX)
            .thenComparingDouble(shape -> shape.bounds.maxY)
            .thenComparingDouble(shape -> shape.bounds.maxZ)
            .thenComparing(shape -> shape.tint.orElse(null), java.util.Comparator.nullsFirst(java.util.Comparator.<Integer>naturalOrder()))
            .thenComparing(shape -> shape.texture, java.util.Comparator.nullsFirst(java.util.Comparator.<String>naturalOrder()));

    public static CompoundTag shapeToNBT(Shape shape) {
        final CompoundTag nbt = new CompoundTag();
        nbt.putByteArray("bounds", new byte[]{
            (byte) Math.round(shape.bounds.minX * 16),
            (byte) Math.round(shape.bounds.minY * 16),
            (byte) Math.round(shape.bounds.minZ * 16),
            (byte) Math.round(shape.bounds.maxX * 16),
            (byte) Math.round(shape.bounds.maxY * 16),
            (byte) Math.round(shape.bounds.maxZ * 16)
        });
        nbt.putString("texture", shape.texture);
        shape.tint.ifPresent(t -> nbt.putInt("tint", t));
        return nbt;
    }

    /** Identity semantics (like the Scala class), so equal-looking shapes are kept separately. */
    public static class Shape {
        public final AABB bounds;
        public final String texture;
        public final Optional<Integer> tint;

        public Shape(AABB bounds, String texture, Optional<Integer> tint) {
            this.bounds = bounds;
            this.texture = texture;
            this.tint = tint;
        }
    }
}
