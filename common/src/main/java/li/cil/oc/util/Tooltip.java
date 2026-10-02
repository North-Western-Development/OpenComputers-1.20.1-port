package li.cil.oc.util;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

import java.util.Collections;
import java.util.List;

/**
 * Tooltip helpers. Formatting (wrapping, the extended tooltip key) happens on
 * the client via {@link li.cil.oc.client.TooltipClient}; on a dedicated server
 * (where tooltips are normally never built) plain lines are returned.
 */
public final class Tooltip {
    private Tooltip() {
    }

    public static final Style DefaultStyle = Style.EMPTY.applyFormat(ChatFormatting.GRAY);

    private static boolean isClient() {
        return Platform.getEnvironment() == Env.CLIENT;
    }

    /** Whether the extended tooltip key is held; always false on a dedicated server. */
    public static boolean showExtended() {
        return isClient() && li.cil.oc.client.KeyBindings.showExtendedTooltips();
    }

    private static Object[] stringify(Object[] args) {
        final Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = String.valueOf(args[i]);
        }
        return result;
    }

    public static List<String> get(String name, Object... args) {
        if (!Localization.canLocalize(Settings.namespace + "tooltip." + name)) return Collections.emptyList();
        final String tooltip = String.format(Localization.localizeImmediately("tooltip." + name), stringify(args));
        if (!isClient()) return tooltip.lines().toList();
        return li.cil.oc.client.TooltipClient.get(name, tooltip);
    }

    public static List<String> extended(String name, Object... args) {
        if (showExtended()) {
            final String tooltip = String.format(Localization.localizeImmediately("tooltip." + name), stringify(args));
            return li.cil.oc.client.TooltipClient.wrapAll(tooltip);
        } else return Collections.emptyList();
    }
}
