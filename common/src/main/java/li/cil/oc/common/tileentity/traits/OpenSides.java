package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.util.RotationHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;

/**
 * @author Vexatos
 */
public interface OpenSides extends TileEntityTrait {
    final class State {
        public volatile boolean[] openSides;
    }

    String OpenSidesTag = Settings.namespace + "openSides";

    /** Provided by {@link TileEntity}. */
    State openSidesState();

    default int SideCount() {
        return RotationHelper.getNumDirections();
    }

    default boolean defaultState() {
        return false;
    }

    default boolean[] openSides() {
        final State state = openSidesState();
        if (state.openSides == null) {
            final boolean[] sides = new boolean[SideCount()];
            Arrays.fill(sides, defaultState());
            state.openSides = sides;
        }
        return state.openSides;
    }

    default void setOpenSides(boolean[] value) {
        openSidesState().openSides = value;
    }

    default byte compressSides() {
        final boolean[] openSides = openSides();
        int acc = 0;
        for (Direction side : Direction.values()) {
            if (side.ordinal() < openSides.length && openSides[side.ordinal()]) acc |= 1 << side.ordinal();
        }
        return (byte) acc;
    }

    default boolean[] uncompressSides(byte value) {
        final Direction[] sides = Direction.values();
        final boolean[] result = new boolean[sides.length];
        for (Direction d : sides) result[d.ordinal()] = ((1 << d.ordinal()) & value) != 0;
        return result;
    }

    default boolean isSideOpen(Direction side) {
        return side != null && openSides()[side.ordinal()];
    }

    default void setSideOpen(Direction side, boolean value) {
        if (side != null && openSides()[side.ordinal()] != value) {
            openSides()[side.ordinal()] = value;
        }
    }

    static void onLoadForServer(OpenSides self, CompoundTag nbt) {
        if (nbt.contains(OpenSidesTag))
            self.setOpenSides(self.uncompressSides(nbt.getByte(OpenSidesTag)));
    }

    static void onSaveForServer(OpenSides self, CompoundTag nbt) {
        nbt.putByte(OpenSidesTag, self.compressSides());
    }

    static void onLoadForClient(OpenSides self, CompoundTag nbt) {
        self.setOpenSides(self.uncompressSides(nbt.getByte(OpenSidesTag)));
    }

    static void onSaveForClient(OpenSides self, CompoundTag nbt) {
        nbt.putByte(OpenSidesTag, self.compressSides());
    }
}
