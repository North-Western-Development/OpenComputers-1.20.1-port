package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import li.cil.oc.server.fs.InputStreamFileSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A read-only OC file system backed by a CC {@link Mount} (e.g. treasure disks, ROM mounts).
 */
public class ComputerCraftFileSystem extends InputStreamFileSystem {
    public final Mount mount;

    public ComputerCraftFileSystem(final Mount mount) {
        this.mount = mount;
    }

    @Override
    public long spaceTotal() {
        return 0;
    }

    @Override
    public long spaceUsed() {
        return 0;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean exists(final String path) {
        try {
            return mount.exists(path);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isDirectory(final String path) {
        try {
            return mount.isDirectory(path);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long lastModified(final String path) {
        try {
            return mount.getAttributes(path).lastModifiedTime().toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public String[] list(final String path) {
        try {
            if (!mount.exists(path))
                throw CallableHelper.<RuntimeException>sneakyThrow(new java.io.FileNotFoundException("no such file or directory: " + path));
            if (!mount.isDirectory(path)) {
                final int slash = path.lastIndexOf('/');
                return new String[]{slash >= 0 ? path.substring(slash + 1) : path};
            }
            final List<String> result = new ArrayList<>();
            mount.list(path, result);
            final String prefix = path.isEmpty() || path.endsWith("/") ? path : path + "/";
            final String[] names = new String[result.size()];
            for (int i = 0; i < names.length; i++) {
                final String name = result.get(i);
                // OC marks directories with a trailing slash.
                names[i] = mount.isDirectory(prefix + name) ? name + "/" : name;
            }
            return names;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public long size(final String path) {
        try {
            return mount.isDirectory(path) ? 0 : mount.getSize(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected Optional<InputChannel> openInputChannel(final String path) {
        try {
            return Optional.of(new SeekableChannel(mount.openForRead(path)));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /** Wraps CC's seekable channels (CC 1.20 mounts return those instead of streams). */
    protected static final class SeekableChannel implements InputChannel {
        private final SeekableByteChannel channel;

        SeekableChannel(final SeekableByteChannel channel) {
            this.channel = channel;
        }

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        @Override
        public long position() throws IOException {
            return channel.position();
        }

        @Override
        public long position(final long newPosition) throws IOException {
            channel.position(Math.max(0, Math.min(newPosition, channel.size())));
            return channel.position();
        }

        @Override
        public int read(final byte[] dst) throws IOException {
            return channel.read(ByteBuffer.wrap(dst));
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException {
            return channel.read(dst);
        }
    }
}
