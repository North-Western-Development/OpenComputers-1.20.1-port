package li.cil.oc.integration.opencomputers;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.IMC;
import li.cil.oc.api.Items;
import li.cil.oc.api.Manual;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.internal.Wrench;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.prefab.ItemStackTabIconRenderer;
import li.cil.oc.api.prefab.ResourceContentProvider;
import li.cil.oc.api.prefab.TextureTabIconRenderer;
import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.nanomachines.provider.DisintegrationProvider;
import li.cil.oc.common.nanomachines.provider.HungryProvider;
import li.cil.oc.common.nanomachines.provider.MagnetProvider;
import li.cil.oc.common.nanomachines.provider.ParticleProvider;
import li.cil.oc.common.nanomachines.provider.PotionProvider;
import li.cil.oc.common.template.DroneTemplate;
import li.cil.oc.common.template.MicrocontrollerTemplate;
import li.cil.oc.common.template.NavigationUpgradeTemplate;
import li.cil.oc.common.template.RobotTemplate;
import li.cil.oc.common.template.ServerTemplate;
import li.cil.oc.common.template.TabletTemplate;
import li.cil.oc.common.template.TemplateBlacklist;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.util.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

public final class ModOpenComputers implements ModProxy {
    public static final ModOpenComputers INSTANCE = new ModOpenComputers();

    private ModOpenComputers() {
    }

    @Override
    public Mod getMod() {
        return Mods.OpenComputers;
    }

