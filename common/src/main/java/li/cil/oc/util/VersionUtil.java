package li.cil.oc.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal version comparison for OC's own version strings (e.g. "1.8.0-snapshot+abc").
 * Replaces Maven's ComparableVersion / DefaultArtifactVersion, which only Forge ships.
 * <p>
 * Numeric components are compared numerically; a version with a qualifier
 * ("-snapshot", "-beta") sorts before the same version without one; build
 * metadata after '+' is ignored.
 */
public final class VersionUtil {
    private VersionUtil() {
    }

    public static int compare(String a, String b) {
        final Parsed pa = parse(a), pb = parse(b);
        final int n = Math.max(pa.numbers.size(), pb.numbers.size());
        for (int i = 0; i < n; i++) {
            final long x = i < pa.numbers.size() ? pa.numbers.get(i) : 0;
            final long y = i < pb.numbers.size() ? pb.numbers.get(i) : 0;
            if (x != y) return Long.compare(x, y);
        }
        if (pa.qualifier.isEmpty() != pb.qualifier.isEmpty()) return pa.qualifier.isEmpty() ? 1 : -1;
        return pa.qualifier.compareToIgnoreCase(pb.qualifier);
    }

    private record Parsed(List<Long> numbers, String qualifier) {
    }

    private static Parsed parse(String version) {
        String v = version == null ? "" : version.trim();
        final int plus = v.indexOf('+');
        if (plus >= 0) v = v.substring(0, plus);
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        String qualifier = "";
        final int dash = v.indexOf('-');
        if (dash >= 0) {
            qualifier = v.substring(dash + 1);
            v = v.substring(0, dash);
        }
        final List<Long> numbers = new ArrayList<>();
        for (String part : v.split("\\.")) {
            try {
                numbers.add(Long.parseLong(part.replaceAll("\\D.*$", "")));
            } catch (NumberFormatException e) {
                numbers.add(0L);
            }
        }
        return new Parsed(numbers, qualifier);
    }
}
