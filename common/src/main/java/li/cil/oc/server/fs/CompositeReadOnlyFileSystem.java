package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

public class CompositeReadOnlyFileSystem implements li.cil.oc.api.fs.FileSystem {
    public LinkedHashMap<String, li.cil.oc.api.fs.FileSystem> parts = new LinkedHashMap<>();

    public CompositeReadOnlyFileSystem(LinkedHashMap<String, Callable<li.cil.oc.api.fs.FileSystem>> factories) {
        for (Map.Entry<String, Callable<li.cil.oc.api.fs.FileSystem>> entry : factories.entrySet()) {
            final li.cil.oc.api.fs.FileSystem fs;
            try {
                fs = entry.getValue().call();
            } catch (Exception e) {
                throw FileSystem.sneakyThrow(e);
            }
            if (fs != null) {
                parts.put(entry.getKey(), fs);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public long spaceTotal() {
        long sum = 0;
        for (li.cil.oc.api.fs.FileSystem fs : parts.values()) sum += fs.spaceTotal();
        return Math.max(spaceUsed(), sum);
    }

    @Override
    public long spaceUsed() {
        long sum = 0;
        for (li.cil.oc.api.fs.FileSystem fs : parts.values()) sum += fs.spaceUsed();
        return sum;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean exists(String path) {
        return findFileSystem(path).isPresent();
    }

    @Override
    public long size(String path) {
        return findFileSystem(path).map(fs -> fs.size(path)).orElse(0L);
    }

    @Override
    public boolean isDirectory(String path) {
        return findFileSystem(path).map(fs -> fs.isDirectory(path)).orElse(false);
    }

    @Override
    public long lastModified(String path) {
        return findFileSystem(path).map(fs -> fs.lastModified(path)).orElse(0L);
    }

    @Override
    public String[] list(String path) {
        if (isDirectory(path)) {
            final Set<String> acc = new LinkedHashSet<>();
            for (li.cil.oc.api.fs.FileSystem fs : parts.values()) {
                if (fs.exists(path)) try {
                    final String[] l = fs.list(path);
                    if (l != null) for (String e : l) {
                        final String f = e.endsWith("/") ? e.substring(0, e.length() - 1) : e;
                        final String d = f + "/";
                        // Avoid duplicates and always only use the latest entry.
                        acc.remove(f);
                        acc.remove(d);
                        acc.add(e);
                    }
                } catch (Throwable ignored) {
                }
            }
            return acc.toArray(new String[0]);
        } else return null;
    }

    // ----------------------------------------------------------------------- //

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

    // ----------------------------------------------------------------------- //

    @Override
    public int open(String path, Mode mode) throws FileNotFoundException {
        final Optional<li.cil.oc.api.fs.FileSystem> fs = findFileSystem(path);
        if (fs.isPresent()) return fs.get().open(path, mode);
        else throw new FileNotFoundException(path);
    }

    @Override
    public Handle getHandle(int handle) {
        for (li.cil.oc.api.fs.FileSystem fs : parts.values()) {
            final Handle result = fs.getHandle(handle);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public void close() {
        for (li.cil.oc.api.fs.FileSystem fs : parts.values()) fs.close();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadData(CompoundTag nbt) {
        for (Map.Entry<String, li.cil.oc.api.fs.FileSystem> entry : parts.entrySet()) {
            entry.getValue().loadData(nbt.getCompound(entry.getKey()));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        for (Map.Entry<String, li.cil.oc.api.fs.FileSystem> entry : parts.entrySet()) {
            ExtendedNBT.setNewCompoundTag(nbt, entry.getKey(), entry.getValue()::saveData);
        }
    }

    // ----------------------------------------------------------------------- //

    protected Optional<li.cil.oc.api.fs.FileSystem> findFileSystem(String path) {
        final List<li.cil.oc.api.fs.FileSystem> reversed = new ArrayList<>(parts.values());
        Collections.reverse(reversed);
        for (li.cil.oc.api.fs.FileSystem fs : reversed) {
            if (fs.exists(path)) return Optional.of(fs);
        }
        return Optional.empty();
    }
}
