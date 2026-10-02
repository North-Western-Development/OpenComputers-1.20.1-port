package li.cil.oc.common.item;

import java.util.Optional;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.world.item.ItemStack;

public class DroneCase extends SimpleItem implements ItemTier {
    public final int tier;

    public DroneCase(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    protected Optional<String> tooltipName() {
        return Optional.ofNullable(unlocalizedName);
    }

    @Override
    protected int tierFromDriver(ItemStack stack) {
        return tier;
    }
}
