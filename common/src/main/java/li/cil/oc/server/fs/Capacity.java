package li.cil.oc.server.fs;

import li.cil.oc.Settings;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Port note: Scala {@code trait Capacity extends OutputStreamFileSystem} was a
 * stackable mixin over different file system bases. In Java it is a marker
 * interface plus the {@link Tracker} that holds the state and implements the
 * behaviour; implementing classes (see the private classes in {@link FileSystem})
 * forward {@code spaceTotal}, {@code spaceUsed}, {@code delete}, {@code makeDirectory},
 * {@code close}, {@code loadData}, {@code saveData} and {@code openOutputHandle} to it,
 * passing their {@code super} implementation.
 */
public interface Capacity extends li.cil.oc.api.fs.FileSystem {
    Tracker capacityTracker();

    final class Tracker {
        private final OutputStreamFileSystem owner;

        private final long capacity;

        private long used;

        // Used when loading data from disk to virtual file systems, to allow
        // exceeding the actual capacity of a file system.
        private boolean ignoreCapacity = false;

        /**
         * Must be constructed once the owning file system is otherwise fully initialized.
         */
        public Tracker(OutputStreamFileSystem owner, long capacity) {
            this.owner = owner;
            this.capacity = capacity;
            this.used = computeSize("/");
        }

        public long capacity() {
            return capacity;
        }

        // ------------------------------------------------------------------- //

        public long spaceTotal() {
            return capacity;
        }

        public long spaceUsed() {
            return used;
        }

        // ------------------------------------------------------------------- //

        public boolean delete(String path, Predicate<String> superDelete) {
            final long freed = Settings.get().fileCost + owner.size(path);
            if (superDelete.test(path)) {
                used = Math.max(0, used - freed);
                return true;
            } else return false;
        }

        @FunctionalInterface
        public interface Rename {
            boolean rename(String from, String to) throws java.io.FileNotFoundException;
        }

        /** Renaming onto an existing file replaces it, so its space is freed. */
        public boolean rename(String from, String to, Rename superRename) throws java.io.FileNotFoundException {
            if (owner.exists(to) && !from.equals(to)) {
                final long freed = Settings.get().fileCost + owner.size(to);
                if (superRename.rename(from, to)) {
                    used = Math.max(0, used - freed);
                    return true;
                } else return false;
            } else return superRename.rename(from, to);
        }

        public boolean makeDirectory(String path, Predicate<String> superMakeDirectory) {
            if (capacity - used < Settings.get().fileCost && !ignoreCapacity) {
                throw FileSystem.sneakyThrow(new IOException("not enough space"));
            }
            if (superMakeDirectory.test(path)) {
                used += Settings.get().fileCost;
                return true;
            } else return false;
        }

        // ------------------------------------------------------------------- //

        /**
         * Call after {@code super.close()}.
         */
        public void onClose() {
            used = computeSize("/");
        }

        // ------------------------------------------------------------------- //

        public void loadData(CompoundTag nbt, Consumer<CompoundTag> superLoadData) {
            try {
                ignoreCapacity = true;
                superLoadData.accept(nbt);
            } finally {
                ignoreCapacity = false;
            }

            used = computeSize("/");
        }

        /**
         * Call after {@code super.saveData(nbt)}.
         */
        public void saveData(CompoundTag nbt) {
            // For the tooltip.
            nbt.putLong("capacity.used", used);
        }

        // ------------------------------------------------------------------- //

        public Optional<OutputStreamFileSystem.OutputHandle> openOutputHandle(int id, String path, Mode mode, OutputStreamFileSystem.OutputHandleOpener superOpen) throws IOException {
            final long delta;
            if (owner.exists(path))
                if (mode == Mode.Write)
                    delta = -owner.size(path); // Overwrite, file gets cleared.
                else
                    delta = 0; // Append, no immediate changes.
            else
                delta = Settings.get().fileCost; // File creation.
            if (capacity - used < delta && !ignoreCapacity) {
                throw new IOException("not enough space");
            }
            final Optional<OutputStreamFileSystem.OutputHandle> result = superOpen.open(id, path, mode);
            if (result.isEmpty()) return Optional.empty();
            final OutputStreamFileSystem.OutputHandle stream = result.get();
            used = Math.max(0, used + delta);
            if (mode == Mode.Append) {
                stream.seek(stream.length());
            }
            return Optional.of(new CountingOutputHandle(this, stream));
        }

        // ------------------------------------------------------------------- //

        private long computeSize(String path) {
            long result = Settings.get().fileCost + owner.size(path);
            if (owner.isDirectory(path)) {
                final String[] children = owner.list(path);
                if (children != null) for (String child : children) result += computeSize(path + child);
            }
            return result;
        }

        protected static class CountingOutputHandle extends OutputStreamFileSystem.OutputHandle {
            public final Tracker tracker;
            public final OutputStreamFileSystem.OutputHandle inner;

            protected CountingOutputHandle(Tracker tracker, OutputStreamFileSystem.OutputHandle inner) {
                super(inner.owner, inner.handle, inner.path);
                this.tracker = tracker;
                this.inner = inner;
            }

            @Override
            public boolean isClosed() {
                return inner.isClosed();
            }

            @Override
            public long length() {
                return inner.length();
            }

            @Override
            public long position() {
                return inner.position();
            }

            @Override
            public void close() {
                inner.close();
            }

            @Override
            public long seek(long to) throws IOException {
                return inner.seek(to);
            }

            @Override
            public void write(byte[] b) throws IOException {
                if (tracker.capacity - tracker.used < b.length && !tracker.ignoreCapacity)
                    throw new IOException("not enough space");
                inner.write(b);
                tracker.used = tracker.used + b.length;
            }
        }
    }
}
