package li.cil.oc.common.tileentity.traits;

import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ExtendedEnumFacing;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TileEntity base class for rotatable blocks.
 * <p>
 * Scala {@code pitch_=} / {@code yaw_=} are {@link #setPitch} / {@link #setYaw}.
 */
public interface Rotatable extends RotationAware, li.cil.oc.api.internal.Rotatable {
    // ----------------------------------------------------------------------- //
    // Lookup tables
    // ----------------------------------------------------------------------- //

    Direction[] pitch2Direction = {Direction.UP, Direction.NORTH, Direction.DOWN};

    Direction[] yaw2Direction = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

    // ----------------------------------------------------------------------- //
    // Accessors
    // ----------------------------------------------------------------------- //

    default Direction pitch() {
        final Level level = getLevel();
        if (level != null && level.isLoaded(getBlockPos())) {
            final BlockState state = level.getBlockState(getBlockPos());
            if (state.hasProperty(PropertyRotatable.Pitch)) return state.getValue(PropertyRotatable.Pitch);
            return Direction.NORTH;
        }
        return null;
    }

    default void setPitch(Direction value) {
        trySetPitchYaw(value == Direction.DOWN || value == Direction.UP ? value : Direction.NORTH, yaw());
    }

    default Direction yaw() {
        final Level level = getLevel();
        if (level != null && level.isLoaded(getBlockPos())) {
            final BlockState state = level.getBlockState(getBlockPos());
            if (state.hasProperty(PropertyRotatable.Yaw)) return state.getValue(PropertyRotatable.Yaw);
            if (state.hasProperty(PropertyRotatable.Facing)) return state.getValue(PropertyRotatable.Facing);
            return Direction.SOUTH;
        }
        return null;
    }

    default void setYaw(Direction value) {
        trySetPitchYaw(pitch(), value == Direction.DOWN || value == Direction.UP ? yaw() : value);
    }

    default boolean setFromEntityPitchAndYaw(Entity entity) {
        return trySetPitchYaw(
            pitch2Direction[Math.round(entity.getXRot() / 90) + 1],
            yaw2Direction[Math.round(entity.getYRot() / 360 * 4) & 3]);
    }

    default boolean setFromFacing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            return trySetPitchYaw(value, yaw());
        } else {
            return trySetPitchYaw(Direction.NORTH, value);
        }
    }

    default boolean invertRotation() {
        final Direction pitch = pitch();
        return trySetPitchYaw(pitch == Direction.DOWN || pitch == Direction.UP ? pitch.getOpposite() : Direction.NORTH, yaw().getOpposite());
    }

    @Override
    default Direction facing() {
        final Direction pitch = pitch();
        return pitch == Direction.DOWN || pitch == Direction.UP ? pitch : yaw();
    }

    default boolean rotate(Direction axis) {
        final Level level = getLevel();
        final BlockPos pos = getBlockPos();
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SimpleBlock simple) {
            final Direction[] valid = simple.getValidRotations(level, pos);
            boolean contains = false;
            if (valid != null) {
                for (Direction d : valid) {
                    if (d == axis) {
                        contains = true;
                        break;
                    }
                }
            }
            if (contains) {
                final Direction value = ExtendedEnumFacing.getRotation(facing(), axis);
                final Direction newPitch;
                final Direction newYaw;
                if (value == Direction.UP || value == Direction.DOWN) {
                    newPitch = value;
                    newYaw = value == pitch() ? ExtendedEnumFacing.getRotation(yaw(), axis) : yaw();
                } else {
                    newPitch = Direction.NORTH;
                    newYaw = value;
                }
                return trySetPitchYaw(newPitch, newYaw);
            } else {
                return false;
            }
        } else if (axis == Direction.UP || axis == Direction.DOWN) {
            final BlockState updated = state.rotate(axis == Direction.DOWN ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90);
            return updated != state && level.setBlockAndUpdate(pos, updated);
        } else {
            return false;
        }
    }

    @Override
    default Direction toLocal(Direction value) {
        if (value == null) return null;
        final Direction p = pitch();
        final Direction y = yaw();
        if (p != null && y != null) return RotationHelper.toLocal(p, y, value);
        else return null;
    }

    @Override
    default Direction toGlobal(Direction value) {
        if (value == null) return null;
        final Direction p = pitch();
        final Direction y = yaw();
        if (p != null && y != null) return RotationHelper.toGlobal(p, y, value);
        else return null;
    }

    default Direction[] validFacings() {
        return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    }

    // ----------------------------------------------------------------------- //

    default void onRotationChanged() {
        final Level level = getLevel();
        if (isServer()) {
            PacketSender.sendRotatableState(this);
        } else {
            ExtendedWorld.notifyBlockUpdate(level, getBlockPos());
        }
        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
    }

    // ----------------------------------------------------------------------- //

    /** Updates cached translation array and sends notification to clients. */
    default void updateTranslation() {
        if (getLevel() != null) {
            onRotationChanged();
        }
    }

    /** Validates new values against the allowed rotations as set in our block. */
    default boolean trySetPitchYaw(Direction pitch, Direction yaw) {
        final Level level = getLevel();
        final BlockPos pos = getBlockPos();
        final BlockState oldState = level.getBlockState(pos);
        final BlockState newState;
        if (oldState.hasProperty(PropertyRotatable.Pitch) && oldState.hasProperty(PropertyRotatable.Yaw)) {
            newState = oldState.setValue(PropertyRotatable.Pitch, pitch).setValue(PropertyRotatable.Yaw, yaw);
        } else if (oldState.hasProperty(PropertyRotatable.Facing)) {
            newState = oldState.setValue(PropertyRotatable.Facing, yaw);
        } else {
            return false;
        }
        if (oldState != newState) {
            level.setBlockAndUpdate(pos, newState);
            updateTranslation();
            return true;
        } else return false;
    }
}
