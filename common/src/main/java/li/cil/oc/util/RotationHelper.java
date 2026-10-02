package li.cil.oc.util;

import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;

public final class RotationHelper {
    private RotationHelper() {
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    public static int getNumDirections() {
        return DIRECTIONS.length;
    }

    public static Direction getFront(int index) {
        return DIRECTIONS[Math.floorMod(index, DIRECTIONS.length)];
    }

    public static Direction fromYaw(float yaw) {
        switch (Math.round(yaw / 360 * 4) & 3) {
            case 0:
                return Direction.SOUTH;
            case 1:
                return Direction.WEST;
            case 2:
                return Direction.NORTH;
            default:
                return Direction.EAST;
        }
    }

    public static Direction toLocal(Direction pitch, Direction yaw, Direction value) {
        return translationFor(pitch, yaw)[value.ordinal()];
    }

    public static Direction toGlobal(Direction pitch, Direction yaw, Direction value) {
        return inverseTranslationFor(pitch, yaw)[value.ordinal()];
    }

    public static Direction[] translationFor(Direction pitch, Direction yaw) {
        synchronized (translationCache) {
            return translationCache.
                    computeIfAbsent(pitch, k -> new EnumMap<>(Direction.class)).
                    computeIfAbsent(yaw, k -> translations[pitch.ordinal()][yaw.ordinal() - 2]);
        }
    }

    public static Direction[] inverseTranslationFor(Direction pitch, Direction yaw) {
        synchronized (inverseTranslationCache) {
            return inverseTranslationCache.
                    computeIfAbsent(pitch, k -> new EnumMap<>(Direction.class)).
                    computeIfAbsent(yaw, k -> {
                        final Direction[] t = translationFor(pitch, yaw);
                        final Direction[] result = new Direction[t.length];
                        for (int i = 0; i < t.length; i++) {
                            final Direction d = Direction.from3DDataValue(i);
                            int index = -1;
                            for (int j = 0; j < t.length; j++) {
                                if (t[j] == d) {
                                    index = j;
                                    break;
                                }
                            }
                            result[i] = Direction.from3DDataValue(index);
                        }
                        return result;
                    });
        }
    }

    // ----------------------------------------------------------------------- //

    private static final Map<Direction, Map<Direction, Direction[]>> translationCache = new EnumMap<>(Direction.class);
    private static final Map<Direction, Map<Direction, Direction[]>> inverseTranslationCache = new EnumMap<>(Direction.class);

    /** Shortcuts for forge directions to make the below more readable. */
    private static final Direction down = Direction.DOWN;
    private static final Direction up = Direction.UP;
    private static final Direction north = Direction.NORTH;
    private static final Direction south = Direction.SOUTH;
    private static final Direction west = Direction.WEST;
    private static final Direction east = Direction.EAST;

    /**
     * Translates forge directions based on the block's pitch and yaw. The base
     * forward direction is facing south with no pitch. The outer array is for
     * the three different pitch states, the inner for the four different yaw
     * states.
     */
    private static final Direction[][][] translations = {
            // Pitch = Down
            {
                    // Yaw = North
                    {south, north, up, down, east, west},
                    // Yaw = South
                    {south, north, down, up, west, east},
                    // Yaw = West
                    {south, north, west, east, up, down},
                    // Yaw = East
                    {south, north, east, west, down, up}},
            // Pitch = Up
            {
                    // Yaw = North
                    {north, south, down, up, east, west},
                    // Yaw = South
                    {north, south, up, down, west, east},
                    // Yaw = West
                    {north, south, west, east, down, up},
                    // Yaw = East
                    {north, south, east, west, up, down}},
            // Pitch = Forward (North|East|South|West)
            {
                    // Yaw = North
                    {down, up, south, north, east, west},
                    // Yaw = South
                    {down, up, north, south, west, east},
                    // Yaw = West
                    {down, up, west, east, south, north},
                    // Yaw = East
                    {down, up, east, west, north, south}}};
}
