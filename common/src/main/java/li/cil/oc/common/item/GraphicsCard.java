package li.cil.oc.common.item;

import java.util.List;
import java.util.Optional;
import li.cil.oc.common.item.traits.GPULike;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;

public class GraphicsCard extends SimpleItem implements ItemTier, GPULike {
    public final int tier;

    public GraphicsCard(Properties props, int tier) {
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
    public int gpuTier() {
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        return gpuTooltipData();
    }
}
