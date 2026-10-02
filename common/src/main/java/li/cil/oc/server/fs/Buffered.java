package li.cil.oc.server.fs;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.util.SafeThreadPool;
import li.cil.oc.util.ThreadPoolFactory;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Port note: Scala mixin trait (only ever used over {@link VirtualFileSystem});
 * now an abstract class.
 */
public abstract class Buffered extends VirtualFileSystem {
    public static final SafeThreadPool fileSaveHandler = ThreadPoolFactory.createSafePool("FileSystem", 1);

    protected abstract File fileRoot();

    private final Map<String, Long> deletions = new HashMap<>();

    // ----------------------------------------------------------------------- //

    @Override
    public boolean delete(String path) {
        if (super.delete(path)) {
            deletions.put(path, System.currentTimeMillis());
            return true;
        } else return false;
    }

    @Override
    public boolean rename(String from, String to) throws FileNotFoundException {
        if (super.rename(from, to)) {
            deletions.put(from, System.currentTimeMillis());
            return true;
        } else return false;
    }

    // ----------------------------------------------------------------------- //

    private Optional<Future<?>> saving = Optional.empty();

    @Override
    public void loadData(CompoundTag nbt) {
        saving.ifPresent(f -> {
            try {
                f.get(120L, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                OpenComputers.log.warn("Waiting for filesystem to save took two minutes! Aborting.");
            } catch (CancellationException e) {
                // NO-OP
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw FileSystem.sneakyThrow(e.getCause());
            }
        });
        loadFiles(nbt);
        super.loadData(nbt);
    }

    private void loadFiles(CompoundTag nbt) {
        synchronized (this) {
            final String[] rootList = fileRoot().list();
            if (rootList == null || rootList.length == 0) {
                fileRoot().delete();
            } else {
                try {
                    loadRecurse("", fileRoot());
                } catch (IOException e) {
                    throw FileSystem.sneakyThrow(e);
                }
            }
        }
    }

    private void loadRecurse(String path, File directory) throws IOException {
        makeDirectory(path);
        final File[] children = directory.listFiles();
        if (children != null) for (File child : children) {
            if (!FileSystem.INSTANCE.isValidFilename(child.getName())) continue;
            final String childPath = path + child.getName();
            final File childFile = new File(directory, child.getName());
            if (child.exists() && child.isDirectory() && child.list() != null) {
                loadRecurse(childPath + "/", childFile);
            } else if (!exists(childPath) || !isDirectory(childPath)) {
                final Optional<OutputHandle> maybeStream = openOutputHandle(0, childPath, Mode.Write);
                if (maybeStream.isPresent()) {
                    final OutputHandle stream = maybeStream.get();
                    try {
                        final InputStream in = new FileInputStream(childFile);
                        final byte[] buffer = new byte[8 * 1024];
                        int read;
                        do {
                            read = in.read(buffer);
                            if (read > 0) {
                                if (read == buffer.length) stream.write(buffer);
                                else stream.write(Arrays.copyOfRange(buffer, 0, read));
                            }
                        } while (read >= 0);
                        in.close();
                    } catch (FileNotFoundException e) {
                        // File got deleted in the meantime.
                    }
                    stream.close();
                    setLastModified(childPath, childFile.lastModified());
                }
                // Else: file is open for writing.
            }
        }
        setLastModified(path, directory.lastModified());
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        saving = fileSaveHandler.withPool(pool -> pool.submit(this::saveFiles));
    }

    public void saveFiles() {
        synchronized (this) {
            for (Map.Entry<String, Long> entry : deletions.entrySet()) {
                final File file = new File(fileRoot(), entry.getKey());
                if (FileUtils.isFileOlder(file, entry.getValue()))
                    FileUtils.deleteQuietly(file);
            }
            deletions.clear();

            final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
            final String[] rootList = list("");
            if (rootList == null || rootList.length == 0) {
                fileRoot().delete();
            } else {
                try {
                    saveRecurse("", buffer);
                } catch (IOException e) {
                    throw FileSystem.sneakyThrow(e);
                }
            }
        }
    }

    private boolean saveRecurse(String path, ByteBuffer buffer) throws IOException {
        final File directory = new File(fileRoot(), path);
        directory.mkdirs();
        boolean dirChanged = false;
        for (String child : list(path)) {
            final String childPath = path + child;
            if (isDirectory(childPath))
                dirChanged = saveRecurse(childPath, buffer) || dirChanged;
            else {
                final File childFile = new File(fileRoot(), childPath);
                final long time = lastModified(childPath);
                if (time == 0 || !childFile.exists() || FileUtils.isFileOlder(childFile, time)) {
                    FileUtils.deleteQuietly(childFile);
                    childFile.createNewFile();
                    final FileChannel out = new FileOutputStream(childFile).getChannel();
                    final InputChannel in = openInputChannel(childPath).get();

                    buffer.clear();
                    while (in.read(buffer) != -1) {
                        buffer.flip();
                        out.write(buffer);
                        buffer.compact();
                    }

                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        out.write(buffer);
                    }

                    out.close();
                    in.close();
                    childFile.setLastModified(time);
                    dirChanged = true;
                }
            }
        }
        if (dirChanged) {
            directory.setLastModified(lastModified(path));
            return true;
        } else return false;
    }
}