    @Override
    public void initialize() {
        DroneTemplate.INSTANCE.register();
        MicrocontrollerTemplate.INSTANCE.register();
        NavigationUpgradeTemplate.register();
        RobotTemplate.INSTANCE.register();
        ServerTemplate.register();
        TabletTemplate.INSTANCE.register();
        TemplateBlacklist.register();

        IMC.registerWrenchTool("li.cil.oc.integration.opencomputers.ModOpenComputers.useWrench");
        IMC.registerWrenchToolCheck("li.cil.oc.integration.opencomputers.ModOpenComputers.isWrench");
        IMC.registerItemCharge(
                "OpenComputers",
                "li.cil.oc.integration.opencomputers.ModOpenComputers.canCharge",
                "li.cil.oc.integration.opencomputers.ModOpenComputers.charge");

        IMC.registerInkProvider("li.cil.oc.integration.opencomputers.ModOpenComputers.inkCartridgeInkProvider");
        IMC.registerInkProvider("li.cil.oc.integration.opencomputers.ModOpenComputers.dyeInkProvider");

        IMC.registerProgramDiskLabel("build", "builder", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("dig", "dig", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("base64", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("deflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("gpg", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("inflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("md5sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("sha256sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("refuel", "generator", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("irc", "irc", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("maze", "maze", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("arp", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("ifconfig", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("ping", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("route", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("opl-flash", "openloader", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("oppm", "oppm", "Lua 5.2", "Lua 5.3", "LuaJ");

        // TODO(port): ForgeChunkManager.setForcedChunkLoadingCallback(OpenComputers.ID, ChunkloaderUpgradeHandler)
        //  is gone; chunk loader upgrades restore their tickets via vanilla ServerLevel.setChunkForced
        //  bookkeeping in ChunkloaderUpgradeHandler.

        // Formerly MinecraftForge.EVENT_BUS.register(...) for EventHandler, NanomachinesHandler.Common,
        // AngelUpgradeHandler, ChunkloaderUpgradeHandler, ExperienceUpgradeHandler, FileSystemAccessHandler,
        // HoverBootsHandler, Loot, NetworkActivityHandler, RobotCommonHandler, SaveHandler,
        // WirelessNetworkCardHandler and the component trackers: these are registered by
        // common.Proxy.preInit() now. The remaining former subscribers register themselves here.
        li.cil.oc.common.item.Tablet.register();
        li.cil.oc.common.item.Analyzer.register();
        li.cil.oc.server.network.Waypoints.register();
        li.cil.oc.server.network.WirelessNetwork.register();

        Driver.add(ConverterNanomachines.INSTANCE);
        Driver.add(ConverterLinkedCard.INSTANCE);
        Driver.add(DriverAPU.INSTANCE);
        Driver.add(DriverComponentBus.INSTANCE);
        Driver.add(DriverCPU.INSTANCE);
        Driver.add(DriverDataCard.INSTANCE);
        Driver.add(DriverDebugCard.INSTANCE);
        Driver.add(DriverEEPROM.INSTANCE);
        Driver.add(DriverFileSystem.INSTANCE);
        Driver.add(DriverGraphicsCard.INSTANCE);
        Driver.add(DriverInternetCard.INSTANCE);
        Driver.add(DriverLinkedCard.INSTANCE);
        Driver.add(DriverLootDisk.INSTANCE);
        Driver.add(DriverMemory.INSTANCE);
        Driver.add(DriverNetworkCard.INSTANCE);
        Driver.add(DriverKeyboard.INSTANCE);
        Driver.add(DriverRedstoneCard.INSTANCE);
        Driver.add(DriverTablet.INSTANCE);
        Driver.add(DriverWirelessNetworkCard.INSTANCE);
        Driver.add(DriverContainerCard.INSTANCE);
        Driver.add(DriverContainerFloppy.INSTANCE);
        Driver.add(DriverContainerUpgrade.INSTANCE);
        Driver.add(DriverGeolyzer.INSTANCE);
        Driver.add(DriverMotionSensor.INSTANCE);
        Driver.add(DriverScreen.INSTANCE);
        Driver.add(DriverTransposer.INSTANCE);
        Driver.add(DriverDiskDriveMountable.INSTANCE);
        Driver.add(DriverServer.INSTANCE);
        Driver.add(DriverTerminalServer.INSTANCE);
        Driver.add(DriverUpgradeAngel.INSTANCE);
        Driver.add(DriverUpgradeBarcodeReader.INSTANCE);
        Driver.add(DriverUpgradeBattery.INSTANCE);
        Driver.add(DriverUpgradeChunkloader.INSTANCE);
        Driver.add(DriverUpgradeCrafting.INSTANCE);
        Driver.add(DriverUpgradeDatabase.INSTANCE);
        Driver.add(DriverUpgradeExperience.INSTANCE);
        Driver.add(DriverUpgradeGenerator.INSTANCE);
        Driver.add(DriverUpgradeHover.INSTANCE);
        Driver.add(DriverUpgradeInventory.INSTANCE);
        Driver.add(DriverUpgradeInventoryController.INSTANCE);
        Driver.add(DriverUpgradeLeash.INSTANCE);
        Driver.add(DriverUpgradeNavigation.INSTANCE);
        Driver.add(DriverUpgradePiston.INSTANCE);
        Driver.add(DriverUpgradeSign.INSTANCE);
        Driver.add(DriverUpgradeSolarGenerator.INSTANCE);
        Driver.add(DriverUpgradeStickyPiston.INSTANCE);
        Driver.add(DriverUpgradeTank.INSTANCE);
        Driver.add(DriverUpgradeTankController.INSTANCE);
        Driver.add(DriverUpgradeTractorBeam.INSTANCE);
        Driver.add(DriverUpgradeTrading.INSTANCE);
        Driver.add(DriverUpgradeMF.INSTANCE);
        Driver.add(DriverAPU.Provider.INSTANCE);
        Driver.add(DriverDataCard.Provider.INSTANCE);
        Driver.add(DriverDebugCard.Provider.INSTANCE);
        Driver.add(DriverEEPROM.Provider.INSTANCE);
        Driver.add(DriverGraphicsCard.Provider.INSTANCE);
        Driver.add(DriverInternetCard.Provider.INSTANCE);
        Driver.add(DriverLinkedCard.Provider.INSTANCE);
        Driver.add(DriverNetworkCard.Provider.INSTANCE);
        Driver.add(DriverRedstoneCard.Provider.INSTANCE);
        Driver.add(DriverWirelessNetworkCard.Provider.INSTANCE);
        Driver.add(DriverGeolyzer.Provider.INSTANCE);
        Driver.add(DriverMotionSensor.Provider.INSTANCE);
        Driver.add(DriverScreen.Provider.INSTANCE);
        Driver.add(DriverTransposer.Provider.INSTANCE);
        Driver.add(DriverUpgradeChunkloader.Provider.INSTANCE);
        Driver.add(DriverUpgradeCrafting.Provider.INSTANCE);
        Driver.add(DriverUpgradeExperience.Provider.INSTANCE);
        Driver.add(DriverUpgradeGenerator.Provider.INSTANCE);
        Driver.add(DriverUpgradeInventoryController.Provider.INSTANCE);
        Driver.add(DriverUpgradeLeash.Provider.INSTANCE);
        Driver.add(DriverUpgradeNavigation.Provider.INSTANCE);
        Driver.add(DriverUpgradePiston.Provider.INSTANCE);
        Driver.add(DriverUpgradeSign.Provider.INSTANCE);
        Driver.add(DriverUpgradeStickyPiston.Provider.INSTANCE);
        Driver.add(DriverUpgradeTankController.Provider.INSTANCE);
        Driver.add(DriverUpgradeTractorBeam.Provider.INSTANCE);
        Driver.add(DriverUpgradeMF.Provider.INSTANCE);
        Driver.add(EnvironmentProviderBlocks.INSTANCE);
        Driver.add(InventoryProviderDatabase.INSTANCE);
        Driver.add(InventoryProviderServer.INSTANCE);


        blacklistHost(li.cil.oc.api.internal.Adapter.class,
                Constants.BlockName.Geolyzer,
                Constants.BlockName.MotionSensor,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.BatteryUpgradeTier1,
                Constants.ItemName.BatteryUpgradeTier2,
                Constants.ItemName.BatteryUpgradeTier3,
                Constants.ItemName.ChunkloaderUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.NavigationUpgrade,
                Constants.ItemName.PistonUpgrade,
                Constants.ItemName.StickyPistonUpgrade,
                Constants.ItemName.SolarGeneratorUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TractorBeamUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);

        blacklistHost(li.cil.oc.api.internal.Drone.class,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.APUTier1,
                Constants.ItemName.APUTier2,
                Constants.ItemName.GraphicsCardTier1,
                Constants.ItemName.GraphicsCardTier2,
                Constants.ItemName.GraphicsCardTier3,
                Constants.ItemName.NetworkCard,
                Constants.ItemName.RedstoneCardTier1,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2);

        blacklistHost(li.cil.oc.api.internal.Microcontroller.class,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.APUTier1,
                Constants.ItemName.APUTier2,
                Constants.ItemName.GraphicsCardTier1,
                Constants.ItemName.GraphicsCardTier2,
                Constants.ItemName.GraphicsCardTier3,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.DatabaseUpgradeTier1,
                Constants.ItemName.DatabaseUpgradeTier2,
                Constants.ItemName.DatabaseUpgradeTier3,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.InventoryControllerUpgrade,
                Constants.ItemName.NavigationUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TankControllerUpgrade,
                Constants.ItemName.TractorBeamUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);

        blacklistHost(li.cil.oc.api.internal.Robot.class,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.LeashUpgrade);

        blacklistHost(li.cil.oc.api.internal.Tablet.class,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.NetworkCard,
                Constants.ItemName.RedstoneCardTier1,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.ChunkloaderUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.DatabaseUpgradeTier1,
                Constants.ItemName.DatabaseUpgradeTier2,
                Constants.ItemName.DatabaseUpgradeTier3,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.InventoryControllerUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TankControllerUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);

        // Note: kinda nasty, but we have to check for availability for extended
        // redstone mods after integration init, so we have to set tier two
        // redstone card availability here, after all other mods were inited.
        if (BundledRedstone.isAvailable()) {
            OpenComputers.log.info("Found extended redstone mods, enabling tier two redstone card.");
            ModOpenComputers.hasRedstoneCardT2 = true;
        }

        Nanomachines.addProvider(DisintegrationProvider.INSTANCE);
        Nanomachines.addProvider(HungryProvider.INSTANCE);
        Nanomachines.addProvider(ParticleProvider.INSTANCE);
        Nanomachines.addProvider(PotionProvider.INSTANCE);
        Nanomachines.addProvider(MagnetProvider.INSTANCE);

        if (Platform.getEnvironment() == Env.CLIENT) {
            initializeClient();
        }
    }

    // Client only; only referenced when running on the physical client.
    private static void initializeClient() {
        Manual.addProvider(DefinitionPathProvider.INSTANCE);
        Manual.addProvider(new ResourceContentProvider(Settings.resourceDomain, "doc/"));
        Manual.addProvider("", li.cil.oc.client.renderer.markdown.segment.render.TextureImageProvider.INSTANCE);
        Manual.addProvider("item", li.cil.oc.client.renderer.markdown.segment.render.ItemImageProvider.INSTANCE);
        Manual.addProvider("block", li.cil.oc.client.renderer.markdown.segment.render.BlockImageProvider.INSTANCE);
        Manual.addProvider("oredict", li.cil.oc.client.renderer.markdown.segment.render.OreDictImageProvider.INSTANCE);

        Manual.addTab(new TextureTabIconRenderer(li.cil.oc.client.Textures.GUI.ManualHome), "oc:gui.Manual.Home", "%LANGUAGE%/index.md");
        Manual.addTab(new ItemStackTabIconRenderer(Items.get("case1").createItemStack(1)), "oc:gui.Manual.Blocks", "%LANGUAGE%/block/index.md");
        Manual.addTab(new ItemStackTabIconRenderer(Items.get("cpu1").createItemStack(1)), "oc:gui.Manual.Items", "%LANGUAGE%/item/index.md");
    }

    public static boolean hasRedstoneCardT2 = false;

    public static boolean useWrench(Player player, BlockPos pos, boolean changeDurability) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof Wrench wrench) {
            return wrench.useWrenchOnBlock(player, player.level(), pos, !changeDurability);
        }
        return false;
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof Wrench;
    }

    public static boolean canCharge(ItemStack stack) {
        if (stack.getItem() instanceof Chargeable chargeable) return chargeable.canCharge(stack);
        return false;
    }

    public static double charge(ItemStack stack, double amount, boolean simulate) {
        if (stack.getItem() instanceof Chargeable chargeable) return chargeable.charge(stack, amount, simulate);
        return amount;
    }

    public static int inkCartridgeInkProvider(ItemStack stack) {
        if (Items.get(stack) == Items.get(Constants.ItemName.InkCartridge))
            return Settings.get().printInkValue;
        else
            return 0;
    }

    public static int dyeInkProvider(ItemStack stack) {
        if (Color.isDye(stack))
            return Settings.get().printInkValue / 10;
        else
            return 0;
    }

    private static void blacklistHost(Class<?> host, String... itemNames) {
        for (String itemName : itemNames) {
            try {
                IMC.blacklistHost(itemName, host, Items.get(itemName).createItemStack(1));
            } catch (Throwable t) {
                OpenComputers.log.warn("Error blacklisting '" + itemName + "' for '" + host.getSimpleName() + ".", t);
            }
        }
    }

    public static final class DefinitionPathProvider implements PathProvider {
        public static final DefinitionPathProvider INSTANCE = new DefinitionPathProvider();

        private static final Set<String> Blacklist = Set.of(
                Constants.ItemName.Debugger,
                Constants.ItemName.DiamondChip,
                Constants.BlockName.Endstone
        );

        private DefinitionPathProvider() {
        }

        @Override
        public String pathFor(ItemStack stack) {
            final ItemInfo definition = Items.get(stack);
            return definition != null ? checkBlacklisted(definition) : null;
        }

        @Override
        public String pathFor(Level world, BlockPos pos) {
            if (world.getBlockState(pos).getBlock() instanceof SimpleBlock block) {
                return checkBlacklisted(Items.get(new ItemStack(block)));
            }
            return null;
        }

        private static String checkBlacklisted(ItemInfo info) {
            if (info == null || Blacklist.contains(info.name())) return null;
            else if (info.block() != null) return "%LANGUAGE%/block/" + info.name() + ".md";
            else return "%LANGUAGE%/item/" + info.name() + ".md";
        }
    }
}
