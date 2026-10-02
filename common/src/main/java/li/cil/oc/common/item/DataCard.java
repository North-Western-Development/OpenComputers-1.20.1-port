package li.cil.oc.common.item;

import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;

public class DataCard extends SimpleItem implements ItemTier {
    public final int tier;

    public DataCard(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }
}
