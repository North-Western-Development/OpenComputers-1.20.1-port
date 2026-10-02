package li.cil.oc.server.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Port note: Scala mixin trait; now an abstract class over {@link InputStreamFileSystem}.
 * The file based read operations are also available as static helpers
 * ({@code fileExists} etc.) so {@link FileOutputStreamFileSystem}, which has
 * {@link OutputStreamFileSystem} as its base class, can share them.
 */
public abstract class FileInputStreamFileSystem extends InputStreamFileSystem {
    protected abstract File root();

    // ----------------------------------------------------------------------- //

    @Override
    public long spaceTotal() {
        return spaceUsed();
    }

    private volatile Long spaceUsed_;

    @Override
    public long spaceUsed() {
        Long result = spaceUsed_;
        if (result == null) {
            result = recurseSize(root());
            spaceUsed_ = result;
        }
        return result;
    }

    static long recurseSize(File path) {
        if (path.isDirectory()) {
            long acc = 0L;
            final File[] files = path.listFiles();
            if (files != null) for (File f : files) acc += recurseSize(f);
            return acc;
        } else return path.length();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean exists(String path) {
        return fileExists(root(), path);
    }

    @Override
    public long size(String path) {
        return fileSize(root(), path);
    }

    @Override
    public boolean isDirectory(String path) {
        return fileIsDirectory(root(), path);
    }

    @Override
    public long lastModified(String path) {
        return fileLastModified(root(), path);
    }

    @Override
    public String[] list(String path) {
        return fileList(root(), path);
    }

    // ----------------------------------------------------------------------- //

    public static boolean fileExists(File root, String path) {
        return new File(root, FileSystem.INSTANCE.validatePath(path)).exists();
    }

    public static long fileSize(File root, String path) {
        final File file = new File(root, FileSystem.INSTANCE.validatePath(path));
        return file.isFile() ? file.length() : 0L;
    }

    public static boolean fileIsDirectory(File root, String path) {
        return new File(root, FileSystem.INSTANCE.validatePath(path)).isDirectory();
    }

    public static long fileLastModified(File root, String path) {
        return new File(root, FileSystem.INSTANCE.validatePath(path)).lastModified();
    }

    public static String[] fileList(File root, String path) {
        final File file = new File(root, FileSystem.INSTANCE.validatePath(path));
        if (file.exists() && file.isFile()) return new String[]{file.getName()};
        else if (file.exists() && file.isDirectory() && file.list() != null) {
            final File[] files = file.listFiles();
            final String[] result = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                result[i] = files[i].isDirectory() ? files[i].getName() + "/" : files[i].getName();
            }
            return result;
        } else throw FileSystem.sneakyThrow(new FileNotFoundException("no such file or directory: " + path));
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected Optional<InputChannel> openInputChannel(String path) throws IOException {
        return Optional.of(new FileChannel(new File(root(), path)));
    }

    public static class FileChannel implements InputChannel {
        public final java.nio.channels.FileChannel channel;

        public FileChannel(File file) throws FileNotFoundException {
            this.channel = new RandomAccessFile(file, "r").getChannel();
        }

        @Override
        public long position(long newPosition) throws IOException {
            channel.position(newPosition);
            return channel.position();
        }

        @Override
        public long position() throws IOException {
            return channel.position();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public int read(byte[] dst) throws IOException {
            return channel.read(ByteBuffer.wrap(dst));
        }
    }
}
