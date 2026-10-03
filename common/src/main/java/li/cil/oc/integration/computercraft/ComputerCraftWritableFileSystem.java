package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.filesystem.WritableMount;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.server.fs.OutputStreamFileSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * A writable OC file system backed by a CC {@link WritableMount} (e.g. floppy disks).
 * <p>
 * Port note: Scala mixed {@code OutputStreamFileSystem} into {@code ComputerCraftFileSystem};
 * in Java the read-only part is delegated to a {@link ComputerCraftFileSystem} instead.
 */
public class ComputerCraftWritableFileSystem extends OutputStreamFileSystem {
    public final WritableMount mount;
    private final ComputerCraftFileSystem reader;

    public ComputerCraftWritableFileSystem(final WritableMount mount) {
        this.mount = mount;
        this.reader = new ComputerCraftFileSystem(mount);
    }

    @Override
    public long spaceTotal() {
        return mount.getCapacity();
    }

    @Override
    public long spaceUsed() {
        try {
            return Math.max(0, mount.getCapacity() - mount.getRemainingSpace());
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public boolean exists(final String path) {
        return reader.exists(path);
    }

    @Override
    public boolean isDirectory(final String path) {
        return reader.isDirectory(path);
    }

    @Override
    public long lastModified(final String path) {
        return reader.lastModified(path);
    }

    @Override
    public String[] list(final String path) {
        return reader.list(path);
    }

    @Override
    public long size(final String path) {
        return reader.size(path);
    }

    @Override
    protected Optional<InputChannel> openInputChannel(final String path) {
        return reader.openInputChannel(path);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean delete(final String path) {
        try {
            mount.delete(path);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean makeDirectory(final String path) {
        try {
            if (mount.exists(path)) return false;
            mount.makeDirectory(path);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean rename(final String from, final String to) {
        try {
            mount.rename(from, to);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    protected Optional<OutputHandle> openOutputHandle(final int id, final String path, final Mode mode) {
        try {
            final SeekableByteChannel channel = switch (mode) {
                case Append -> mount.openFile(path, MountConstants.APPEND_OPTIONS);
                case Write -> mount.openFile(path, MountConstants.WRITE_OPTIONS);
                default -> throw new IllegalArgumentException();
            };
            return Optional.of(new ComputerCraftOutputHandle(channel, this, id, path));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    protected static class ComputerCraftOutputHandle extends OutputHandle {
        private final SeekableByteChannel channel;

        protected ComputerCraftOutputHandle(final SeekableByteChannel channel, final OutputStreamFileSystem owner, final int handle, final String path) {
            super(owner, handle, path);
            this.channel = channel;
        }

        @Override
        public long length() {
            try {
                return channel.size();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public long position() {
            try {
                return channel.position();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public void write(final byte[] value) throws IOException {
            final ByteBuffer buffer = ByteBuffer.wrap(value);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }

        @Override
        public void close() {
            super.close();
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
