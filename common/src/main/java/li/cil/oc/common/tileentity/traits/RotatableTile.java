package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Like Rotatable, but stores the rotation information in the TE's NBT instead
 * of the block's metadata.
 */
public interface RotatableTile extends Rotatable {
    final class State {
        /** One of Up, Down and North (where north means forward/no pitch). */
        public Direction pitch = Direction.NORTH;

        /** One of the four cardinal directions. */
        public Direction yaw = Direction.SOUTH;
    }

    String PitchTag = Settings.namespace + "pitch";
    String YawTag = Settings.namespace + "yaw";

    /** Provided by {@link TileEntity}. */
    State rotatableTileState();

    // ----------------------------------------------------------------------- //
    // Accessors
    // ----------------------------------------------------------------------- //

    @Override
    default Direction pitch() {
        return rotatableTileState().pitch;
    }

    @Override
    default Direction yaw() {
        return rotatableTileState().yaw;
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(RotatableTile self, CompoundTag nbt) {
        if (nbt.contains(PitchTag)) {
            self.setPitch(Direction.from3DDataValue(nbt.getInt(PitchTag)));
        }
        if (nbt.contains(YawTag)) {
            self.setYaw(Direction.from3DDataValue(nbt.getInt(YawTag)));
        }
        validatePitchAndYaw(self);
    }

    static void onSaveForServer(RotatableTile self, CompoundTag nbt) {
        nbt.putInt(PitchTag, self.pitch().ordinal());
        nbt.putInt(YawTag, self.yaw().ordinal());
    }

    static void onLoadForClient(RotatableTile self, CompoundTag nbt) {
        self.setPitch(Direction.from3DDataValue(nbt.getInt(PitchTag)));
        self.setYaw(Direction.from3DDataValue(nbt.getInt(YawTag)));
        validatePitchAndYaw(self);
    }

    static void onSaveForClient(RotatableTile self, CompoundTag nbt) {
        nbt.putInt(PitchTag, self.pitch().ordinal());
        nbt.putInt(YawTag, self.yaw().ordinal());
    }

    private static void validatePitchAndYaw(RotatableTile self) {
        final State state = self.rotatableTileState();
        if (!state.pitch.getAxis().isVertical()) {
            state.pitch = Direction.NORTH;
        }
        if (!state.yaw.getAxis().isHorizontal()) {
            state.yaw = Direction.SOUTH;
        }
        self.updateTranslation();
    }

    // ----------------------------------------------------------------------- //

    /** Validates new values against the allowed rotations as set in our block. */
    @Override
    default boolean trySetPitchYaw(Direction pitch, Direction yaw) {
        final State state = rotatableTileState();
        boolean changed = false;
        if (pitch != state.pitch) {
            changed = true;
            state.pitch = pitch;
        }
        if (yaw != state.yaw) {
            changed = true;
            state.yaw = yaw;
        }
        if (changed) {
            updateTranslation();
        }
        return changed;
    }
}
