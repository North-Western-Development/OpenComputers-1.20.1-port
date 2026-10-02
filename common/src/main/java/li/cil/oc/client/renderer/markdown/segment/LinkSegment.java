package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.Manual;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.util.Optional;

public class LinkSegment extends TextSegment implements InteractiveSegment {
    private static final int normalColor = 0x66FF66;
    private static final int normalHoverColor = 0xAAFFAA;
    private static final int errorColor = 0xFF6666;
    private static final int errorHoverColor = 0xFFAAAA;
    private static final int fadeTime = 500;

    public final String url;

    private Boolean isLinkValid = null;

    private long lastHovered = System.currentTimeMillis() - fadeTime;

    public LinkSegment(Segment parent, String text, String url) {
        super(parent, text);
        this.url = url;
    }

    private boolean isLinkValid() {
        if (isLinkValid == null) {
            isLinkValid = (url.startsWith("http://") || url.startsWith("https://")) ||
                li.cil.oc.api.Manual.contentFor(Manual.makeRelative(url, Manual.history.peek().path)) != null;
        }
        return isLinkValid;
    }

    @Override
    protected Optional<Integer> color() {
        final boolean valid = isLinkValid();
        final int color = valid ? normalColor : errorColor;
        final int hoverColor = valid ? normalHoverColor : errorHoverColor;
        final int timeSinceHover = (int) (System.currentTimeMillis() - lastHovered);
        if (timeSinceHover > fadeTime) return Optional.of(color);
        else return Optional.of(fadeColor(hoverColor, color, timeSinceHover / (float) fadeTime));
    }

    @Override
    public Optional<String> tooltip() {
        return Optional.ofNullable(url);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY) {
        if (url.startsWith("http://") || url.startsWith("https://")) handleUrl(url);
        else Manual.INSTANCE.navigate(Manual.makeRelative(url, Manual.history.peek().path));
        return true;
    }

    @Override
    public void notifyHover() {
        lastHovered = System.currentTimeMillis();
    }

    private static int fadeColor(int c1, int c2, float t) {
        final int r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        final int r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        final int r = (int) (r1 + (r2 - r1) * t), g = (int) (g1 + (g2 - g1) * t), b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void handleUrl(String url) {
        try {
            Util.getPlatform().openUri(new URI(url));
        } catch (Throwable t) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Localization.Chat.WarningLink(t.toString()));
            }
        }
    }

    @Override
    public String toString(MarkupFormat format) {
        switch (format) {
            case IGWMod:
                if (url.startsWith("http://") || url.startsWith("https://")) return text;
                else return "[link{" + OpenComputers.ID + ":" + url + "}]" + text + " [link{}]";
            default:
                return "[" + text + "](" + url + ")";
        }
    }
}
