package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Port note: Scala trait used as primary base class of all OC stream based file
 * systems; now an abstract class.
 */
public abstract class InputStreamFileSystem implements li.cil.oc.api.fs.FileSystem {
    private final Map<Integer, Handle> handles = new HashMap<>();

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public boolean makeDirectory(String path) {
        return false;
    }

    @Override
    public boolean rename(String from, String to) throws FileNotFoundException {
        return false;
    }

    @Override
    public boolean setLastModified(String path, long time) {
        return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int open(String path, Mode mode) throws FileNotFoundException {
        FileSystem.INSTANCE.validatePath(path);
        synchronized (this) {
            if (mode == Mode.Read && exists(path) && !isDirectory(path)) {
                int handle;
                do {
                    handle = (int) (Math.random() * Integer.MAX_VALUE) + 1;
                } while (handles.containsKey(handle));
                final Optional<InputChannel> channel;
                try {
                    channel = openInputChannel(path);
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (IOException e) {
                    throw FileSystem.sneakyThrow(e);
                }
                if (channel.isPresent()) {
                    handles.put(handle, new Handle(this, handle, path, channel.get()));
                    return handle;
                } else throw new FileNotFoundException(path);
            } else throw new FileNotFoundException(path);
        }
    }

    @Override
    public li.cil.oc.api.fs.Handle getHandle(int handle) {
        synchronized (this) {
            return handles.get(handle);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            for (Handle handle : new ArrayList<>(handles.values()))
                handle.close();
            handles.clear();
        }
    }

    // ----------------------------------------------------------------------- //

    private static final String InputTag = "input";
    private static final String HandleTag = "handle";
    private static final String PathTag = "path";
    private static final String PositionTag = "position";

    @Override
    public void loadData(CompoundTag nbt) {
        final ListTag handlesNbt = nbt.getList(InputTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < handlesNbt.size(); i++) {
            final CompoundTag handleNbt = handlesNbt.getCompound(i);
            final int handle = handleNbt.getInt(HandleTag);
            final String path = handleNbt.getString(PathTag);
            final long position = handleNbt.getLong(PositionTag);
            try {
                final Optional<InputChannel> channel = openInputChannel(path);
                if (channel.isPresent()) {
                    final Handle fileHandle = new Handle(this, handle, path, channel.get());
                    channel.get().position(position);
                    handles.put(handle, fileHandle);
                }
                // Else: the source file seems to have disappeared since last time.
            } catch (IOException e) {
                throw FileSystem.sneakyThrow(e);
            }
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        synchronized (this) {
            final ListTag handlesNbt = new ListTag();
            for (Handle file : handles.values()) {
                assert file.channel.isOpen();
                final CompoundTag handleNbt = new CompoundTag();
                handleNbt.putInt(HandleTag, file.handle);
                handleNbt.putString(PathTag, file.path);
                handleNbt.putLong(PositionTag, file.position());
                handlesNbt.add(handleNbt);
            }
            nbt.put(InputTag, handlesNbt);
        }
    }

    // ----------------------------------------------------------------------- //

    protected abstract Optional<InputChannel> openInputChannel(String path) throws IOException;

    public interface InputChannel extends ReadableByteChannel {
        @Override
        boolean isOpen();

        @Override
        void close() throws IOException;

        long position() throws IOException;

        long position(long newPosition) throws IOException;

        int read(byte[] dst) throws IOException;

        @Override
        default int read(ByteBuffer dst) throws IOException {
            if (dst.hasArray()) {
                return read(dst.array());
            } else {
                final int count = Math.max(0, dst.limit() - dst.position());
                final byte[] buffer = new byte[count];
                final int n = read(buffer);
                if (n > 0) dst.put(buffer, 0, n);
                return n;
            }
        }
    }

    public static class InputStreamChannel implements InputChannel {
        public final java.io.InputStream inputStream;

        public boolean isOpen = true;

        private long position_ = 0L;

        public InputStreamChannel(java.io.InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public boolean isOpen() {
            return isOpen;
        }

        @Override
        public void close() throws IOException {
            if (isOpen) {
                isOpen = false;
                inputStream.close();
            }
        }

        @Override
        public long position() {
            return position_;
        }

        @Override
        public long position(long newPosition) throws IOException {
            inputStream.reset();
            position_ = inputStream.skip(newPosition);
            return position_;
        }

        @Override
        public int read(byte[] dst) throws IOException {
            final int read = inputStream.read(dst);
            position_ += read;
            return read;
        }
    }

    // ----------------------------------------------------------------------- //

    private static final class Handle implements li.cil.oc.api.fs.Handle {
        final InputStreamFileSystem owner;
        final int handle;
        final String path;
        final InputChannel channel;

        Handle(InputStreamFileSystem owner, int handle, String path, InputChannel channel) {
            this.owner = owner;
            this.handle = handle;
            this.path = path;
            this.channel = channel;
        }

        @Override
        public long position() {
            try {
                return channel.position();
            } catch (IOException e) {
                throw FileSystem.sneakyThrow(e);
            }
        }

        @Override
        public long length() {
            return owner.size(path);
        }

        @Override
        public void close() {
            if (channel.isOpen()) {
                owner.handles.remove(handle);
                try {
                    channel.close();
                } catch (IOException e) {
                    throw FileSystem.sneakyThrow(e);
                }
            }
        }

        @Override
        public int read(byte[] into) throws IOException {
            return channel.read(into);
        }

        @Override
        public long seek(long to) throws IOException {
            return channel.position(to);
        }

        @Override
        public void write(byte[] value) throws IOException {
            throw new IOException("bad file descriptor");
        }
    }
}
