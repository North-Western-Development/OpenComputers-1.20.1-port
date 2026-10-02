package li.cil.oc.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class UpdateCheck {
    private UpdateCheck() {
    }

    private static final URL releasesUrl;

    static {
        try {
            releasesUrl = new URL("https://api.github.com/repos/MightyPirates/OpenComputers/releases");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static CompletableFuture<Optional<Release>> info = CompletableFuture.supplyAsync(UpdateCheck::initialize);

    private static String stripPrefix(String s) {
        return s.startsWith("v") ? s.substring(1) : s;
    }

    private static Optional<Release> initialize() {
        // Keep the version template split up so it's not replaced with the actual version...
        if (Settings.get().updateCheck && !OpenComputers.version().equals("@" + "VERSION" + "@")) {
            try {
                OpenComputers.log.info("Starting OpenComputers version check.");
                final JsonReader reader = new JsonReader(new InputStreamReader(releasesUrl.openStream()));
                reader.beginArray();
                final List<Release> candidates = new ArrayList<>();
                while (reader.hasNext()) {
                    final Release release = new Gson().fromJson(reader, Release.class);
                    if (!release.prerelease) {
                        candidates.add(release);
                    }
                }
                reader.endArray();
                if (!candidates.isEmpty()) {
                    final Release latest = candidates.stream().max((a, b) -> VersionUtil.compare(stripPrefix(a.tag_name), stripPrefix(b.tag_name))).get();
                    if (VersionUtil.compare(stripPrefix(latest.tag_name), OpenComputers.version()) > 0) {
                        OpenComputers.log.info("A newer version of OpenComputers is available: " + latest.tag_name + ".");
                        return Optional.of(latest);
                    }
                }
                OpenComputers.log.info("Running the latest OpenComputers version.");
            } catch (Throwable t) {
                OpenComputers.log.warn("Update check for OpenComputers failed.", t);
            }
        }
        return Optional.empty();
    }

    public static class Release {
        public String tag_name = "";
        public String body = "";
        public boolean prerelease = false;
    }
}
