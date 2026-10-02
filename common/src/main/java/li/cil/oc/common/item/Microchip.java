package li.cil.oc.common.item;

import java.util.Optional;
import li.cil.oc.common.item.traits.SimpleItem;

public class Microchip extends SimpleItem {
    public final int tier;

    public Microchip(Properties props, int tier) {
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
}
