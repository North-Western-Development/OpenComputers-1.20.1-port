package li.cil.oc.util;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DatabaseAccess {
    private DatabaseAccess() {
    }

    public static List<UpgradeDatabase> databases(Node node) {
        final List<UpgradeDatabase> result = new ArrayList<>();
        for (Node n : node.network().nodes()) {
            if (n instanceof Component component && component.host() instanceof UpgradeDatabase db) {
                result.add(db);
            }
        }
        return result;
    }

    public static UpgradeDatabase database(Node node, String address) {
        final Node n = node.network().node(address);
        if (n instanceof Component component) {
            if (component.host() instanceof UpgradeDatabase db) {
                return db;
            }
            throw new IllegalArgumentException("not a database");
        }
        throw new IllegalArgumentException("no such component");
    }

    public static Object[] withDatabase(Node node, String address, Function<UpgradeDatabase, Object[]> f) {
        return f.apply(DatabaseAccess.database(node, address));
    }
}
