package li.cil.oc.common;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.utils.GameInstance;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.SafeThreadPool;
import li.cil.oc.util.ThreadPoolFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

// Used by the native lua state to store kernel and stack data in auxiliary
// files instead of directly in the tile entity data, avoiding potential
// problems with the tile entity data becoming too large.
public final class SaveHandler {
    private static final String uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

    private static final int TimeToHoldOntoOldSaves = 60 * 1000;

    // THIS IS A MASSIVE HACK OF THE UGLIEST KINDS.
    // But it works, and the alternative would be to change the Persistable
    // interface to pass along this state to *everything that gets saved ever*,
    // which in 99% of the cases it doesn't need to know. So yes, this is fugly,
    // but the "clean" solution would be no less fugly.
    // Why is this even required? To avoid flushing file systems to disk and
    // avoid persisting machine states when sending description packets to clients,
    // which takes a lot of time and is completely unnecessary in those cases.
    public static volatile boolean savingForClients = false;

    public static class SaveDataEntry implements Runnable {
        public final byte[] data;
        public final ChunkPos pos;
        public final String name;
        public final ResourceLocation dimension;

        public SaveDataEntry(byte[] data, ChunkPos pos, String name, ResourceLocation dimension) {
            this.data = data;
            this.pos = pos;
            this.name = name;
            this.dimension = dimension;
        }

