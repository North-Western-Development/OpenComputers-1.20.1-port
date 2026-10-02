package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Optional;

/**
 * Port note: Scala {@code trait FileOutputStreamFileSystem extends FileInputStreamFileSystem
 * with OutputStreamFileSystem}; now an abstract class over {@link OutputStreamFileSystem}
 * that reuses {@link FileInputStreamFileSystem}'s static file helpers. The root
 * directory is passed to the constructor.
 */
public abstract class FileOutputStreamFileSystem extends OutputStreamFileSystem {
    private final File root;

    protected FileOutputStreamFileSystem(File root) {
        this.root = root;
    }

    protected File root() {
        return root;
    }

    @Override
    public long spaceTotal() {
        return -1;
    }

    @Override
    public long spaceUsed() {
        return -1;
    }

    // ----------------------------------------------------------------------- //
    // FileInputStreamFileSystem part.

    @Override
    public boolean exists(String path) {
        return FileInputStreamFileSystem.fileExists(root(), path);
    }

    @Override
    public long size(String path) {
        return FileInputStreamFileSystem.fileSize(root(), path);
    }

    @Override
    public boolean isDirectory(String path) {
        return FileInputStreamFileSystem.fileIsDirectory(root(), path);
    }

    @Override
    public long lastModified(String path) {
        return FileInputStreamFileSystem.fileLastModified(root(), path);
    }

    @Override
    public String[] list(String path) {
        return FileInputStreamFileSystem.fileList(root(), path);
    }

    @Override
    protected Optional<InputChannel> openInputChannel(String path) throws IOException {
        return Optional.of(new FileInputStreamFileSystem.FileChannel(new File(root(), path)));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean delete(String path) {
        final File file = new File(root(), FileSystem.INSTANCE.validatePath(path));
        return file.equals(root()) || file.delete();
    }

    @Override
    public boolean makeDirectory(String path) {
        return new File(root(), FileSystem.INSTANCE.validatePath(path)).mkdir();
    }

    @Override
    public boolean rename(String from, String to) {
        return new File(root(), FileSystem.INSTANCE.validatePath(from)).renameTo(new File(root(), FileSystem.INSTANCE.validatePath(to)));
    }

    @Override
    public boolean setLastModified(String path, long time) {
        return new File(root(), FileSystem.INSTANCE.validatePath(path)).setLastModified(time);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected Optional<OutputHandle> openOutputHandle(int id, String path, Mode mode) throws IOException {
        final String fileMode;
        if (mode == Mode.Append || mode == Mode.Write) fileMode = "rw";
        else throw new IllegalArgumentException();
        return Optional.of(new FileHandle(new RandomAccessFile(new File(root(), path), fileMode), this, id, path, mode));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        root().mkdirs();
        root().setLastModified(System.currentTimeMillis());
    }

    // ----------------------------------------------------------------------- //

    protected static class FileHandle extends OutputHandle {
        public final RandomAccessFile file;

        protected FileHandle(RandomAccessFile file, OutputStreamFileSystem owner, int handle, String path, Mode mode) throws IOException {
            super(owner, handle, path);
            this.file = file;
            if (mode == Mode.Write) {
                file.setLength(0);
            }
        }

        @Override
        public long position() {
            try {
                return file.getFilePointer();
            } catch (IOException e) {
                throw FileSystem.sneakyThrow(e);
            }
        }

        @Override
        public long length() {
            try {
                return file.length();
            } catch (IOException e) {
                throw FileSystem.sneakyThrow(e);
            }
        }

        @Override
        public void close() {
            super.close();
            try {
                file.close();
            } catch (IOException e) {
                throw FileSystem.sneakyThrow(e);
            }
        }

        @Override
        public long seek(long to) throws IOException {
            file.seek(to);
            return to;
        }

        @Override
        public void write(byte[] value) throws IOException {
            file.write(value);
        }
    }
}
