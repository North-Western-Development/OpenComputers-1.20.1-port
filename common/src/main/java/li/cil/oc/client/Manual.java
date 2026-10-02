package li.cil.oc.client;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Client side implementation of the manual API.
 * <p>
 * Members that are not part of {@link ManualAPI} are static, so they can be
 * reached both as {@code Manual.history} and {@code Manual.INSTANCE.history}.
 * The navigation history is a stack: {@code Manual.history.peek()} is the
 * current page (Scala's {@code history.top}).
 */
public final class Manual implements ManualAPI {
    public static final Manual INSTANCE = new Manual();

    public static final String LanguageKey = "%LANGUAGE%";

    public static final String FallbackLanguage = "en_us";

    public static final class History {
        public final String path;
        public int offset;

        public History(String path) {
            this(path, 0);
        }

        public History(String path, int offset) {
            this.path = path;
            this.offset = offset;
        }
    }

    public static final class Tab {
        public final TabIconRenderer renderer;
        public final Optional<String> tooltip;
        public final String path;

        public Tab(TabIconRenderer renderer, Optional<String> tooltip, String path) {
            this.renderer = renderer;
            this.tooltip = tooltip;
            this.path = path;
        }
    }

    public static final List<Tab> tabs = new ArrayList<>();

    public static final List<PathProvider> pathProviders = new ArrayList<>();

    public static final List<ContentProvider> contentProviders = new ArrayList<>();

    public static final List<Pair<String, ImageProvider>> imageProviders = new ArrayList<>();

    public static final Deque<History> history = new ArrayDeque<>();

    static {
        history.push(new History(LanguageKey + "/index.md"));
    }

    private Manual() {
    }

    @Override
    public void addTab(TabIconRenderer renderer, String tooltip, String path) {
        tabs.add(new Tab(renderer, Optional.ofNullable(tooltip), path));
        if (tabs.size() > 7) {
            OpenComputers.log.warn("Gosh I'm popular! Too many tabs were added to the OpenComputers in-game manual, so some won't be shown. In case this actually happens, let me know and I'll look into making them scrollable or something...");
        }
    }

    @Override
    public void addProvider(PathProvider provider) {
        pathProviders.add(provider);
    }

    @Override
    public void addProvider(ContentProvider provider) {
        contentProviders.add(provider);
    }

    @Override
    public void addProvider(String prefix, ImageProvider provider) {
        imageProviders.add(Pair.of(Strings.isNullOrEmpty(prefix) ? "" : prefix + ":", provider));
    }

    @Override
    public String pathFor(ItemStack stack) {
        for (PathProvider provider : pathProviders) {
            String path;
            try {
                path = provider.pathFor(stack);
            } catch (Throwable t) {
                OpenComputers.log.warn("A path provider threw an error when queried with an item.", t);
                path = null;
            }
            if (path != null) return path;
        }
        return null;
    }

    @Override
    public String pathFor(Level world, BlockPos pos) {
        for (PathProvider provider : pathProviders) {
            String path;
            try {
                path = provider.pathFor(world, pos);
            } catch (Throwable t) {
                OpenComputers.log.warn("A path provider threw an error when queried with a block.", t);
                path = null;
            }
            if (path != null) return path;
        }
        return null;
    }

    @Override
    public Iterable<String> contentFor(String path) {
        final String cleanPath = com.google.common.io.Files.simplifyPath(path);
        final String language = Minecraft.getInstance().getLanguageManager().getSelected();
        return contentForWithRedirects(cleanPath.replaceAll(LanguageKey, language))
                .or(() -> contentForWithRedirects(cleanPath.replaceAll(LanguageKey, FallbackLanguage)))
                .orElse(null);
    }

    @Override
    public ImageRenderer imageFor(String href) {
        for (int i = imageProviders.size() - 1; i >= 0; i--) {
            final String prefix = imageProviders.get(i).getLeft();
            final ImageProvider provider = imageProviders.get(i).getRight();
            if (href.startsWith(prefix)) {
                ImageRenderer image;
                try {
                    image = provider.getImage(href.substring(prefix.length()));
                } catch (Throwable t) {
                    OpenComputers.log.warn("An image provider threw an error when queried.", t);
                    image = null;
                }
                if (image != null) return image;
            }
        }
        return null;
    }

    @Override
    public void openFor(Player player) {
        if (player.level().isClientSide()) {
            final Minecraft mc = Minecraft.getInstance();
            // TODO(port): Forge's pushGuiLayer is gone; this replaces the current screen instead of layering.
            if (player == mc.player) mc.setScreen(new li.cil.oc.client.gui.Manual());
        }
    }

    @Override
    public void reset() {
        history.clear();
        history.push(new History(LanguageKey + "/index.md"));
    }

    @Override
    public void navigate(String path) {
        if (Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Manual manual) {
            manual.pushPage(path);
        } else {
            history.push(new History(path));
        }
    }

    public static String makeRelative(String path, String base) {
        if (path.startsWith("/")) return path;
        final int splitAt = base.lastIndexOf('/');
        if (splitAt >= 0) return base.substring(0, splitAt) + "/" + path;
        return path;
    }

    private static Optional<Iterable<String>> contentForWithRedirects(String path) {
        final List<String> seen = new ArrayList<>();
        String current = path;
        while (true) {
            if (seen.contains(current)) {
                final List<String> result = new ArrayList<>();
                result.add("Redirection loop: ");
                result.addAll(seen);
                result.add(current);
                return Optional.of(result);
            }
            final Optional<Iterable<String>> content = doContentLookup(current);
            if (content.isEmpty()) return Optional.empty();
            final java.util.Iterator<String> it = content.get().iterator();
            if (it.hasNext()) {
                final String line = it.next();
                if (line.toLowerCase().startsWith("#redirect ")) {
                    seen.add(current);
                    current = makeRelative(line.substring("#redirect ".length()), current);
                    continue;
                }
            }
            return content;
        }
    }

    private static Optional<Iterable<String>> doContentLookup(String path) {
        for (ContentProvider provider : contentProviders) {
            Iterable<String> lines;
            try {
                lines = provider.getContent(path);
            } catch (Throwable t) {
                OpenComputers.log.warn("A content provider threw an error when queried.", t);
                lines = null;
            }
            if (lines != null) return Optional.of(lines);
        }
        return Optional.empty();
    }
}
