package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;

import java.io.FileNotFoundException;

class ReadOnlyWrapper implements li.cil.oc.api.fs.FileSystem {
    public final li.cil.oc.api.fs.FileSystem fileSystem;

    ReadOnlyWrapper(li.cil.oc.api.fs.FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public long spaceTotal() {
        return fileSystem.spaceUsed();
    }

    @Override
    public long spaceUsed() {
        return fileSystem.spaceUsed();
    }

    @Override
    public boolean exists(String path) {
        return fileSystem.exists(path);
    }

    @Override
    public long size(String path) {
        return fileSystem.size(path);
    }

    @Override
    public boolean isDirectory(String path) {
        return fileSystem.isDirectory(path);
    }

    @Override
    public long lastModified(String path) {
        return fileSystem.lastModified(path);
    }

    @Override
    public String[] list(String path) {
        return fileSystem.list(path);
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
    public boolean rename(String from, String to) {
        return false;
    }

    @Override
    public boolean setLastModified(String path, long time) {
        return false;
    }

    @Override
    public int open(String path, Mode mode) throws FileNotFoundException {
        switch (mode) {
            case Read:
                return fileSystem.open(path, mode);
            case Write:
                throw new FileNotFoundException("read-only filesystem; cannot open for writing: " + path);
            case Append:
                throw new FileNotFoundException("read-only filesystem; cannot open for appending: " + path);
            default:
                throw new MatchError(mode);
        }
    }

    @Override
    public li.cil.oc.api.fs.Handle getHandle(int handle) {
        return fileSystem.getHandle(handle);
    }

    @Override
    public void close() {
        fileSystem.close();
    }

    @Override
    public void loadData(CompoundTag nbt) {
        fileSystem.loadData(nbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        fileSystem.saveData(nbt);
    }

    private static final class MatchError extends RuntimeException {
        MatchError(Object value) {
            super(String.valueOf(value));
        }
    }
}
