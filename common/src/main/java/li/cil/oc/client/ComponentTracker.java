package li.cil.oc.client;

import net.minecraft.world.level.Level;

public final class ComponentTracker extends li.cil.oc.common.ComponentTracker {
    public static final ComponentTracker INSTANCE = new ComponentTracker();

    private ComponentTracker() {
    }

    @Override
    protected void clear(Level world) {
        if (world.isClientSide()) super.clear(world);
    }
}
