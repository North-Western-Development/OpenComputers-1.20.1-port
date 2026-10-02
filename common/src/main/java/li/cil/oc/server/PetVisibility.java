package li.cil.oc.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PetVisibility {
    public static final Set<String> hidden = Collections.synchronizedSet(new HashSet<>());

    private PetVisibility() {
    }
}
