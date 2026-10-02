package li.cil.oc.client;

import li.cil.oc.Localization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client part of {@link li.cil.oc.util.Tooltip} (font based wrapping and the
 * extended tooltip key binding).
 */
public final class TooltipClient {
    private TooltipClient() {
    }

    private static final int maxWidth = 220;

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static List<String> get(String name, String tooltip) {
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

    public static List<String> wrapAll(String tooltip) {
        final Font font = font();
        if (font == null) return tooltip.lines().toList();
        return wrapAll(font, tooltip);
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
