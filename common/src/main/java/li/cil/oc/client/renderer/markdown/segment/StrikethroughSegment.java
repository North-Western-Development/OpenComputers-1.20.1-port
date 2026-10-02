package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.ChatFormatting;

public class StrikethroughSegment extends TextSegment {
    public StrikethroughSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    protected String format() {
        return ChatFormatting.STRIKETHROUGH.toString();
    }

    @Override
    public String toString(MarkupFormat format) {
        switch (format) {
            case IGWMod:
                return String.format("[prefix{m}]%s [prefix{}]", text);
            default:
                return String.format("~~%s~~", text);
        }
    }
}
