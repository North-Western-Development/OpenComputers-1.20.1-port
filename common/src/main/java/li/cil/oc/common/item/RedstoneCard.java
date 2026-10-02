package li.cil.oc.common.item;

import java.util.Optional;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;

public class RedstoneCard extends SimpleItem implements ItemTier {
    public final int tier;

    public RedstoneCard(Properties props, int tier) {
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

    // Formerly fillItemCategory: tier two is only listed in the creative tab if
    // ModOpenComputers.hasRedstoneCardT2; see Items.decorateCreativeTab.
}
