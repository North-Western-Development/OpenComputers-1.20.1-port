package li.cil.oc.common.item;

import java.util.Collections;
import java.util.List;
import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.SimpleItem;

public class TerminalServer extends SimpleItem {
    public TerminalServer(Properties props) {
        super(props);
    }

    @Override
    protected List<Object> tooltipData() {
        return Collections.singletonList(Settings.get().terminalsPerServer);
    }
}
