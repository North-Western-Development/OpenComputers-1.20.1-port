package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

public interface TextBuffer extends Environment, Tickable {
    final class State {
        public volatile li.cil.oc.api.internal.TextBuffer buffer;
    }

    /** Provided by {@link TileEntity}. */
    State textBufferState();

    /** Scala {@code lazy val buffer}. */
    default li.cil.oc.api.internal.TextBuffer buffer() {
        final State state = textBufferState();
        if (state.buffer == null) {
            synchronized (state) {
                if (state.buffer == null) {
                    final ItemStack screenItem = li.cil.oc.api.Items.get(Constants.BlockName.ScreenTier1).createItemStack(1);
                    final li.cil.oc.api.internal.TextBuffer buffer = (li.cil.oc.api.internal.TextBuffer) li.cil.oc.api.Driver.driverFor(screenItem, getClass()).createEnvironment(screenItem, this);
                    final Pair<Integer, Integer> maxResolution = Settings.screenResolutionsByTier[tier()];
                    buffer.setMaximumResolution(maxResolution.getLeft(), maxResolution.getRight());
                    buffer.setMaximumColorDepth(Settings.screenDepthsByTier[tier()]);
                    state.buffer = buffer;
                }
            }
        }
        return state.buffer;
    }

    @Override
    default Node node() {
        return buffer().node();
    }

    int tier();

    static void onUpdateEntity(TextBuffer self) {
        if (self.isClient() || self.isConnected()) {
            self.buffer().update();
        }
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(TextBuffer self, CompoundTag nbt) {
        self.buffer().loadData(nbt);
    }

    static void onSaveForServer(TextBuffer self, CompoundTag nbt) {
        self.buffer().saveData(nbt);
    }

    static void onLoadForClient(TextBuffer self, CompoundTag nbt) {
        self.buffer().loadData(nbt);
    }

    static void onSaveForClient(TextBuffer self, CompoundTag nbt) {
        self.buffer().saveData(nbt);
    }
}
