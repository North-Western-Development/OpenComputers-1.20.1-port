package li.cil.oc.common.item;

import java.util.Optional;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;

public class UpgradeStickyPiston extends SimpleItem implements ItemTier {
    public UpgradeStickyPiston(Properties props) {
        super(props);
    }

    @Override
    protected Optional<String> tooltipName() {
        return Optional.ofNullable(unlocalizedName);
    }
}
