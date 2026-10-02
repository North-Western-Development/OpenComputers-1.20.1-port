package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.ChatFormatting;

import java.util.Optional;

public class HeaderSegment extends TextSegment {
    public final int level;
    private final float fontScale;

    public HeaderSegment(Segment parent, String text, int level) {
        super(parent, text);
        this.level = level;
        this.fontScale = Math.max(2, 5 - level) / 2f;
    }

    @Override
    protected Optional<Float> scale() {
        return Optional.of(fontScale);
    }

    @Override
    protected String format() {
        return ChatFormatting.UNDERLINE.toString();
    }

    @Override
    public String toString(MarkupFormat format) {
        switch (format) {
            case IGWMod:
                return "[prefix{l}]" + text + " [prefix{}]";
            default:
                return "#".repeat(level) + " " + text;
        }
    }
}
