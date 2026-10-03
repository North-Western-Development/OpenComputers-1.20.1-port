package li.cil.oc.server.fs;

import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import dev.architectury.utils.GameInstance;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.item.traits.FileSystemLike;
import li.cil.oc.integration.opencomputers.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port notes:
 * <ul>
 * <li>Scala {@code object FileSystem extends FileSystemAPI}: use {@link #INSTANCE}
 * (also for {@link #validatePath}, {@link #isValidFilename}, {@link #isCaseInsensitive()},
 * {@link #removeAddress}, {@link #fromMemory}).</li>
 * <li>{@link #validatePath(String)} still throws {@link IOException} for invalid paths,
 * but undeclared (sneaky), like the Scala original did from API methods.</li>
 * <li>{@link #fromResource(ResourceLocation)} reads {@code assets/<namespace>/<path>/}
 * from the owning mod's files via Architectury {@link Mod#findResource(String...)}
 * (works for jars and dev directories on both loaders), see {@link PathInputStreamFileSystem}.</li>
 * <li>Save directories live under {@code server.getWorldPath(LevelResource.ROOT)}.</li>
 * </ul>
 */
public final class FileSystem implements FileSystemAPI {
    public static final FileSystem INSTANCE = new FileSystem();

    private FileSystem() {
    }

    /**
     * Throws any throwable without declaring it (Scala threw checked exceptions
     * from methods whose Java signatures do not declare them).
     */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    private volatile Boolean isCaseInsensitive;

    public boolean isCaseInsensitive() {
        Boolean result = isCaseInsensitive;
        if (result == null) {
            synchronized (this) {
                result = isCaseInsensitive;
                if (result == null) {
                    result = computeIsCaseInsensitive();
                    isCaseInsensitive = result;
                }
            }
        }
        return result;
    }

    private static boolean computeIsCaseInsensitive() {
        if (Settings.get().forceCaseInsensitive) return true;
        try {
            final String uuid = UUID.randomUUID().toString();
            final File saveDir = saveRoot().toFile();
            final File lowerCase = new File(saveDir, uuid + "oc_rox");
            final File upperCase = new File(saveDir, uuid + "OC_ROX");
            // This should NEVER happen but could also lead to VERY weird bugs, so we
            // make sure the files don't exist.
            if (lowerCase.exists()) lowerCase.delete();
            if (upperCase.exists()) upperCase.delete();
            lowerCase.createNewFile();
            final boolean insensitive = upperCase.exists();
            lowerCase.delete();
            return insensitive;
        } catch (Throwable t) {
            // Among the security errors, createNewFile can throw an IOException.
            // We just fall back to assuming case insensitive, since that's always
            // safe in those cases.
            OpenComputers.log.warn("Couldn't determine if file system is case sensitive, falling back to insensitive.", t);
            return true;
        }
    }

    private static Path saveRoot() {
        final MinecraftServer server = GameInstance.getServer();
        if (server == null) throw new IllegalStateException("No server running.");
        return server.getWorldPath(LevelResource.ROOT);
    }

    // Worst-case: we're on Windows or using a FAT32 partition mounted in *nix.
    // Note: we allow / as the path separator and expect all \s to be converted
    // accordingly before the path is passed to the file system.
    private static final String invalidChars = "\\:*?\"<>|";

    public boolean isValidFilename(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (invalidChars.indexOf(name.charAt(i)) >= 0) return false;
        }
        return true;
    }

    /**
     * Throws an (undeclared) {@link IOException} if the path contains invalid characters.
     */
    public String validatePath(String path) {
        if (!isValidFilename(path)) {
            throw sneakyThrow(new IOException("path contains invalid characters"));
        }
        return path;
    }

    @Override
    public li.cil.oc.api.fs.FileSystem fromResource(ResourceLocation loc) {
        final String namespace = loc.getNamespace();
        final String innerPath = loc.getPath().trim();

        if (!Platform.isModLoaded(namespace)) return null;
        final Mod mod = Platform.getMod(namespace);

        final List<String> segments = new ArrayList<>();
        segments.add("assets");
        segments.add(namespace);
        for (String segment : innerPath.split("/")) {
            if (!segment.isEmpty()) segments.add(segment);
        }

        final Optional<Path> path = mod.findResource(segments.toArray(new String[0]));
        if (path.isPresent() && Files.isDirectory(path.get())) {
            return new PathInputStreamFileSystem(path.get());
        }
        return null;
    }

    @Override
    public Capacity fromSaveDirectory(String root, long capacity, boolean buffered) {
        final File path = saveRoot().resolve(Settings.savePath + root).toFile();
        if (!path.isDirectory()) {
            path.delete();
        }
        path.mkdirs();
        if (path.exists() && path.isDirectory()) {
            if (buffered) return new BufferedFileSystem(path, capacity);
            else return new ReadWriteFileSystem(path, capacity);
        } else return null;
    }

    public boolean removeAddress(ItemStack fsStack) {
        if (fsStack.getItem() instanceof FileSystemLike) {
            final CompoundTag data = Item.dataTagStatic(fsStack);
            if (data.contains("node")) {
                final CompoundTag nodeData = data.getCompound("node");
                if (nodeData.contains("address")) {
                    nodeData.remove("address");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public li.cil.oc.api.fs.FileSystem fromMemory(long capacity) {
        return new RamFileSystem(capacity);
    }

    @Override
    public li.cil.oc.api.fs.FileSystem asReadOnly(li.cil.oc.api.fs.FileSystem fileSystem) {
        if (fileSystem.isReadOnly()) return fileSystem;
        else return new ReadOnlyWrapper(fileSystem);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, Label label, EnvironmentHost host, String accessSound, int speed) {
        if (fileSystem == null) return null;
        return new li.cil.oc.server.component.FileSystem(fileSystem, label, Optional.ofNullable(host), Optional.ofNullable(accessSound), Math.min(Math.max(speed - 1, 0), 5));
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, String label, EnvironmentHost host, String accessSound, int speed) {
        return asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), host, accessSound, speed);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, Label label, EnvironmentHost host, String sound) {
        return asManagedEnvironment(fileSystem, label, host, sound, 1);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, String label, EnvironmentHost host, String sound) {
        return asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), host, sound, 1);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, Label label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, String label) {
        return asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), null, null, 1);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem) {
        return asManagedEnvironment(fileSystem, (Label) null, null, null, 1);
    }

    public abstract static class ItemLabel implements Label {
        public final ItemStack stack;

        protected ItemLabel(ItemStack stack) {
            this.stack = stack;
        }
    }

    public static class ReadOnlyLabel implements Label {
        private static final String LabelTag = Settings.namespace + "fs.label";

        public final String label;

        public ReadOnlyLabel(String label) {
            this.label = label;
        }

        @Override
        public void setLabel(String value) {
            throw new IllegalArgumentException("label is read only");
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public void loadData(CompoundTag nbt) {
        }

        @Override
        public void saveData(CompoundTag nbt) {
            if (label != null) {
                nbt.putString(LabelTag, label);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private static final class ReadOnlyFileSystem extends FileInputStreamFileSystem {
        private final File root;

        ReadOnlyFileSystem(File root) {
            this.root = root;
        }

        @Override
        protected File root() {
            return root;
        }
    }

    private static final class ReadWriteFileSystem extends FileOutputStreamFileSystem implements Capacity {
        private final Capacity.Tracker capacity;

        ReadWriteFileSystem(File root, long capacity) {
            super(root);
            this.capacity = new Capacity.Tracker(this, capacity);
        }

        @Override
        public Capacity.Tracker capacityTracker() {
            return capacity;
        }

        // Capacity overrides.

        @Override
        public long spaceTotal() {
            return capacity.spaceTotal();
        }

        @Override
        public long spaceUsed() {
            return capacity.spaceUsed();
        }

        @Override
        public boolean delete(String path) {
            return capacity.delete(path, super::delete);
        }

        @Override
        public boolean makeDirectory(String path) {
            return capacity.makeDirectory(path, super::makeDirectory);
        }

        @Override
        public boolean rename(String from, String to) {
            try {
                return capacity.rename(from, to, super::rename);
            } catch (java.io.FileNotFoundException e) {
                throw sneakyThrow(e);
            }
        }

        @Override
        public void close() {
            super.close();
            capacity.onClose();
        }

        @Override
        public void loadData(CompoundTag nbt) {
            capacity.loadData(nbt, super::loadData);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            capacity.saveData(nbt);
        }

        @Override
        protected Optional<OutputHandle> openOutputHandle(int id, String path, li.cil.oc.api.fs.Mode mode) throws IOException {
            return capacity.openOutputHandle(id, path, mode, super::openOutputHandle);
        }
    }

    private static final class RamFileSystem extends Volatile implements Capacity {
        private final Capacity.Tracker capacity;

        RamFileSystem(long capacity) {
            this.capacity = new Capacity.Tracker(this, capacity);
        }

        @Override
        public Capacity.Tracker capacityTracker() {
            return capacity;
        }

        // Capacity overrides.

        @Override
        public long spaceTotal() {
            return capacity.spaceTotal();
        }

        @Override
        public long spaceUsed() {
            return capacity.spaceUsed();
        }

        @Override
        public boolean delete(String path) {
            return capacity.delete(path, super::delete);
        }

        @Override
        public boolean makeDirectory(String path) {
            return capacity.makeDirectory(path, super::makeDirectory);
        }

        @Override
        public boolean rename(String from, String to) throws java.io.FileNotFoundException {
            return capacity.rename(from, to, super::rename);
        }

        @Override
        public void close() {
            super.close();
            capacity.onClose();
        }

        @Override
        public void loadData(CompoundTag nbt) {
            capacity.loadData(nbt, super::loadData);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            capacity.saveData(nbt);
        }

        @Override
        protected Optional<OutputHandle> openOutputHandle(int id, String path, li.cil.oc.api.fs.Mode mode) throws IOException {
            return capacity.openOutputHandle(id, path, mode, super::openOutputHandle);
        }
    }

    private static final class BufferedFileSystem extends Buffered implements Capacity {
        private final File fileRoot;
        private final Capacity.Tracker capacity;

        BufferedFileSystem(File fileRoot, long capacity) {
            this.fileRoot = fileRoot;
            this.capacity = new Capacity.Tracker(this, capacity);
        }

        @Override
        protected File fileRoot() {
            return fileRoot;
        }

        @Override
        public Capacity.Tracker capacityTracker() {
            return capacity;
        }

        @Override
        protected String[] segments(String path) {
            final String[] parts = super.segments(path);
            if (INSTANCE.isCaseInsensitive()) return toCaseInsensitive(parts);
            else return parts;
        }

        private String[] toCaseInsensitive(String[] path) {
            VirtualDirectory node = root;
            final String[] result = new String[path.length];
            for (int i = 0; i < path.length; i++) {
                final String segment = path[i];
                if (node == null) throw new AssertionError("assertion failed: corrupted virtual file system");
                String resolved = segment;
                for (Map.Entry<String, VirtualObject> entry : node.children.entrySet()) {
                    if (entry.getKey().toLowerCase().equals(segment.toLowerCase())) {
                        if (entry.getValue() instanceof VirtualDirectory child) {
                            node = child;
                            resolved = entry.getKey();
                        } else if (entry.getValue() instanceof VirtualFile) {
                            node = null;
                            resolved = entry.getKey();
                        }
                        break;
                    }
                }
                result[i] = resolved;
            }
            return result;
        }

        // Capacity overrides.

        @Override
        public long spaceTotal() {
            return capacity.spaceTotal();
        }

        @Override
        public long spaceUsed() {
            return capacity.spaceUsed();
        }

        @Override
        public boolean delete(String path) {
            return capacity.delete(path, super::delete);
        }

        @Override
        public boolean makeDirectory(String path) {
            return capacity.makeDirectory(path, super::makeDirectory);
        }

        @Override
        public boolean rename(String from, String to) throws java.io.FileNotFoundException {
            return capacity.rename(from, to, super::rename);
        }

        @Override
        public void close() {
            super.close();
            capacity.onClose();
        }

        @Override
        public void loadData(CompoundTag nbt) {
            capacity.loadData(nbt, super::loadData);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            capacity.saveData(nbt);
        }

        @Override
        protected Optional<OutputHandle> openOutputHandle(int id, String path, li.cil.oc.api.fs.Mode mode) throws IOException {
            return capacity.openOutputHandle(id, path, mode, super::openOutputHandle);
        }
    }
}
