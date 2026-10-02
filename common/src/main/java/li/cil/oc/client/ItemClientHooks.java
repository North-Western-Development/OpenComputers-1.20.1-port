package li.cil.oc.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.component.TerminalServer;
import li.cil.oc.common.item.Tablet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/**
 * Client-only helpers used by item classes in {@code li.cil.oc.common.item} (GUIs opened
 * from items, texture picker, tablet client events). Item code only calls these on the
 * logical client, so this class is never loaded on a dedicated server.
 */
public final class ItemClientHooks {
    private ItemClientHooks() {
    }

    /** traits.FileSystemLike: open the drive mode GUI for a held HDD / floppy. */
    public static void openDriveGui(Player player, ItemStack stack) {
        Minecraft.getInstance().setScreen(new li.cil.oc.client.gui.Drive(player.getInventory(), () -> stack));
    }

    /** item.TexturePicker: print the particle texture name of the block at pos. */
    public static void showTextureName(Player player, BlockPos pos) {
        final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(player.level().getBlockState(pos));
        // TODO(port): Forge passed ModelData to getParticleTexture; vanilla getParticleIcon() has none.
        final TextureAtlasSprite particle = model != null ? model.getParticleIcon() : null;
        if (particle != null && particle.contents().name() != null) {
            player.sendSystemMessage(Localization.Chat.TextureName(particle.contents().name().toString()));
        }
    }

    /** item.Terminal: open the remote screen of a terminal server. */
    public static void openTerminalScreen(ItemStack stack, String key, TerminalServer term, BooleanSupplier inRange) {
        Minecraft.getInstance().setScreen(new li.cil.oc.client.gui.Screen(term.buffer(), true, () -> true, () -> {
            // Check if someone else bound a term to our server.
            if (!stack.getTag().getString(Settings.namespace + "key").equals(key)) Minecraft.getInstance().setScreen(null);
            // Check whether we're still in range.
            if (!inRange.getAsBoolean()) Minecraft.getInstance().setScreen(null);
            return true;
        }));
    }

    /** item.Tablet: open the tablet's screen. */
    public static void openTabletScreen(TextBuffer buffer) {
        Minecraft.getInstance().setScreen(new li.cil.oc.client.gui.Screen(buffer, true, () -> true, buffer::isRenderingEnabled));
    }

    private static boolean tabletEventsRegistered;

    /** Client half of the former Tablet Forge subscriptions (client tick, world unload). */
    public static synchronized void registerTabletClientEvents() {
        if (tabletEventsRegistered) return;
        tabletEventsRegistered = true;
        ClientTickEvent.CLIENT_POST.register(mc -> Tablet.onClientTick(mc.hasSingleplayerServer() && mc.isPaused()));
        // There is no client level unload event; drop tablets of other levels when a new one loads,
        // and everything when leaving the game.
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> Tablet.Client.INSTANCE.clearAllExcept(level));
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> Tablet.Client.INSTANCE.clearAllExcept(null));
    }
}
