package li.cil.oc.server.machine;

import java.util.HashMap;
import java.util.Map;

public final class ProgramLocations {
    private ProgramLocations() {
    }

    public static final Map<String, Map<String, String>> architectureLocations = new HashMap<>();
    public static final Map<String, String> globalLocations = new HashMap<>();

    public static synchronized void addMapping(String program, String label, String... architectures) {
        if (architectures == null || architectures.length == 0) {
            globalLocations.put(program, label);
        } else {
            for (String architecture : architectures) {
                architectureLocations.computeIfAbsent(architecture, k -> new HashMap<>()).put(program, label);
            }
        }
    }

    public static synchronized Map<String, String> getMappings(String architecture) {
        final Map<String, String> result = new HashMap<>(architectureLocations.getOrDefault(architecture, Map.of()));
        result.putAll(globalLocations);
        return result;
    }
}
