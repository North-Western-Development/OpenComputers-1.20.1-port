package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;

import javax.annotation.Nullable;

public class StaticComponentSlot extends ComponentSlot {
    public final Player agentContainer;
    public final String slot;
    public final int tier;

    public StaticComponentSlot(Player agentContainer, Container inventory, int index, int x, int y, @Nullable Class<? extends EnvironmentHost> host, String slot, int tier) {
        super(inventory, index, x, y, host);
        this.agentContainer = agentContainer;
        this.slot = slot;
        this.tier = tier;
    }

    @Override
    public Player agentContainer() {
        return agentContainer;
    }

    @Override
    public String slot() {
        return slot;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public ResourceLocation tierIcon() {
        return SlotIcons.get(tier);
    }

    @Override
    public ResourceLocation getBackgroundLocation() {
        return SlotIcons.get(slot);
    }

    @Override
    public int getMaxStackSize() {
        if (li.cil.oc.common.Slot.Tool.equals(slot) || li.cil.oc.common.Slot.Any.equals(slot) || li.cil.oc.common.Slot.Filtered.equals(slot)) {
            return super.getMaxStackSize();
        }
        if (li.cil.oc.common.Slot.None.equals(slot)) return 0;
        return 1;
    }
}
