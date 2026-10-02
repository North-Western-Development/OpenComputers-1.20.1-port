package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.Color;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;

public interface Colored extends TileEntityTrait, li.cil.oc.api.internal.Colored {
    final class State {
        public volatile int color = 0;
    }

    String RenderColorTag = Settings.namespace + "renderColorRGB";
    String RenderColorTagCompat = Settings.namespace + "renderColor";

    /** Provided by {@link TileEntity}. */
    State coloredState();

    default boolean consumesDye() {
        return false;
    }

    @Override
    default int getColor() {
        return coloredState().color;
    }

    @Override
    default void setColor(int value) {
        final State state = coloredState();
        if (value != state.color) {
            state.color = value;
            onColorChanged();
        }
    }

    @Override
    default boolean controlsConnectivity() {
        return false;
    }

    default void onColorChanged() {
        if (getLevel() != null && isServer()) {
            PacketSender.sendColorChange(this);
        }
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(Colored self, CompoundTag nbt) {
        final State state = self.coloredState();
        if (nbt.contains(RenderColorTagCompat)) {
            state.color = Color.rgbValues.get(DyeColor.byId(nbt.getInt(RenderColorTagCompat)));
        }
        if (nbt.contains(RenderColorTag)) {
            state.color = nbt.getInt(RenderColorTag);
        }
    }

    static void onSaveForServer(Colored self, CompoundTag nbt) {
        nbt.putInt(RenderColorTag, self.coloredState().color);
    }

    static void onLoadForClient(Colored self, CompoundTag nbt) {
        self.coloredState().color = nbt.getInt(RenderColorTag);
    }

    static void onSaveForClient(Colored self, CompoundTag nbt) {
        nbt.putInt(RenderColorTag, self.coloredState().color);
    }
}
