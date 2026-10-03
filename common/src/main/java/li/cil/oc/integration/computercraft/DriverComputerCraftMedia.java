package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.media.IMedia;
import li.cil.oc.Settings;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.integration.opencomputers.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

import java.util.UUID;

/**
 * Lets CC media (floppy disks, treasure disks, ...) be used in OC disk drives.
 */
public final class DriverComputerCraftMedia implements Item {
    public static final DriverComputerCraftMedia INSTANCE = new DriverComputerCraftMedia();

    private DriverComputerCraftMedia() {
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        // Records are media in CC (for the disk drive's audio), but have no data.
        return !stack.isEmpty() && !(stack.getItem() instanceof RecordItem) && ComputerCraftPlatform.getMedia(stack) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (host.world() instanceof ServerLevel level) {
            final IMedia media = ComputerCraftPlatform.getMedia(stack);
            if (media == null) return null;
            final String address = addressFromTag(dataTag(stack));
            final FileSystem fs = fromComputerCraft(media.createDataMount(stack, level));
            if (fs == null) return null;
            final ManagedEnvironment environment = li.cil.oc.api.FileSystem.asManagedEnvironment(fs, new ComputerCraftLabel(stack, media), host, Settings.resourceDomain + ":floppy_access");
            if (environment != null && environment.node() instanceof li.cil.oc.server.network.Node node) {
                node.setAddress(address);
            }
            return environment;
        }
        return null;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Floppy;
    }

    public static FileSystem fromComputerCraft(final Object mount) {
        if (mount instanceof WritableMount rw) return new ComputerCraftWritableFileSystem(rw);
        if (mount instanceof Mount ro) return new ComputerCraftFileSystem(ro);
        return null;
    }

    private static String addressFromTag(final CompoundTag tag) {
        if (tag.contains("node") && tag.getCompound("node").contains("address")) {
            return tag.getCompound("node").getString("address");
        }
        return UUID.randomUUID().toString();
    }

    public static final class ComputerCraftLabel implements Label {
        private final ItemStack stack;
        private final IMedia media;

        public ComputerCraftLabel(final ItemStack stack, final IMedia media) {
            this.stack = stack;
            this.media = media;
        }

        @Override
        public String getLabel() {
            return media.getLabel(stack);
        }

        @Override
        public void setLabel(final String value) {
            media.setLabel(stack, value);
        }

        @Override
        public void loadData(final CompoundTag nbt) {
        }

        @Override
        public void saveData(final CompoundTag nbt) {
        }
    }
}
