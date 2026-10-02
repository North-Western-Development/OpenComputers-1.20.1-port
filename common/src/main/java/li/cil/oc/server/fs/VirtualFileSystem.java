package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class VirtualFileSystem extends OutputStreamFileSystem {
    protected final VirtualDirectory root = new VirtualDirectory();

    // ----------------------------------------------------------------------- //

    @Override
    public boolean exists(String path) {
        return root.get(segmentList(path)).isPresent();
    }

    @Override
    public boolean isDirectory(String path) {
        return root.get(segmentList(path)).map(VirtualObject::isDirectory).orElse(false);
    }

    @Override
    public long size(String path) {
        return root.get(segmentList(path)).map(VirtualObject::size).orElse(0L);
    }

    @Override
    public long lastModified(String path) {
        return root.get(segmentList(path)).map(obj -> obj.lastModified).orElse(0L);
    }

    @Override
    public String[] list(String path) {
        final Optional<VirtualObject> obj = root.get(segmentList(path));
        if (obj.isPresent() && obj.get() instanceof VirtualDirectory directory) return directory.list();
        return null;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean delete(String path) {
        final String[] parts = segments(path);
        if (parts.length == 0) return true;
        else {
            final Optional<VirtualObject> parent = root.get(Arrays.asList(parts).subList(0, parts.length - 1));
            if (parent.isPresent() && parent.get() instanceof VirtualDirectory directory)
                return directory.delete(parts[parts.length - 1]);
            return false;
        }
    }

    @Override
    public boolean makeDirectory(String path) {
        final String[] parts = segments(path);
        if (parts.length == 0) return false;
        else {
            final Optional<VirtualObject> parent = root.get(Arrays.asList(parts).subList(0, parts.length - 1));
            if (parent.isPresent() && parent.get() instanceof VirtualDirectory directory)
                return directory.makeDirectory(parts[parts.length - 1]);
            return false;
        }
    }

    @Override
    public boolean rename(String from, String to) throws FileNotFoundException {
        if (from.equals("") || !exists(from)) throw new FileNotFoundException(from);
        else if (!exists(to)) {
            final String[] segmentsTo = segments(to);
            final Optional<VirtualObject> toParentObj = root.get(Arrays.asList(segmentsTo).subList(0, Math.max(segmentsTo.length - 1, 0)));
            if (toParentObj.isPresent() && toParentObj.get() instanceof VirtualDirectory toParent) {
                final String toName = segmentsTo[segmentsTo.length - 1];
                final String[] segmentsFrom = segments(from);
                final VirtualDirectory fromParent = (VirtualDirectory) root.get(Arrays.asList(segmentsFrom).subList(0, segmentsFrom.length - 1)).get();
                final String fromName = segmentsFrom[segmentsFrom.length - 1];
                final VirtualObject obj = fromParent.children.get(fromName);
                if (obj == null) throw new java.util.NoSuchElementException("key not found: " + fromName);

                fromParent.children.remove(fromName);
                fromParent.lastModified = System.currentTimeMillis();

                toParent.children.put(toName, obj);
                toParent.lastModified = System.currentTimeMillis();

                obj.lastModified = System.currentTimeMillis();
                return true;
            }
            return false;
        } else return false;
    }

    @Override
    public boolean setLastModified(String path, long time) {
        final Optional<VirtualObject> obj = root.get(segmentList(path));
        if (obj.isPresent() && time >= 0) {
            obj.get().lastModified = time;
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected Optional<InputChannel> openInputChannel(String path) throws IOException {
        final Optional<VirtualObject> obj = root.get(segmentList(path));
        if (obj.isPresent() && obj.get() instanceof VirtualFile file) {
            return file.openInputStream().map(InputStreamChannel::new);
        }
        return Optional.empty();
    }

    @Override
    protected Optional<OutputHandle> openOutputHandle(int id, String path, Mode mode) throws IOException {
        final String[] parts = segments(path);
        if (parts.length == 0) return Optional.empty();
        else {
            final Optional<VirtualObject> parent = root.get(Arrays.asList(parts).subList(0, parts.length - 1));
            if (parent.isPresent() && parent.get() instanceof VirtualDirectory directory) {
                final Optional<VirtualFile> file = directory.touch(parts[parts.length - 1]);
                if (file.isPresent()) return file.get().openOutputHandle(this, id, path, mode).map(h -> h);
            }
            return Optional.empty();
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadData(CompoundTag nbt) {
        if (!(this instanceof Buffered)) root.loadData(nbt);
        super.loadData(nbt); // Last to ensure streams can be re-opened.
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt); // First to allow flushing.
        if (!(this instanceof Buffered)) root.saveData(nbt);
    }

    // ----------------------------------------------------------------------- //

    protected String[] segments(String path) {
        return Arrays.stream(FileSystem.INSTANCE.validatePath(path).split("/")).filter(s -> !s.equals("")).toArray(String[]::new);
    }

    private List<String> segmentList(String path) {
        return Arrays.asList(segments(path));
    }

    // ----------------------------------------------------------------------- //

    protected abstract static class VirtualObject {
        public long lastModified = System.currentTimeMillis();

        public abstract boolean isDirectory();

        public abstract long size();

        public void loadData(CompoundTag nbt) {
            if (nbt.contains("lastModified"))
                lastModified = nbt.getLong("lastModified");
        }

        public void saveData(CompoundTag nbt) {
            nbt.putLong("lastModified", lastModified);
        }

        public Optional<VirtualObject> get(List<String> path) {
            if (path.isEmpty()) return Optional.of(this);
            else return Optional.empty();
        }

        public abstract boolean canDelete();
    }

    // ----------------------------------------------------------------------- //

    /**
     * Growable byte array (Scala {@code mutable.ArrayBuffer[Byte]}).
     */
    public static final class ByteData {
        private byte[] bytes = new byte[0];
        private int length = 0;

        public int length() {
            return length;
        }

        public byte get(int index) {
            if (index < 0 || index >= length) throw new IndexOutOfBoundsException(index);
            return bytes[index];
        }

        public void set(int index, byte value) {
            if (index < 0 || index >= length) throw new IndexOutOfBoundsException(index);
            bytes[index] = value;
        }

        public void clear() {
            bytes = new byte[0];
            length = 0;
        }

        /**
         * Grows the buffer to the specified length, padding with zeros.
         */
        public void growTo(int newLength) {
            if (newLength <= length) return;
            if (newLength > bytes.length) {
                bytes = Arrays.copyOf(bytes, Math.max(newLength, bytes.length * 2));
            }
            length = newLength;
        }

        public void append(byte[] value) {
            final int offset = length;
            growTo(length + value.length);
            System.arraycopy(value, 0, bytes, offset, value.length);
        }

        public void copyTo(int from, byte[] target, int offset, int count) {
            System.arraycopy(bytes, from, target, offset, count);
        }

        public void copyFrom(byte[] source, int to) {
            System.arraycopy(source, 0, bytes, to, source.length);
        }

        public byte[] toArray() {
            return Arrays.copyOf(bytes, length);
        }
    }

    protected static class VirtualFile extends VirtualObject {
        public final ByteData data = new ByteData();

        public Optional<VirtualOutputHandle> handle = Optional.empty();

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public long size() {
            return data.length();
        }

        public Optional<InputStream> openInputStream() {
            return Optional.of(new VirtualFileInputStream(this));
        }

        public Optional<VirtualOutputHandle> openOutputHandle(OutputStreamFileSystem owner, int id, String path, Mode mode) {
            if (handle.isPresent()) return Optional.empty();
            else {
                if (mode == Mode.Write) {
                    data.clear();
                    lastModified = System.currentTimeMillis();
                }
                handle = Optional.of(new VirtualOutputHandle(this, owner, id, path));
                return handle;
            }
        }

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            data.clear();
            data.append(nbt.getByteArray("data"));
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            nbt.putByteArray("data", data.toArray());
        }

        @Override
        public boolean canDelete() {
            return handle.isEmpty();
        }
    }

    // ----------------------------------------------------------------------- //

    protected static class VirtualDirectory extends VirtualObject {
        public final Map<String, VirtualObject> children = new LinkedHashMap<>();

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public long size() {
            return 0;
        }

        public String[] list() {
            final List<String> result = new ArrayList<>(children.size());
            for (Map.Entry<String, VirtualObject> entry : children.entrySet()) {
                result.add(entry.getValue().isDirectory() ? entry.getKey() + "/" : entry.getKey());
            }
            return result.toArray(new String[0]);
        }

        public boolean makeDirectory(String name) {
            if (children.containsKey(name)) return false;
            else {
                children.put(name, new VirtualDirectory());
                lastModified = System.currentTimeMillis();
                return true;
            }
        }

        public boolean delete(String name) {
            final VirtualObject child = children.get(name);
            if (child != null && child.canDelete()) {
                children.remove(name);
                lastModified = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        public Optional<VirtualFile> touch(String name) {
            final VirtualObject obj = children.get(name);
            if (obj instanceof VirtualFile file) return Optional.of(file);
            else if (obj == null) {
                final VirtualFile child = new VirtualFile();
                children.put(name, child);
                lastModified = System.currentTimeMillis();
                return Optional.of(child);
            } else return Optional.empty(); // Directory.
        }

        private static final String ChildrenTag = "children";
        private static final String IsDirectoryTag = "isDirectory";
        private static final String NameTag = "name";

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            final ListTag childrenNbt = nbt.getList(ChildrenTag, Tag.TAG_COMPOUND);
            for (int i = 0; i < childrenNbt.size(); i++) {
                final CompoundTag childNbt = childrenNbt.getCompound(i);
                final VirtualObject child =
                        childNbt.getBoolean(IsDirectoryTag) ? new VirtualDirectory() : new VirtualFile();
                child.loadData(childNbt);
                children.put(childNbt.getString(NameTag), child);
            }
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            final ListTag childrenNbt = new ListTag();
            for (Map.Entry<String, VirtualObject> entry : children.entrySet()) {
                final CompoundTag childNbt = new CompoundTag();
                childNbt.putBoolean(IsDirectoryTag, entry.getValue().isDirectory());
                childNbt.putString(NameTag, entry.getKey());
                entry.getValue().saveData(childNbt);
                childrenNbt.add(childNbt);
            }
            nbt.put(ChildrenTag, childrenNbt);
        }

        @Override
        public Optional<VirtualObject> get(List<String> path) {
            final Optional<VirtualObject> self = super.get(path);
            if (self.isPresent()) return self;
            final VirtualObject child = children.get(path.get(0));
            if (child != null) return child.get(path.subList(1, path.size()));
            return Optional.empty();
        }

        @Override
        public boolean canDelete() {
            return children.isEmpty();
        }
    }

    // ----------------------------------------------------------------------- //

    protected static class VirtualFileInputStream extends InputStream {
        public final VirtualFile file;

        private boolean isClosed = false;

        private int position = 0;

        public VirtualFileInputStream(VirtualFile file) {
            this.file = file;
        }

        @Override
        public int available() {
            if (isClosed) return 0;
            else return Math.max(file.data.length() - position, 0);
        }

        @Override
        public void close() {
            isClosed = true;
        }

        @Override
        public int read() throws IOException {
            if (!isClosed) {
                if (available() == 0) return -1;
                else {
                    position += 1;
                    return file.data.get(position - 1) & 0xFF;
                }
            } else throw new IOException("file is closed");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!isClosed) {
                final int count = available();
                if (count == 0) return -1;
                else {
                    final int n = Math.min(len, count);
                    file.data.copyTo(position, b, off, n);
                    position += n;
                    return n;
                }
            } else throw new IOException("file is closed");
        }

        @Override
        public synchronized void reset() throws IOException {
            if (!isClosed) {
                position = 0;
            } else throw new IOException("file is closed");
        }

        @Override
        public long skip(long n) throws IOException {
            if (!isClosed) {
                position = (int) Math.min(position + n, Integer.MAX_VALUE);
                return position;
            } else throw new IOException("file is closed");
        }
    }

    // ----------------------------------------------------------------------- //

    protected static class VirtualOutputHandle extends OutputHandle {
        public final VirtualFile file;

        public long position;

        public VirtualOutputHandle(VirtualFile file, OutputStreamFileSystem owner, int handle, String path) {
            super(owner, handle, path);
            this.file = file;
            this.position = file.data.length();
        }

        @Override
        public long length() {
            return file.size();
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public void close() {
            if (!isClosed()) {
                super.close();
                assert file.handle.isPresent() && file.handle.get() == this;
                file.handle = Optional.empty();
            }
        }

        @Override
        public long seek(long to) throws IOException {
            if (to < 0) throw new IOException("invalid offset");
            position = to;
            return position;
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (!isClosed()) {
                final int pos = (int) position;
                file.data.growTo(pos + b.length);
                file.data.copyFrom(b, pos);
                position += b.length;
                file.lastModified = System.currentTimeMillis();
            } else throw new IOException("file is closed");
        }
    }
}
