package li.cil.oc.common;

import dev.architectury.event.events.common.LifecycleEvent;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.common.init.Items;
import li.cil.oc.util.Color;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;

// TODO(port): loot disks were not added to dungeon chests on 1.16.5 either (the
//  ChestGenHooks code was commented out). If wanted, inject a pool via
//  dev.architectury.event.events.common.LootEvent.MODIFY_LOOT_TABLE using
//  Settings.get().lootProbability and disksForSampling.
public final class Loot {
    public static final Map<ResourceLocation, Callable<FileSystem>> factories = new HashMap<>();

    public static final List<Pair<ItemStack, Integer>> globalDisks = new ArrayList<>();

    public static final List<Pair<ItemStack, Integer>> worldDisks = new ArrayList<>();

    public static List<ItemStack> disksForCycling() {
        return !disksForCyclingClient.isEmpty() ? disksForCyclingClient : disksForCyclingServer;
    }

    public static final List<ItemStack> disksForCyclingServer = new ArrayList<>();

    public static final List<ItemStack> disksForCyclingClient = new ArrayList<>();

    public static final List<ItemStack> disksForSampling = new ArrayList<>();

    public static final List<ItemStack> disksForClient = new ArrayList<>();

    private static boolean registered;

    /**
     * Registers the world load listener (formerly a Forge WorldEvent.Load subscription).
     */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        LifecycleEvent.SERVER_LEVEL_LOAD.register(Loot::initForWorld);
    }

    public static boolean isLootDisk(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.Floppy) &&
                stack.hasTag() && stack.getTag().contains(Settings.namespace + "lootFactory", Tag.TAG_STRING);
    }

    public static Optional<ItemStack> randomDisk(RandomSource rng) {
        if (!disksForSampling.isEmpty()) return Optional.of(disksForSampling.get(rng.nextInt(disksForSampling.size())));
        else return Optional.empty();
    }

    public static ItemStack registerLootDisk(String name, ResourceLocation loc, DyeColor color, Callable<FileSystem> factory, boolean doRecipeCycling) {
        OpenComputers.log.debug("Registering loot disk '" + name + "' from mod " + loc.getNamespace() + ".");

        final CompoundTag data = new CompoundTag();
        data.putString(Settings.namespace + "fs.label", name);

        final ItemStack stack = li.cil.oc.api.Items.get(Constants.ItemName.Floppy).createItemStack(1);
        final CompoundTag nbt = stack.getOrCreateTag();
        nbt.put(Settings.namespace + "data", data);

        // Store this top level, so it won't get wiped on save.
        nbt.putString(Settings.namespace + "lootFactory", loc.toString());
        nbt.putInt(Settings.namespace + "color", color.getId());

        factories.put(loc, factory);

        if (doRecipeCycling) {
            disksForCyclingServer.add(stack);
        }

        return stack.copy();
    }

    public static void init() {
        final Properties list = new Properties();
        try (InputStream listStream = Loot.class.getResourceAsStream("/assets/" + Settings.resourceDomain + "/loot/loot.properties")) {
            list.load(listStream);
        } catch (Exception e) {
            OpenComputers.log.warn("Failed loading loot disk descriptor.", e);
        }
        parseLootDisks(list, globalDisks, false);
    }

    private static void initForWorld(ServerLevel world) {
        if (world.dimension() != Level.OVERWORLD) return;
        worldDisks.clear();
        disksForSampling.clear();
        final File path = world.getServer().getWorldPath(LevelResource.ROOT).resolve(Settings.savePath).normalize().toFile();
        if (path.exists() && path.isDirectory()) {
            final File listFile = new File(path, "loot/loot.properties");
            if (listFile.exists() && listFile.isFile()) {
                try (InputStream listStream = new FileInputStream(listFile)) {
                    final Properties list = new Properties();
                    list.load(listStream);
                    parseLootDisks(list, worldDisks, true);
                } catch (Throwable t) {
                    OpenComputers.log.warn("Failed opening loot descriptor file in saves folder.");
                }
            }
        }
        for (Pair<ItemStack, Integer> entry : globalDisks) {
            if (!worldDisks.contains(entry)) worldDisks.add(entry);
        }
        for (Pair<ItemStack, Integer> entry : worldDisks) {
            for (int i = 0; i < entry.getRight(); i++) {
                disksForSampling.add(entry.getLeft());
            }
        }
    }

    private static void parseLootDisks(Properties list, List<Pair<ItemStack, Integer>> acc, boolean external) {
        for (String key : list.stringPropertyNames()) {
            final String value = list.getProperty(key);
            try {
                final String[] parts = value.split(":");
                if (parts.length == 3) {
                    final DyeColor color = Color.byName.get(parts[2]);
                    if (color == null) throw new IllegalArgumentException("Unknown color: " + parts[2]);
                    acc.add(Pair.of(createLootDisk(parts[0], key, external, Optional.of(color)), Integer.parseInt(parts[1])));
                } else if (parts.length == 2) {
                    acc.add(Pair.of(createLootDisk(parts[0], key, external), Integer.parseInt(parts[1])));
                } else {
                    acc.add(Pair.of(createLootDisk(value, key, external), 1));
                }
            } catch (Throwable t) {
                OpenComputers.log.warn("Bad loot descriptor: " + value, t);
            }
        }
    }

    public static ItemStack createLootDisk(String name, String path, boolean external) {
        return createLootDisk(name, path, external, Optional.empty());
    }

    public static ItemStack createLootDisk(String name, String path, boolean external, Optional<DyeColor> color) {
        final Callable<FileSystem> callable = external
                ? () -> li.cil.oc.api.FileSystem.asReadOnly(li.cil.oc.api.FileSystem.fromSaveDirectory("loot/" + path, 0, false))
                : () -> li.cil.oc.api.FileSystem.fromResource(new ResourceLocation(Settings.resourceDomain, "loot/" + path));
        final ItemStack stack = registerLootDisk(path, new ResourceLocation(Settings.resourceDomain, path), color.orElse(DyeColor.LIGHT_GRAY), callable, true);
        stack.setHoverName(Component.literal(name));
        if (!external) {
            Items.INSTANCE.registerStack(stack, path);
        }
        return stack;
    }

    private Loot() {
    }
}
