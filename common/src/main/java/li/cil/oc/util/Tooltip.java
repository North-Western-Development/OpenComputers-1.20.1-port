package li.cil.oc.util;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.client.KeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client only.
 */
public final class Tooltip {
    private Tooltip() {
    }

    private static final int maxWidth = 220;

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static final Style DefaultStyle = Style.EMPTY.applyFormat(ChatFormatting.GRAY);

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
        final Font font = font();
        if (font == null) return tooltip.lines().toList(); // Some mods request tooltips before font renderer is available.
        final boolean isSubTooltip = name.contains(".");
        final boolean shouldShorten = (isSubTooltip || font.width(tooltip) > maxWidth) && !KeyBindings.showExtendedTooltips();
        if (shouldShorten) {
            if (isSubTooltip) return Collections.emptyList();
            else return Collections.singletonList(Localization.localizeImmediately("tooltip.toolong", KeyBindings.getKeyBindingName(KeyBindings.extendedTooltip)));
        } else {
            return wrapAll(font, tooltip);
        }
    }

    public static List<String> extended(String name, Object... args) {
        if (KeyBindings.showExtendedTooltips()) {
            final String tooltip = String.format(Localization.localizeImmediately("tooltip." + name), stringify(args));
            return wrapAll(font(), tooltip);
        } else return Collections.emptyList();
    }

    private static List<String> wrapAll(Font font, String tooltip) {
        final List<String> result = new ArrayList<>();
        tooltip.lines().forEach(line -> {
            for (String part : wrap(font, line, maxWidth)) {
                result.add(part.trim() + " ");
            }
        });
        return result;
    }

    private static List<String> wrap(Font font, String line, int width) {
        final List<String> list = new ArrayList<>();
        font.getSplitter().splitLines(line, width, Style.EMPTY, true, (style, start, end) -> list.add(line.substring(start, end)));
        return list;
    }
}
