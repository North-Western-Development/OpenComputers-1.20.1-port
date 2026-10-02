package li.cil.oc.client;

import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Manual;
import li.cil.oc.api.prefab.ItemStackTabIconRenderer;
import li.cil.oc.api.prefab.ResourceContentProvider;
import li.cil.oc.api.prefab.TextureTabIconRenderer;
import li.cil.oc.integration.opencomputers.ModOpenComputers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only helpers called from common code (blocks, block entities, ...).
 * Only common types appear in the signatures, and callers only invoke these on
 * the client, so a dedicated server never resolves client classes.
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static boolean isLocalPlayer(Entity entity) {
        return entity instanceof Player player && player == Minecraft.getInstance().player;
    }

    // ----------------------------------------------------------------------- //
    // Screens.

    public static void showScreenGui(li.cil.oc.common.tileentity.Screen screen) {
        final li.cil.oc.common.tileentity.Screen origin = screen.origin;
        Minecraft.getInstance().setScreen(new li.cil.oc.client.gui.Screen(origin.buffer(), screen.tier > 0,
            () -> origin.hasKeyboard(), () -> origin.buffer().isRenderingEnabled()));
    }

    public static void updateMergedScreenModels(li.cil.oc.common.tileentity.Screen self) {
        final Minecraft mc = Minecraft.getInstance();
        if (self.getLevel() == mc.level) {
            final LevelRenderer renderer = mc.levelRenderer;
            for (li.cil.oc.common.tileentity.Screen screen : self.screens) {
                final BlockPos pos = screen.getBlockPos();
                renderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            }
        }
    }

    public static void closeScreenGuiFor(li.cil.oc.api.internal.TextBuffer buffer) {
        if (Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Screen screenGui && screenGui.buffer == buffer) {
            screenGui.onClose();
        }
    }

    // ----------------------------------------------------------------------- //
    // Other blocks.

    public static void showWaypointGui(li.cil.oc.common.tileentity.Waypoint t) {
        Minecraft.getInstance().setScreen(new li.cil.oc.client.gui.Waypoint(t));
    }

    public static void closeRobotGuiFor(li.cil.oc.common.tileentity.Robot robot) {
        if (Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Robot robotGui
            && robotGui.inventoryContainer.otherInventory == robot) {
            robotGui.onClose();
        }
    }

    // ----------------------------------------------------------------------- //
    // Manual (formerly ModOpenComputers.initializeClient).

    public static void registerManualContent() {
        Manual.addProvider(ModOpenComputers.DefinitionPathProvider.INSTANCE);
        Manual.addProvider(new ResourceContentProvider(Settings.resourceDomain, "doc/"));
        Manual.addProvider("", li.cil.oc.client.renderer.markdown.segment.render.TextureImageProvider.INSTANCE);
        Manual.addProvider("item", li.cil.oc.client.renderer.markdown.segment.render.ItemImageProvider.INSTANCE);
        Manual.addProvider("block", li.cil.oc.client.renderer.markdown.segment.render.BlockImageProvider.INSTANCE);
        Manual.addProvider("oredict", li.cil.oc.client.renderer.markdown.segment.render.OreDictImageProvider.INSTANCE);

        Manual.addTab(new TextureTabIconRenderer(Textures.GUI.ManualHome), "oc:gui.Manual.Home", "%LANGUAGE%/index.md");
        Manual.addTab(new ItemStackTabIconRenderer(Items.get("case1").createItemStack(1)), "oc:gui.Manual.Blocks", "%LANGUAGE%/block/index.md");
        Manual.addTab(new ItemStackTabIconRenderer(Items.get("cpu1").createItemStack(1)), "oc:gui.Manual.Items", "%LANGUAGE%/item/index.md");
    }
}
