package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static li.cil.oc.util.ResultWrapper.result;

public class FileSystem extends AbstractManagedEnvironment implements DeviceInfo {
    public final li.cil.oc.api.fs.FileSystem fileSystem;
    public Label label;
    public final Optional<EnvironmentHost> host;
    public final Optional<String> sound;
    public final int speed;

    public final ComponentConnector node;

    private final Map<String, Set<Integer>> owners = new HashMap<>();

    public final double[] readCosts = {1.0 / 1, 1.0 / 4, 1.0 / 7, 1.0 / 10, 1.0 / 13, 1.0 / 15};
    public final double[] seekCosts = {1.0 / 1, 1.0 / 4, 1.0 / 7, 1.0 / 10, 1.0 / 13, 1.0 / 15};
    public final double[] writeCosts = {1.0 / 1, 1.0 / 2, 1.0 / 3, 1.0 / 4, 1.0 / 5, 1.0 / 6};

    public FileSystem(li.cil.oc.api.fs.FileSystem fileSystem, Label label, Optional<EnvironmentHost> host, Optional<String> sound, int speed) {
        this.fileSystem = fileSystem;
        this.label = label;
        this.host = host;
        this.sound = sound;
        this.speed = speed;
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Network).
                withComponent("filesystem", Visibility.Neighbors).
                withConnector().
                create();
        setNode(node);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Volume);
            info.put(DeviceAttribute.Description, "Filesystem");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "MPFS.21.6");
            info.put(DeviceAttribute.Capacity, Integer.toString((int) (fileSystem.spaceTotal() * 1.024)));
            info.put(DeviceAttribute.Size, Long.toString(fileSystem.spaceTotal()));
            info.put(DeviceAttribute.Clock, ((int) (2000 / readCosts[speed]) / 100) + "/" + ((int) (2000 / seekCosts[speed]) / 100) + "/" + ((int) (2000 / writeCosts[speed]) / 100));
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():string -- Get the current label of the drive.")
    public Object[] getLabel(Context context, Arguments args) {
        synchronized (fileSystem) {
            if (label != null) return result(label.getLabel());
            else return null;
        }
    }

    @Callback(doc = "function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.")
    public Object[] setLabel(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            if (label == null) throw new Exception("drive does not support labeling");
            if (args.checkAny(0) == null) label.setLabel(null);
            else label.setLabel(args.checkString(0));
            return result(label.getLabel());
        }
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the file system is read-only.")
    public Object[] isReadOnly(Context context, Arguments args) {
        synchronized (fileSystem) {
            return result(fileSystem.isReadOnly());
        }
    }

    @Callback(direct = true, doc = "function():number -- The overall capacity of the file system, in bytes.")
    public Object[] spaceTotal(Context context, Arguments args) {
        synchronized (fileSystem) {
            final long space = fileSystem.spaceTotal();
            if (space < 0) return result(Double.POSITIVE_INFINITY);
            else return result(space);
        }
    }

    @Callback(direct = true, doc = "function():number -- The currently used capacity of the file system, in bytes.")
    public Object[] spaceUsed(Context context, Arguments args) {
        synchronized (fileSystem) {
            return result(fileSystem.spaceUsed());
        }
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether an object exists at the specified absolute path in the file system.")
    public Object[] exists(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            diskActivity();
            return result(fileSystem.exists(clean(args.checkString(0))));
        }
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns the size of the object at the specified absolute path in the file system.")
    public Object[] size(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            diskActivity();
            return result(fileSystem.size(clean(args.checkString(0))));
        }
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether the object at the specified absolute path in the file system is a directory.")
    public Object[] isDirectory(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            diskActivity();
            return result(fileSystem.isDirectory(clean(args.checkString(0))));
        }
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns the (real world) timestamp of when the object at the specified absolute path in the file system was modified.")
    public Object[] lastModified(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            diskActivity();
            return result(fileSystem.lastModified(clean(args.checkString(0))));
        }
    }

    @Callback(doc = "function(path:string):table -- Returns a list of names of objects in the directory at the specified absolute path in the file system.")
    public Object[] list(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            final String[] list = fileSystem.list(clean(args.checkString(0)));
            if (list != null) {
                diskActivity();
                return new Object[]{list};
            } else return null;
        }
    }

    @Callback(doc = "function(path:string):boolean -- Creates a directory at the specified absolute path in the file system. Creates parent directories, if necessary.")
    public Object[] makeDirectory(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            final boolean success = makeDirectoryRecursive(clean(args.checkString(0)));
            diskActivity();
            return result(success);
        }
    }

    private boolean makeDirectoryRecursive(String path) {
        if (fileSystem.exists(path)) return false;
        if (fileSystem.makeDirectory(path)) return true;
        final String[] parts = path.split("/");
        final String parent = String.join("/", Arrays.copyOf(parts, Math.max(0, parts.length - 1)));
        return makeDirectoryRecursive(parent) && fileSystem.makeDirectory(path);
    }

    @Callback(doc = "function(path:string):boolean -- Removes the object at the specified absolute path in the file system.")
    public Object[] remove(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            final boolean success = removeRecursive(clean(args.checkString(0)));
            diskActivity();
            return result(success);
        }
    }

    private boolean removeRecursive(String parent) {
        if (fileSystem.isDirectory(parent)) {
            for (String child : fileSystem.list(parent)) {
                if (!removeRecursive(parent + "/" + child)) return false;
            }
        }
        return fileSystem.delete(parent);
    }

    @Callback(doc = "function(from:string, to:string):boolean -- Renames/moves an object from the first specified absolute path in the file system to the second.")
    public Object[] rename(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            final boolean success = fileSystem.rename(clean(args.checkString(0)), clean(args.checkString(1)));
            diskActivity();
            return result(success);
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata) -- Closes an open file descriptor with the specified handle.")
    public Object[] close(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            close(context, checkHandle(args, 0));
            return null;
        }
    }

    @Callback(direct = true, limit = 4, doc = "function(path:string[, mode:string='r']):userdata -- Opens a new file descriptor and returns its handle.")
    public Object[] open(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            final Set<Integer> owned = owners.get(context.node().address());
            if (owned != null && owned.size() >= Settings.get().maxHandles) {
                throw new IOException("too many open handles");
            }
            final String path = args.checkString(0);
            final String mode = args.optString(1, "r");
            final int handle = fileSystem.open(clean(path), parseMode(mode));
            if (handle > 0) {
                owners.computeIfAbsent(context.node().address(), k -> new HashSet<>()).add(handle);
            }
            diskActivity();
            return result(new HandleValue(node.address(), handle));
        }
    }

    @Callback(direct = true, limit = 15, doc = "function(handle:userdata, count:number):string or nil -- Reads up to the specified amount of data from an open file descriptor with the specified handle. Returns nil when EOF is reached.")
    public Object[] read(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            context.consumeCallBudget(readCosts[speed]);
            final int handle = checkHandle(args, 0);
            final int n = Math.min(Settings.get().maxReadBuffer, Math.max(0, args.checkInteger(1)));
            checkOwner(context.node().address(), handle);
            final Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                // Limit size of read buffer to avoid crazy allocations.
                final byte[] buffer = new byte[n];
                final int read = file.read(buffer);
                if (read >= 0) {
                    final byte[] bytes;
                    if (read == buffer.length)
                        bytes = buffer;
                    else {
                        bytes = new byte[read];
                        System.arraycopy(buffer, 0, bytes, 0, read);
                    }
                    if (!node.tryChangeBuffer(-Settings.get().hddReadCost * bytes.length)) {
                        throw new IOException("not enough energy");
                    }
                    diskActivity();
                    return result((Object) bytes);
                } else {
                    return result((Object) null);
                }
            } else throw new IOException("bad file descriptor");
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata, whence:string, offset:number):number -- Seeks in an open file descriptor with the specified handle. Returns the new pointer position.")
    public Object[] seek(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            context.consumeCallBudget(seekCosts[speed]);
            final int handle = checkHandle(args, 0);
            final String whence = args.checkString(1);
            final int offset = args.checkInteger(2);
            checkOwner(context.node().address(), handle);
            final Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                switch (whence) {
                    case "cur" -> file.seek(file.position() + offset);
                    case "set" -> file.seek(offset);
                    case "end" -> file.seek(file.length() + offset);
                    default -> throw new IllegalArgumentException("invalid mode");
                }
                return result(file.position());
            } else throw new IOException("bad file descriptor");
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata, value:string):boolean -- Writes the specified data to an open file descriptor with the specified handle.")
    public Object[] write(Context context, Arguments args) throws Exception {
        synchronized (fileSystem) {
            context.consumeCallBudget(writeCosts[speed]);
            final int handle = checkHandle(args, 0);
            final byte[] value = args.checkByteArray(1);
            if (!node.tryChangeBuffer(-Settings.get().hddWriteCost * value.length)) {
                throw new IOException("not enough energy");
            }
            checkOwner(context.node().address(), handle);
            final Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                file.write(value);
                diskActivity();
                return result(true);
            } else throw new IOException("bad file descriptor");
        }
    }

    // ----------------------------------------------------------------------- //

    public int checkHandle(Arguments args, int index) throws IOException {
        if (args.isInteger(index)) {
            return args.checkInteger(index);
        } else if (args.isTable(index)) {
            if (args.checkTable(index).get("handle") instanceof Number handle) return handle.intValue();
            else throw new IOException("bad file descriptor");
        } else if (args.checkAny(index) instanceof HandleValue handle) {
            return handle.handle;
        } else throw new IOException("bad file descriptor");
    }

    public void close(Context context, int handle) throws IOException {
        final Handle file = fileSystem.getHandle(handle);
        if (file != null) {
            final Set<Integer> set = owners.get(context.node().address());
            if (set != null && set.remove(handle)) file.close();
            else throw new IOException("bad file descriptor");
        } else throw new IOException("bad file descriptor");
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onMessage(Message message) {
        synchronized (fileSystem) {
            super.onMessage(message);
            if ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) {
                final Set<Integer> set = owners.get(message.source().address());
                if (set != null) {
                    for (int handle : set) {
                        final Handle file = fileSystem.getHandle(handle);
                        if (file != null) file.close();
                        // else: Invalid handle... huh.
                    }
                    set.clear();
                } // else: Computer had no open files.
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        synchronized (fileSystem) {
            super.onDisconnect(node);
            if (node == this.node) {
                fileSystem.close();
            } else if (owners.containsKey(node.address())) {
                for (int handle : owners.get(node.address())) {
                    final Handle file = fileSystem.getHandle(handle);
                    if (file != null) file.close();
                }
                owners.remove(node.address());
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);

        final ListTag ownersNbt = nbt.getList("owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < ownersNbt.size(); i++) {
            final CompoundTag ownerNbt = ownersNbt.getCompound(i);
            final String address = ownerNbt.getString("address");
            if (!address.isEmpty()) {
                final Set<Integer> handles = new HashSet<>();
                for (int handle : ownerNbt.getIntArray("handles")) handles.add(handle);
                owners.put(address, handles);
            }
        }

        if (label != null) {
            label.loadData(nbt);
        }
        fileSystem.loadData(nbt.getCompound("fs"));
    }

    @Override
    public void saveData(CompoundTag nbt) {
        synchronized (fileSystem) {
            super.saveData(nbt);

            if (label != null) {
                label.saveData(nbt);
            }

            if (!SaveHandler.savingForClients) {
                final ListTag ownersNbt = new ListTag();
                for (Map.Entry<String, Set<Integer>> entry : owners.entrySet()) {
                    final CompoundTag ownerNbt = new CompoundTag();
                    ownerNbt.putString("address", entry.getKey());
                    ownerNbt.put("handles", new IntArrayTag(entry.getValue().stream().mapToInt(Integer::intValue).toArray()));
                    ownersNbt.add(ownerNbt);
                }
                nbt.put("owners", ownersNbt);

                ExtendedNBT.setNewCompoundTag(nbt, "fs", fileSystem::saveData);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private String clean(String path) throws FileNotFoundException {
        final String result = com.google.common.io.Files.simplifyPath(path);
        if (result.startsWith("../") || result.equals("..")) throw new FileNotFoundException(path);
        if (result.equals("/") || result.equals(".")) return "";
        else return result;
    }

    private Mode parseMode(String value) {
        if (("r".equals(value)) || ("rb".equals(value))) return Mode.Read;
        if (("w".equals(value)) || ("wb".equals(value))) return Mode.Write;
        if (("a".equals(value)) || ("ab".equals(value))) return Mode.Append;
        throw new IllegalArgumentException("unsupported mode");
    }

    private void checkOwner(String owner, int handle) throws IOException {
        if (!owners.containsKey(owner) || !owners.get(owner).contains(handle))
            throw new IOException("bad file descriptor");
    }

    private void diskActivity() {
        if (sound.isPresent() && host.isPresent()) {
            PacketSender.sendFileSystemActivity(node, host.get(), sound.get());
        }
    }
}
