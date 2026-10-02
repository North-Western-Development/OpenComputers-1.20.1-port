package li.cil.oc.server.component;

import com.google.common.io.Files;
import dev.architectury.utils.GameInstance;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.PacketSender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static li.cil.oc.util.ResultWrapper.result;

public class Drive extends AbstractManagedEnvironment implements DeviceInfo {
    public final int capacity;
    public final int platterCount;
    public final Label label;
    private final Optional<EnvironmentHost> host;
    public final Optional<String> sound;
    public final int speed;
    public final boolean isLocked;

    public final ComponentConnector node;

    public Drive(int capacity, int platterCount, Label label, Optional<EnvironmentHost> host, Optional<String> sound, int speed, boolean isLocked) {
        this.capacity = capacity;
        this.platterCount = platterCount;
        this.label = label;
        this.host = host;
        this.sound = sound;
        this.speed = speed;
        this.isLocked = isLocked;
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Network).
                withComponent("drive", Visibility.Neighbors).
                withConnector().
                create();
        setNode(node);
        this.data = new byte[capacity];
        this.sectorCount = capacity / sectorSize;
        this.sectorsPerPlatter = sectorCount / platterCount;
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    private File savePath() {
        return GameInstance.getServer().getWorldPath(LevelResource.ROOT).resolve(Settings.savePath + node.address() + ".bin").toFile();
    }

    private static final int sectorSize = 512;

    private final byte[] data;

    private final int sectorCount;

    private final int sectorsPerPlatter;

    private int headPos = 0;

