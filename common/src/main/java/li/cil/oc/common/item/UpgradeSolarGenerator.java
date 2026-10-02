package li.cil.oc.common.item;

import java.util.Collections;
import java.util.List;
import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;

public class UpgradeSolarGenerator extends SimpleItem implements ItemTier {
    public UpgradeSolarGenerator(Properties props) {
        super(props);
    }

    @Override
    protected List<Object> tooltipData() {
        return Collections.singletonList((int) (Settings.get().solarGeneratorEfficiency * 100));
    }
}
