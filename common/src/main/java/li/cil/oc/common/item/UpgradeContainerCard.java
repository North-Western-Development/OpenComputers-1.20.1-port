package li.cil.oc.common.item;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;

public class UpgradeContainerCard extends SimpleItem implements ItemTier {
    public final int tier;

    public UpgradeContainerCard(Properties props, int tier) {
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
    protected List<Object> tooltipData() {
        return Collections.singletonList(tier + 1);
    }
}
