package li.cil.oc.server.component;

import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.nbt.CompoundTag;

/** Formerly declared in FileSystem.scala. */
public final class HandleValue extends AbstractValue {
    public String owner = "";
    public int handle = 0;

    public HandleValue() {
    }

    public HandleValue(String owner, int handle) {
        this.owner = owner;
        this.handle = handle;
    }

    @Override
    public void dispose(Context context) {
        super.dispose(context);
        if (context.node() != null && context.node().network() != null) {
            final Node node = context.node().network().node(owner);
            if (node != null && node.host() instanceof FileSystem fs) {
                try {
                    fs.close(context, handle);
                } catch (Throwable ignored) {
                    // Ignore, already closed.
                }
            }
        }
    }

    private static final String OwnerTag = "owner";
    private static final String HandleTag = "handle";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        owner = nbt.getString(OwnerTag);
        handle = nbt.getInt(HandleTag);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        nbt.putString(OwnerTag, owner);
        nbt.putInt(HandleTag, handle);
    }

    @Override
    public String toString() {
        return Integer.toString(handle);
    }
}
