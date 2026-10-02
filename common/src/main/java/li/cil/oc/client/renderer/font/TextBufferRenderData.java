package li.cil.oc.client.renderer.font;

import li.cil.oc.util.TextBuffer;
import org.apache.commons.lang3.tuple.Pair;

public interface TextBufferRenderData {
    boolean dirty();

    void setDirty(boolean value);

    TextBuffer data();

    Pair<Integer, Integer> viewport();
}
