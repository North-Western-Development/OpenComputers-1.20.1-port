package li.cil.oc.integration.opencomputers;

import dev.architectury.utils.GameInstance;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Loot;
import li.cil.oc.common.Slot;
import li.cil.oc.common.item.FloppyDisk;
import li.cil.oc.common.item.HardDiskDrive;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.server.component.Drive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public final class DriverFileSystem implements Item {
    public static final DriverFileSystem INSTANCE = new DriverFileSystem();

    public static final Pattern UUIDVerifier = Pattern.compile("^([0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12})$");

    private DriverFileSystem() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.HDDTier1),
                Items.get(Constants.ItemName.HDDTier2),
                Items.get(Constants.ItemName.HDDTier3),
                Items.get(Constants.ItemName.Floppy)) &&
                (!stack.hasTag() || !stack.getTag().contains(Settings.namespace + "lootPath"));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        if (stack.getItem() instanceof HardDiskDrive hdd) {
            return createEnvironment(stack, hdd.kiloBytes * 1024, hdd.platterCount, host, hdd.tier + 2);
        }
        if (stack.getItem() instanceof FloppyDisk) {
            return createEnvironment(stack, Settings.get().floppySize * 1024, 1, host, 1);
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        if (stack.getItem() instanceof HardDiskDrive) return Slot.HDD;
        if (stack.getItem() instanceof FloppyDisk) return Slot.Floppy;
        throw new IllegalArgumentException();
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof HardDiskDrive hdd) return hdd.tier;
        return 0;
    }

    private ManagedEnvironment createEnvironment(ItemStack stack, int capacity, int platterCount, EnvironmentHost host, int speed) {
        if (GameInstance.getServer() == null) return null;
        if (stack.hasTag() && stack.getTag().contains(Settings.namespace + "lootFactory")) {
            // Loot disk, create file system using factory callback.
            final ResourceLocation lootFactory = new ResourceLocation(stack.getTag().getString(Settings.namespace + "lootFactory"));
            final Callable<FileSystem> factory = Loot.factories.get(lootFactory);
            if (factory == null) return null; // Invalid loot disk.
            final String label = dataTag(stack).contains(Settings.namespace + "fs.label")
                    ? dataTag(stack).getString(Settings.namespace + "fs.label")
                    : null;
            final FileSystem fs;
            try {
                fs = factory.call();
            } catch (Exception e) {
                OpenComputers.log.warn("Error creating loot disk file system for '" + lootFactory + "'.", e);
                return null;
            }
            return li.cil.oc.api.FileSystem.asManagedEnvironment(fs, label, host, Settings.resourceDomain + ":floppy_access");
        } else {
            // We have a bit of a chicken-egg problem here, because we want to use the
            // node's address as the folder name... so we generate the address here,
            // if necessary. No one will know, right? Right!?
            final String address = addressFromTag(dataTag(stack));
            Label label = new ReadWriteItemLabel(stack);
            final boolean isFloppy = Items.get(stack) == Items.get(Constants.ItemName.Floppy);
            final String sound = Settings.resourceDomain + ":" + (isFloppy ? "floppy_access" : "hdd_access");
            final DriveData drive = new DriveData(stack);
            final ManagedEnvironment environment;
            if (drive.isUnmanaged) {
                environment = new Drive(Math.max(capacity, 0), platterCount, label, Optional.ofNullable(host), Optional.ofNullable(sound), speed, drive.isLocked());
            } else {
                FileSystem fs = li.cil.oc.api.FileSystem.fromSaveDirectory(address, Math.max(capacity, 0), Settings.get().bufferChanges);
                if (drive.isLocked()) {
                    fs = li.cil.oc.api.FileSystem.asReadOnly(fs);
                    label = new li.cil.oc.server.fs.FileSystem.ReadOnlyLabel(label.getLabel());
                }
                environment = li.cil.oc.api.FileSystem.asManagedEnvironment(fs, label, host, sound, speed);
            }
            if (environment != null && environment.node() != null) {
                ((li.cil.oc.server.network.Node) environment.node()).setAddress(address);
            }
            return environment;
        }
    }

    private static String addressFromTag(CompoundTag tag) {
        if (tag.contains("node") && tag.getCompound("node").contains("address")) {
            final String address = tag.getCompound("node").getString("address");
            if (UUIDVerifier.matcher(address).matches()) {
                return address;
            } else { // Invalid disk address.
                final String newAddress = UUID.randomUUID().toString();
                tag.getCompound("node").putString("address", newAddress);
                OpenComputers.log.warn("Generated new address for disk '" + newAddress + "'.");
                return newAddress;
            }
        } else return UUID.randomUUID().toString();
    }

    private static final class ReadWriteItemLabel extends li.cil.oc.server.fs.FileSystem.ItemLabel {
        private static final String LabelTag = Settings.namespace + "fs.label";

        private Optional<String> label = Optional.empty();

        ReadWriteItemLabel(ItemStack stack) {
            super(stack);
        }

        @Override
        public String getLabel() {
            return label.orElse(null);
        }

        @Override
        public void setLabel(String value) {
            label = Optional.ofNullable(value).map(v -> v.length() > 16 ? v.substring(0, 16) : v);
        }

        @Override
        public void loadData(CompoundTag nbt) {
            if (nbt.contains(LabelTag)) {
                label = Optional.of(nbt.getString(LabelTag));
            }
        }

        @Override
        public void saveData(CompoundTag nbt) {
            label.ifPresent(value -> nbt.putString(LabelTag, value));
        }
    }
}
