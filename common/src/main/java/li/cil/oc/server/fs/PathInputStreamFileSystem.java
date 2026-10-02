package li.cil.oc.server.fs;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * New in the port: read-only file system over a {@link java.nio.file.Path} root.
 * Used for resources in mod files ({@link FileSystem#fromResource}), where the
 * root may live inside a jar's (zip / union) file system or a dev directory.
 * Files are read fully into memory when opened (like the former ZIP backed file
 * system did), which gives cheap seeking.
 */
public class PathInputStreamFileSystem extends InputStreamFileSystem {
    protected final Path root;

    public PathInputStreamFileSystem(Path root) {
        this.root = root;
    }

    private Path resolve(String path) {
        String clean = FileSystem.INSTANCE.validatePath(path).replace('\\', '/');
        while (clean.startsWith("/")) clean = clean.substring(1);
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        if (clean.isEmpty()) return root;
        Path result = root;
        for (String segment : clean.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) continue;
            result = result.resolve(segment);
        }
        return result;
    }

    // ----------------------------------------------------------------------- //

    private volatile Long spaceUsed_;

    @Override
    public long spaceTotal() {
        return spaceUsed();
    }

    @Override
    public long spaceUsed() {
        Long result = spaceUsed_;
        if (result == null) {
            long acc = 0L;
            try (Stream<Path> stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (Files.isRegularFile(p)) acc += Files.size(p);
                }
            } catch (IOException ignored) {
            }
            result = acc;
            spaceUsed_ = result;
        }
        return result;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean exists(String path) {
        return Files.exists(resolve(path));
    }

    @Override
    public long size(String path) {
        final Path file = resolve(path);
        try {
            return Files.isRegularFile(file) ? Files.size(file) : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public boolean isDirectory(String path) {
        return Files.isDirectory(resolve(path));
    }

    @Override
    public long lastModified(String path) {
        try {
            return Files.getLastModifiedTime(resolve(path)).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public String[] list(String path) {
        final Path directory = resolve(path);
        if (!Files.isDirectory(directory)) return null;
        final List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            stream.forEach(p -> {
                String name = p.getFileName().toString();
                if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
                result.add(Files.isDirectory(p) ? name + "/" : name);
            });
        } catch (IOException e) {
            return null;
        }
        return result.toArray(new String[0]);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected Optional<InputChannel> openInputChannel(String path) throws IOException {
        final Path file = resolve(path);
        if (!Files.isRegularFile(file)) {
            if (Files.exists(file)) return Optional.empty();
            throw new FileNotFoundException(path);
        }
        return Optional.of(new InputStreamChannel(new ByteArrayInputStream(Files.readAllBytes(file))));
    }
}
