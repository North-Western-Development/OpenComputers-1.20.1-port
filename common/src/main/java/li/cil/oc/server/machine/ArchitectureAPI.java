package li.cil.oc.server.machine;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

public abstract class ArchitectureAPI {
    public final li.cil.oc.api.machine.Machine machine;

    protected ArchitectureAPI(li.cil.oc.api.machine.Machine machine) {
        this.machine = machine;
    }

    protected Node node() {
        return machine.node();
    }

    protected Map<String, String> components() {
        return machine.components();
    }

    public abstract void initialize();

    public void loadData(CompoundTag nbt) {
    }

    public void saveData(CompoundTag nbt) {
    }
}
