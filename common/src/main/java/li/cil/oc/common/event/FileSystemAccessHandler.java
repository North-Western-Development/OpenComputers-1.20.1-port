package li.cil.oc.common.event;

import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.common.tileentity.Case;
import li.cil.oc.common.tileentity.DiskDrive;
import li.cil.oc.common.tileentity.Raid;
import li.cil.oc.server.component.DiskDriveMountable;
import li.cil.oc.server.component.Server;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class FileSystemAccessHandler {
    private FileSystemAccessHandler() {
    }

    public static void register() {
        EventBus.INSTANCE.register(FileSystemAccessEvent.Server.class, FileSystemAccessHandler::onFileSystemAccess);
        EventBus.INSTANCE.register(FileSystemAccessEvent.Client.class, FileSystemAccessHandler::onFileSystemAccess);
    }

    public static void onFileSystemAccess(FileSystemAccessEvent.Server e) {
        if (e.getBlockEntity() instanceof Rack t) {
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                Object mountable = t.getMountable(slot);
                if (mountable instanceof Server server) {
                    boolean containsNode = server.componentSlot(e.getNode().address()) >= 0;
                    if (containsNode) {
                        server.lastFileSystemAccess = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                }
                else if (mountable instanceof DiskDriveMountable diskDrive) {
                    boolean containsNode = diskDrive.filesystemNode().isPresent() && diskDrive.filesystemNode().get() == e.getNode();
                    if (containsNode) {
                        diskDrive.lastAccess = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                }
            }
        }
    }

    public static void onFileSystemAccess(FileSystemAccessEvent.Client e) {
        float volume = Settings.get().soundVolume;
        SoundEvent sound = SoundEvent.createVariableRangeEvent(new ResourceLocation(e.getSound()));
        e.getWorld().playLocalSound(e.getX(), e.getY(), e.getZ(), sound, SoundSource.BLOCKS, volume, 1, false);
        if (e.getBlockEntity() instanceof DiskDrive t) t.lastAccess = System.currentTimeMillis();
        else if (e.getBlockEntity() instanceof Case t) t.lastFileSystemAccess = System.currentTimeMillis();
        else if (e.getBlockEntity() instanceof Raid t) t.lastAccess = System.currentTimeMillis();
    }
}
