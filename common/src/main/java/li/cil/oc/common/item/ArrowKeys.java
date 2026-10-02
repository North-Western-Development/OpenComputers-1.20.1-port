package li.cil.oc.common.item;

import li.cil.oc.common.item.traits.SimpleItem;
import java.util.Optional;

public class ArrowKeys extends SimpleItem {
    public ArrowKeys(Properties props) {
        super(props);
    }

    @Override
    protected Optional<String> tooltipName() {
        return Optional.empty();
    }
}