    public final double[] readSectorCosts = {1.0 / 10, 1.0 / 20, 1.0 / 30, 1.0 / 40, 1.0 / 50, 1.0 / 60};
    public final double[] writeSectorCosts = {1.0 / 5, 1.0 / 10, 1.0 / 15, 1.0 / 20, 1.0 / 25, 1.0 / 30};
    public final double[] readByteCosts = {1.0 / 48, 1.0 / 64, 1.0 / 80, 1.0 / 96, 1.0 / 112, 1.0 / 128};
    public final double[] writeByteCosts = {1.0 / 24, 1.0 / 32, 1.0 / 40, 1.0 / 48, 1.0 / 56, 1.0 / 64};

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Disk);
            info.put(DeviceAttribute.Description, "Hard disk drive");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "MPD" + (capacity / 1024) + "L" + platterCount);
            info.put(DeviceAttribute.Capacity, Integer.toString((int) (capacity * 1.024)));
            info.put(DeviceAttribute.Size, Integer.toString(capacity));
            info.put(DeviceAttribute.Clock, ((int) (2000 / readSectorCosts[speed]) / 100) + "/" + ((int) (2000 / writeSectorCosts[speed]) / 100) + "/" + ((int) (2000 / readByteCosts[speed]) / 100) + "/" + ((int) (2000 / writeByteCosts[speed]) / 100));
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():string -- Get the current label of the drive.")
    public synchronized Object[] getLabel(Context context, Arguments args) {
        if (label != null) return result(label.getLabel());
        else return null;
    }

    @Callback(doc = "function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.")
    public synchronized Object[] setLabel(Context context, Arguments args) throws Exception {
        if (isLocked) throw new Exception("drive is read only");
        if (label == null) throw new Exception("drive does not support labeling");
        if (args.checkAny(0) == null) label.setLabel(null);
        else label.setLabel(args.checkString(0));
        return result(label.getLabel());
    }

    @Callback(direct = true, doc = "function():number -- Returns the total capacity of the drive, in bytes.")
    public Object[] getCapacity(Context context, Arguments args) {
        return result(capacity);
    }

    @Callback(direct = true, doc = "function():number -- Returns the size of a single sector on the drive, in bytes.")
    public Object[] getSectorSize(Context context, Arguments args) {
        return result(sectorSize);
    }

    @Callback(direct = true, doc = "function():number -- Returns the number of platters in the drive.")
    public Object[] getPlatterCount(Context context, Arguments args) {
        return result(platterCount);
    }

    @Callback(direct = true, doc = "function(sector:number):string -- Read the current contents of the specified sector.")
    public synchronized Object[] readSector(Context context, Arguments args) {
        context.consumeCallBudget(readSectorCosts[speed]);
        final int sector = moveToSector(context, checkSector(args, 0));
        diskActivity();
        final byte[] sectorData = new byte[sectorSize];
        System.arraycopy(data, sectorOffset(sector), sectorData, 0, sectorSize);
        return result((Object) sectorData);
    }

    @Callback(direct = true, doc = "function(sector:number, value:string) -- Write the specified contents to the specified sector.")
    public synchronized Object[] writeSector(Context context, Arguments args) throws Exception {
        if (isLocked) throw new Exception("drive is read only");
        context.consumeCallBudget(writeSectorCosts[speed]);
        final byte[] sectorData = args.checkByteArray(1);
        final int sector = moveToSector(context, checkSector(args, 0));
        diskActivity();
        System.arraycopy(sectorData, 0, data, sectorOffset(sector), Math.min(sectorSize, sectorData.length));
        return null;
    }

    @Callback(direct = true, doc = "function(offset:number):number -- Read a single byte at the specified offset.")
    public synchronized Object[] readByte(Context context, Arguments args) {
        context.consumeCallBudget(readByteCosts[speed]);
        final int offset = args.checkInteger(0) - 1;
        moveToSector(context, checkSector(offset));
        diskActivity();
        return result(data[offset]);
    }

    @Callback(direct = true, doc = "function(offset:number, value:number) -- Write a single byte to the specified offset.")
    public synchronized Object[] writeByte(Context context, Arguments args) throws Exception {
        if (isLocked) throw new Exception("drive is read only");
        context.consumeCallBudget(writeByteCosts[speed]);
        final int offset = args.checkInteger(0) - 1;
        final byte value = (byte) args.checkInteger(1);
        moveToSector(context, checkSector(offset));
        diskActivity();
        data[offset] = value;
        return null;
    }

    // ----------------------------------------------------------------------- //

    private static final String HeadPosTag = "headPos";

    @Override
    public synchronized void loadData(CompoundTag nbt) {
        super.loadData(nbt);

        if (node.address() != null) try {
            final File path = savePath();
            if (path.exists()) {
                final ByteArrayInputStream bin = new ByteArrayInputStream(Files.toByteArray(path));
                final GZIPInputStream zin = new GZIPInputStream(bin);
                int offset = 0;
                int read = 0;
                while (read >= 0 && offset < data.length) {
                    read = zin.read(data, offset, data.length - offset);
                    offset += read;
                }
            }
        } catch (Throwable t) {
            OpenComputers.log.warn("Failed loading drive contents for '" + node.address() + "'.", t);
        }

        headPos = Math.min(Math.max(nbt.getInt(HeadPosTag), 0), sectorToHeadPos(sectorCount));

        if (label != null) {
            label.loadData(nbt);
        }
    }

    @Override
    public synchronized void saveData(CompoundTag nbt) {
        super.saveData(nbt);

        if (node.address() != null) try {
            final File path = savePath();
            path.getParentFile().mkdirs();
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(bos);
            zos.write(data);
            zos.close();
            Files.write(bos.toByteArray(), path);
        } catch (Throwable t) {
            OpenComputers.log.warn("Failed saving drive contents for '" + node.address() + "'.", t);
        }

        nbt.putInt(HeadPosTag, headPos);

        if (label != null) {
            label.saveData(nbt);
        }
    }

    // ----------------------------------------------------------------------- //

    private int validateSector(int sector) {
        if (sector < 0 || sector >= sectorCount)
            throw new IllegalArgumentException("invalid offset, not in a usable sector");
        return sector;
    }

    private int checkSector(int offset) {
        return validateSector(offsetSector(offset));
    }

    private int checkSector(Arguments args, int n) {
        return validateSector(args.checkInteger(n) - 1);
    }

    private int moveToSector(Context context, int sector) {
        final int newHeadPos = sectorToHeadPos(sector);
        if (headPos != newHeadPos) {
            final int delta = Math.abs(headPos - newHeadPos);
            if (delta > Settings.get().sectorSeekThreshold) context.pause(Settings.get().sectorSeekTime);
            headPos = newHeadPos;
        }
        return sector;
    }

    private int sectorToHeadPos(int sector) {
        return sector % sectorsPerPlatter;
    }

    private int sectorOffset(int sector) {
        return sector * sectorSize;
    }

    private int offsetSector(int offset) {
        return offset / sectorSize;
    }

    private void diskActivity() {
        if (sound.isPresent() && host.isPresent()) {
            PacketSender.sendFileSystemActivity(node, host.get(), sound.get());
        }
    }
}
