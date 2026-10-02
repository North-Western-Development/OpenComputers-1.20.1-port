package li.cil.oc.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Scala trait extending AbstractManagedEnvironment; every implementer uses it as
 * its primary superclass, so it became an abstract class. The former trait vars
 * are exposed both as public fields and as {@code x()}/{@code setX(v)} accessors.
 */
public abstract class RedstoneSignaller extends AbstractManagedEnvironment {
    public int wakeThreshold = 0;

    public boolean wakeNeighborsOnly = true;

    protected RedstoneSignaller() {
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("redstone", Visibility.Neighbors).
            create());
    }

    @Override
    public Component node() {
        return (Component) super.node();
    }

    public int wakeThreshold() {
        return wakeThreshold;
    }

    public void setWakeThreshold(int value) {
        wakeThreshold = value;
    }

    public boolean wakeNeighborsOnly() {
        return wakeNeighborsOnly;
    }

    public void setWakeNeighborsOnly(boolean value) {
        wakeNeighborsOnly = value;
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():number -- Get the current wake-up threshold.")
    public Object[] getWakeThreshold(Context context, Arguments args) {
        return ResultWrapper.result(wakeThreshold);
    }

    @Callback(doc = "function(threshold:number):number -- Set the wake-up threshold.")
    public Object[] setWakeThreshold(Context context, Arguments args) {
        final int oldThreshold = wakeThreshold;
        wakeThreshold = args.checkInteger(0);
        return ResultWrapper.result(oldThreshold);
    }

    // ----------------------------------------------------------------------- //

    public void onRedstoneChanged(RedstoneChangedEventArgs args) {
        final Object side = args.side == null ? "wireless" : (Object) args.side.ordinal();
        final List<Object> flatArgs = new ArrayList<>(List.of("redstone_changed", side, args.oldValue, args.newValue));
        if (args.color >= 0)
            flatArgs.add(args.color);
        node().sendToReachable("computer.signal", flatArgs.toArray());
        if (args.oldValue < wakeThreshold && args.newValue >= wakeThreshold) {
            if (wakeNeighborsOnly)
                node().sendToNeighbors("computer.start");
            else
                node().sendToReachable("computer.start");
        }
    }

    // ----------------------------------------------------------------------- //

    private static final String WakeThresholdNbt = "wakeThreshold";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        wakeThreshold = nbt.getInt(WakeThresholdNbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        nbt.putInt(WakeThresholdNbt, wakeThreshold);
    }
}
