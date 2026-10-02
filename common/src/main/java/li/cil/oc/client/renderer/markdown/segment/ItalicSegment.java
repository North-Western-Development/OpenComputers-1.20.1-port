package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.ChatFormatting;

public class ItalicSegment extends TextSegment {
    public ItalicSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    protected String format() {
        return ChatFormatting.ITALIC.toString();
    }

    @Override
    public String toString(MarkupFormat format) {
        switch (format) {
            case IGWMod:
                return String.format("[prefix{o}]%s [prefix{}]", text);
            default:
                return String.format("*%s*", text);
        }
    }
}
