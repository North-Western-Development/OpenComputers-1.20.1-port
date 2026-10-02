package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class OutputStreamFileSystem extends InputStreamFileSystem {
    private final Map<Integer, OutputHandle> handles = new HashMap<>();

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isReadOnly() {
        return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int open(String path, Mode mode) throws FileNotFoundException {
        synchronized (this) {
            if (mode == Mode.Read) return super.open(path, mode);
            FileSystem.INSTANCE.validatePath(path);
            if (!isDirectory(path)) {
                int handle;
                do {
                    handle = (int) (Math.random() * Integer.MAX_VALUE) + 1;
                } while (handles.containsKey(handle));
                final Optional<OutputHandle> fileHandle;
                try {
                    fileHandle = openOutputHandle(handle, path, mode);
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (IOException e) {
                    throw FileSystem.sneakyThrow(e);
                }
                if (fileHandle.isPresent()) {
                    handles.put(handle, fileHandle.get());
                    return handle;
                } else throw new FileNotFoundException(path);
            } else throw new FileNotFoundException(path);
        }
    }

    @Override
    public li.cil.oc.api.fs.Handle getHandle(int handle) {
        synchronized (this) {
            final li.cil.oc.api.fs.Handle result = super.getHandle(handle);
            return result != null ? result : handles.get(handle);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            super.close();
            for (OutputHandle handle : new ArrayList<>(handles.values()))
                handle.close();
            handles.clear();
        }
    }

    // ----------------------------------------------------------------------- //

    private static final String OutputTag = "output";
    private static final String HandleTag = "handle";
    private static final String PathTag = "path";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);

        final ListTag handlesNbt = nbt.getList(OutputTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < handlesNbt.size(); i++) {
            final CompoundTag handleNbt = handlesNbt.getCompound(i);
            final int handle = handleNbt.getInt(HandleTag);
            final String path = handleNbt.getString(PathTag);
            try {
                final Optional<OutputHandle> fileHandle = openOutputHandle(handle, path, Mode.Append);
                fileHandle.ifPresent(h -> handles.put(handle, h));
                // Else: the source file seems to have changed since last time.
            } catch (IOException e) {
                throw FileSystem.sneakyThrow(e);
            }
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        synchronized (this) {
            super.saveData(nbt);

            final ListTag handlesNbt = new ListTag();
            for (OutputHandle file : handles.values()) {
                assert !file.isClosed();
                final CompoundTag handleNbt = new CompoundTag();
                handleNbt.putInt(HandleTag, file.handle);
                handleNbt.putString(PathTag, file.path);
                handlesNbt.add(handleNbt);
            }
            nbt.put(OutputTag, handlesNbt);
        }
    }

    // ----------------------------------------------------------------------- //

    protected abstract Optional<OutputHandle> openOutputHandle(int id, String path, Mode mode) throws IOException;

    /**
     * Functional shape of {@link #openOutputHandle(int, String, Mode)}, used to pass
     * {@code super::openOutputHandle} to stackable behaviours such as {@link Capacity}.
     */
    @FunctionalInterface
    public interface OutputHandleOpener {
        Optional<OutputHandle> open(int id, String path, Mode mode) throws IOException;
    }

    // ----------------------------------------------------------------------- //

    public abstract static class OutputHandle implements li.cil.oc.api.fs.Handle {
        public final OutputStreamFileSystem owner;
        public final int handle;
        public final String path;

        protected boolean _isClosed = false;

        protected OutputHandle(OutputStreamFileSystem owner, int handle, String path) {
            this.owner = owner;
            this.handle = handle;
            this.path = path;
        }

        public boolean isClosed() {
            return _isClosed;
        }

        @Override
        public void close() {
            if (!isClosed()) {
                _isClosed = true;
                owner.handles.remove(handle);
            }
        }

        @Override
        public int read(byte[] into) throws IOException {
            throw new IOException("bad file descriptor");
        }

        @Override
        public long seek(long to) throws IOException {
            throw new IOException("bad file descriptor");
        }
    }
}
