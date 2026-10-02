package li.cil.oc.common.tileentity.traits;

import net.minecraft.core.Direction;

/** Scala case class {@code RedstoneChangedEventArgs(side, oldValue, newValue, color = -1)}. */
public final class RedstoneChangedEventArgs {
    public final Direction side;
    public final int oldValue;
    public final int newValue;
    public final int color;

    public RedstoneChangedEventArgs(Direction side, int oldValue, int newValue, int color) {
        this.side = side;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.color = color;
    }

    public RedstoneChangedEventArgs(Direction side, int oldValue, int newValue) {
        this(side, oldValue, newValue, -1);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RedstoneChangedEventArgs that && that.side == side && that.oldValue == oldValue && that.newValue == newValue && that.color == color;
    }

    @Override
    public int hashCode() {
        int result = side != null ? side.hashCode() : 0;
        result = 31 * result + oldValue;
        result = 31 * result + newValue;
        result = 31 * result + color;
        return result;
    }

    @Override
    public String toString() {
        return "RedstoneChangedEventArgs(" + side + "," + oldValue + "," + newValue + "," + color + ")";
    }
}
