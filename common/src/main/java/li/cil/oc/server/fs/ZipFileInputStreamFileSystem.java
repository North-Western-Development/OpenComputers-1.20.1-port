package li.cil.oc.server.fs;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import li.cil.oc.OpenComputers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only file system over a directory inside a ZIP file. Port note: no
 * longer used by {@link FileSystem#fromResource} (which uses
 * {@link PathInputStreamFileSystem}), kept for API parity.
 */
public class ZipFileInputStreamFileSystem extends InputStreamFileSystem {
    private static final Object LOCK = ZipFileInputStreamFileSystem.class;

    private final ArchiveDirectory archive;

    public ZipFileInputStreamFileSystem(ArchiveDirectory archive) {
        this.archive = archive;
    }

    @Override
    public long spaceTotal() {
        return spaceUsed();
    }

    private volatile Long spaceUsed_;

    @Override
    public long spaceUsed() {
        Long result = spaceUsed_;
        if (result == null) {
            synchronized (LOCK) {
                result = recurse(archive);
            }
            spaceUsed_ = result;
        }
        return result;
    }

    private static long recurse(ArchiveDirectory d) {
        long acc = 0L;
        for (Archive c : d.children) {
            if (c instanceof ArchiveDirectory directory) acc += recurse(directory);
            else acc += c.size();
        }
        return acc;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean exists(String path) {
        synchronized (LOCK) {
            return entry(path).isPresent();
        }
    }

    @Override
    public long size(String path) {
        synchronized (LOCK) {
            final Optional<Archive> file = entry(path);
            if (file.isPresent() && !file.get().isDirectory) return file.get().size();
            return 0L;
        }
    }

    @Override
    public boolean isDirectory(String path) {
        synchronized (LOCK) {
            return entry(path).map(e -> e.isDirectory).orElse(false);
        }
    }

    @Override
    public long lastModified(String path) {
        synchronized (LOCK) {
            return entry(path).map(e -> e.lastModified).orElse(0L);
        }
    }

    @Override
    public String[] list(String path) {
        synchronized (LOCK) {
            final Optional<Archive> entry = entry(path);
            if (entry.isPresent() && entry.get().isDirectory) return entry.get().list();
            return null;
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected Optional<InputChannel> openInputChannel(String path) {
        synchronized (LOCK) {
            return entry(path).map(entry -> new InputStreamChannel(entry.openStream()));
        }
    }

    // ----------------------------------------------------------------------- //

    private Optional<Archive> entry(String path) {
        String cleanPath = path.replace("\\", "/").replace("//", "/");
        while (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1);
        while (cleanPath.endsWith("/")) cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        cleanPath = "/" + cleanPath;
        if (cleanPath.equals("/")) return Optional.of(archive);
        else return archive.find(List.of(cleanPath.split("/", -1)));
    }

    // ----------------------------------------------------------------------- //

    private static final Cache<String, ArchiveDirectory> cache = CacheBuilder.newBuilder().weakValues().build();

    public static ZipFileInputStreamFileSystem fromFile(File file, String innerPath) {
        synchronized (LOCK) {
            try {
                final ArchiveDirectory archive = cache.get(file.getPath() + ":" + innerPath, () -> {
                    try (ZipFile zip = new ZipFile(file.getPath())) {
                        String cleanedPath = innerPath;
                        while (cleanedPath.startsWith("/")) cleanedPath = cleanedPath.substring(1);
                        while (cleanedPath.endsWith("/")) cleanedPath = cleanedPath.substring(0, cleanedPath.length() - 1);
                        cleanedPath = cleanedPath + "/";
                        final ZipEntry rootEntry = zip.getEntry(cleanedPath);
                        if (rootEntry == null || !rootEntry.isDirectory()) {
                            throw new IllegalArgumentException("Root path " + innerPath + " doesn't exist or is not a directory in ZIP file " + file.getName() + ".");
                        }
                        final Set<ArchiveDirectory> directories = new LinkedHashSet<>();
                        final Set<ArchiveFile> files = new LinkedHashSet<>();
                        final Enumeration<? extends ZipEntry> iterator = zip.entries();
                        while (iterator.hasMoreElements()) {
                            final ZipEntry entry = iterator.nextElement();
                            if (entry.getName().startsWith(cleanedPath)) {
                                if (entry.isDirectory()) directories.add(new ArchiveDirectory(entry, cleanedPath));
                                else files.add(new ArchiveFile(zip, entry, cleanedPath));
                            }
                        }
                        ArchiveDirectory root = null;
                        final List<Archive> all = new ArrayList<>(directories);
                        all.addAll(files);
                        for (Archive entry : all) {
                            if (entry.path.length() > 0) {
                                final String parent = entry.path.substring(0, Math.max(entry.path.lastIndexOf('/'), 0));
                                for (ArchiveDirectory d : directories) {
                                    if (d.path.equals(parent)) {
                                        d.children.add(entry);
                                        break;
                                    }
                                }
                            } else {
                                root = (ArchiveDirectory) entry;
                            }
                        }
                        return root;
                    }
                });
                return archive != null ? new ZipFileInputStreamFileSystem(archive) : null;
            } catch (Throwable e) {
                OpenComputers.log.warn("Failed creating ZIP file system.", e);
                return null;
            }
        }
    }

    public abstract static class Archive {
        public final String path;

        public final String name;

        public final long lastModified;

        public final boolean isDirectory;

        protected Archive(ZipEntry entry, String root) {
            String p = entry.getName();
            if (p.startsWith(root)) p = p.substring(root.length());
            if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
            this.path = p;
            this.name = path.substring(path.lastIndexOf('/') + 1);
            this.lastModified = entry.getTime();
            this.isDirectory = entry.isDirectory();
        }

        public abstract int size();

        public abstract String[] list();

        public abstract InputStream openStream();

        public abstract Optional<Archive> find(List<String> path);
    }

    private static final class ArchiveFile extends Archive {
        final byte[] data;

        ArchiveFile(ZipFile zip, ZipEntry entry, String root) throws IOException {
            super(entry, root);
            try (InputStream in = zip.getInputStream(entry)) {
                data = in.readAllBytes();
            }
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public String[] list() {
            return null;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public Optional<Archive> find(List<String> path) {
            if (path.size() == 1 && path.get(0).equals(name)) return Optional.of(this);
            else return Optional.empty();
        }
    }

    public static final class ArchiveDirectory extends Archive {
        final Set<Archive> children = new HashSet<>();

        ArchiveDirectory(ZipEntry entry, String root) {
            super(entry, root);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String[] list() {
            final List<String> result = new ArrayList<>();
            for (Archive c : children) result.add(c.name + (c.isDirectory ? "/" : ""));
            return result.toArray(new String[0]);
        }

        @Override
        public InputStream openStream() {
            return null;
        }

        @Override
        public Optional<Archive> find(List<String> path) {
            if (path.get(0).equals(name)) {
                if (path.size() == 1) return Optional.of(this);
                else {
                    final List<String> subPath = path.subList(1, path.size());
                    for (Archive child : children) {
                        final Optional<Archive> found = child.find(subPath);
                        if (found.isPresent()) return found;
                    }
                    return Optional.empty();
                }
            } else return Optional.empty();
        }
    }
}