        @Override
        public void run() {
            final File path = statePath();
            final File dimPath = new File(path, dimension.toString().replace(':', '/').replace('.', '/'));
            final File chunkPath = new File(dimPath, pos.x + "." + pos.z);
            chunkDirs.add(chunkPath);
            if (!chunkPath.exists()) {
                chunkPath.mkdirs();
            }
            final File file = new File(chunkPath, name);
            try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file))) {
                fos.write(data);
            } catch (IOException e) {
                OpenComputers.log.warn("Error saving auxiliary tile entity data to '" + file.getAbsolutePath() + ".", e);
            }
        }
    }

    public static final SafeThreadPool stateSaveHandler = ThreadPoolFactory.createSafePool("SaveHandler", 1);

    public static final ConcurrentLinkedDeque<File> chunkDirs = new ConcurrentLinkedDeque<>();
    public static final Map<String, Future<?>> saving = new ConcurrentHashMap<>();

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // Formerly WorldEvent.Load (priority HIGHEST) / WorldEvent.Save (priority LOWEST).
        LifecycleEvent.SERVER_LEVEL_LOAD.register(SaveHandler::onWorldLoad);
        LifecycleEvent.SERVER_LEVEL_SAVE.register(SaveHandler::onWorldSave);
    }

    public static File savePath() {
        return GameInstance.getServer().getWorldPath(LevelResource.ROOT).resolve(Settings.savePath).normalize().toFile();
    }

    public static File statePath() {
        return new File(savePath(), "state");
    }

    public static void scheduleSave(MachineHost host, CompoundTag nbt, String name, byte[] data) {
        scheduleSave(BlockPosition.apply(host), nbt, name, data);
    }

    public static void scheduleSave(MachineHost host, CompoundTag nbt, String name, Consumer<CompoundTag> save) {
        scheduleSave(host, nbt, name, writeNBT(save));
    }

    public static void scheduleSave(EnvironmentHost host, CompoundTag nbt, String name, Consumer<CompoundTag> save) {
        scheduleSave(BlockPosition.apply(host), nbt, name, writeNBT(save));
    }

    public static void scheduleSave(Level world, double x, double z, CompoundTag nbt, String name, byte[] data) {
        scheduleSave(BlockPosition.apply(x, 0, z, world), nbt, name, data);
    }

    public static void scheduleSave(Level world, double x, double z, CompoundTag nbt, String name, Consumer<CompoundTag> save) {
        scheduleSave(world, x, z, nbt, name, writeNBT(save));
    }

    public static void scheduleSave(BlockPosition position, CompoundTag nbt, String name, byte[] data) {
        final Level world = position.world.get();
        final ResourceLocation dimension = world.dimension().location();
        final ChunkPos chunk = new ChunkPos(position.x >> 4, position.z >> 4);

        // We have to save the dimension and chunk coordinates, because they are
        // not available on load / may have changed if the computer was moved.
        nbt.putString("dimension", dimension.toString());
        nbt.putInt("chunkX", chunk.x);
        nbt.putInt("chunkZ", chunk.z);

        scheduleSave(dimension, chunk, name, data);
    }

    private static byte[] writeNBT(Consumer<CompoundTag> save) {
        final CompoundTag tmpNbt = new CompoundTag();
        save.accept(tmpNbt);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            NbtIo.write(tmpNbt, dos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public static CompoundTag loadNBT(CompoundTag nbt, String name) {
        final byte[] data = load(nbt, name);
        if (data.length > 0) {
            try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
                return NbtIo.read(dis);
            } catch (Throwable t) {
                OpenComputers.log.warn("There was an error trying to restore a block's state from external data. This indicates that data was somehow corrupted.", t);
                return new CompoundTag();
            }
        } else return new CompoundTag();
    }

    public static byte[] load(CompoundTag nbt, String name) {
        // Since we have no world yet, we rely on the dimension we were saved in.
        // Same goes for the chunk. This also works around issues with computers
        // being moved (e.g. Redstone in Motion).
        final String dimension = nbt.getString("dimension");
        final ChunkPos chunk = new ChunkPos(nbt.getInt("chunkX"), nbt.getInt("chunkZ"));

        // Wait for the latest save task for the requested file to complete.
        // This prevents the chance of loading an outdated version
        // of this file.
        final Future<?> f = saving.get(name);
        if (f != null) {
            try {
                f.get(120L, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                OpenComputers.log.warn("Waiting for state data to save took two minutes! Aborting.");
            } catch (CancellationException e) {
                // NO-OP
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                OpenComputers.log.warn("Error saving auxiliary tile entity data.", e);
            }
        }
        saving.remove(name);

        return load(new ResourceLocation(dimension), chunk, name);
    }

    public static void scheduleSave(ResourceLocation dimension, ChunkPos chunk, String name, byte[] data) {
        if (chunk == null) throw new IllegalArgumentException("chunk is null");
        else {
            // Disregarding whether or not there already was a
            // save submitted for the requested file
            // allows for better concurrency at the cost of
            // doing more writing operations.
            stateSaveHandler.withPool(pool -> pool.submit(new SaveDataEntry(data, chunk, name, dimension)))
                    .ifPresent(future -> saving.put(name, future));
        }
    }

    public static byte[] load(ResourceLocation dimension, ChunkPos chunk, String name) {
        if (chunk == null) throw new IllegalArgumentException("chunk is null");

        final File path = statePath();
        final File dimPath = new File(path, dimension.toString().replace(':', '/').replace('.', '/'));
        final File chunkPath = new File(dimPath, chunk.x + "." + chunk.z);
        final File file = new File(chunkPath, name);
        if (!file.exists()) return new byte[0];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8 * 1024];
            int read;
            do {
                read = bis.read(buffer);
                if (read > 0) {
                    bos.write(buffer, 0, read);
                }
            } while (read >= 0);
            return bos.toByteArray();
        } catch (IOException e) {
            OpenComputers.log.warn("Error loading auxiliary tile entity data.", e);
            return new byte[0];
        }
    }

    public static void cleanSaveData() {
        // Delete empty folders to keep the state folder clean.
        final File[] emptyDirs = savePath().listFiles(file -> file.isDirectory() &&
                // Make sure we only consider file system folders (UUID).
                file.getName().matches(uuidRegex) &&
                // We set the modified time in the save() method of unbuffered file
                // systems, to avoid deleting in-use folders here.
                System.currentTimeMillis() - file.lastModified() > TimeToHoldOntoOldSaves && isEmptyDir(file));
        if (emptyDirs != null) {
            for (File dir : emptyDirs) {
                if (dir != null) dir.delete();
            }
        }
    }

    private static boolean isEmptyDir(File file) {
        final String[] list = file.list();
        return list == null || list.length == 0;
    }

    private static void onWorldLoad(Level world) {
        if (!world.isClientSide()) {
            // Touch all externally saved data when loading, to avoid it getting
            // deleted in the next save (because the now - save time will usually
            // be larger than the time out after loading a world again).
            visitJava17(statePath());
        }
    }

    private static void onWorldSave(Level world) {
        stateSaveHandler.withPool(pool -> pool.submit(SaveHandler::cleanSaveData));
    }

    private static void visitJava17(File statePath) {
        if (!statePath.exists()) return;
        try {
            Files.walkFileTree(statePath.toPath(), new FileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    file.toFile().setLastModified(System.currentTimeMillis());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            OpenComputers.log.warn("Failed touching auxiliary state data.", e);
        }
    }

    private SaveHandler() {
    }
}
