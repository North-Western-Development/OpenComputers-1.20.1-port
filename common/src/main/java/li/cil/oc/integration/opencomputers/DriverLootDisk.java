package li.cil.oc.integration.opencomputers;

import dev.architectury.utils.GameInstance;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;

// This is deprecated and kept for compatibility with old saves.
// As of OC 1.5.10, loot disks are generated using normal floppies, and using
// a factory system that allows third-party mods to register loot disks.
public final class DriverLootDisk implements Item {
    public static final DriverLootDisk INSTANCE = new DriverLootDisk();

    private DriverLootDisk() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.Floppy)) &&
                (stack.hasTag() && stack.getTag().contains(Settings.namespace + "lootPath"));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        final MinecraftServer server = GameInstance.getServer();
        if (!host.world().isClientSide && stack.hasTag() && server != null) {
            final String lootPath = Settings.savePath + "loot/" + stack.getTag().getString(Settings.namespace + "lootPath");
            final File savePath = server.getWorldPath(new LevelResource(lootPath)).toFile();
            final FileSystem fs;
            if (savePath.exists() && savePath.isDirectory()) {
                fs = li.cil.oc.api.FileSystem.fromSaveDirectory(lootPath, 0, false);
            } else {
                fs = li.cil.oc.api.FileSystem.fromResource(new ResourceLocation(Settings.resourceDomain, lootPath));
            }
            final String label = dataTag(stack).contains(Settings.namespace + "fs.label")
                    ? dataTag(stack).getString(Settings.namespace + "fs.label")
                    : null;
            return li.cil.oc.api.FileSystem.asManagedEnvironment(fs, label, host, Settings.resourceDomain + ":floppy_access");
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Floppy;
    }
}
