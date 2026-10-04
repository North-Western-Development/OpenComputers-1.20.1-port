package li.cil.oc.client.renderer.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.util.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Font renderer that dynamically generates lookup textures by rendering a font
 * to it. Glyphs come from the unicode hex font ({@code font.hex}).
 */
public class DynamicFontRenderer extends TextureFontRenderer implements ResourceManagerReloadListener {
    private static final int size = 256;

    private static int nextTextureId = 0;

    private final IGlyphProvider glyphProvider = new FontParserHex();

    private final List<CharTexture> textures = new ArrayList<>();

    private final Map<Integer, CharIcon> charMap = new HashMap<>();

    private CharTexture activeTexture;

    public DynamicFontRenderer() {
        initialize();

        if (Minecraft.getInstance().getResourceManager() instanceof ReloadableResourceManager reloadable) {
            reloadable.registerReloadListener(this);
        }
    }

    public void initialize() {
        for (CharTexture texture : textures) {
            texture.delete();
        }
        textures.clear();
        charMap.clear();
        glyphProvider.initialize();
        textures.add(new CharTexture(this));
        activeTexture = textures.get(0);
        generateChars(basicChars.codePoints().toArray());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        initialize();
    }

    @Override
    protected int charWidth() {
        return glyphProvider.getGlyphWidth();
    }

    @Override
    protected int charHeight() {
        return glyphProvider.getGlyphHeight();
    }

    @Override
    protected int textureCount() {
        return textures.size();
    }

    @Override
    protected RenderType selectType(int index) {
        activeTexture = textures.get(index);
        return activeTexture.getType();
    }

    @Override
    protected void generateChar(int ch) {
        if (!charMap.containsKey(ch)) {
            charMap.put(ch, createCharIcon(ch));
        }
    }

    @Override
    protected void onCharsGenerated() {
        for (CharTexture texture : textures) {
            texture.uploadIfDirty();
        }
    }

    @Override
    protected void drawChar(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty, int ch) {
        final CharIcon icon = charMap.get(ch);
        if (icon != null && icon.texture == activeTexture) {
            icon.draw(builder, matrix, color, tx, ty);
        }
    }

    private CharIcon createCharIcon(int ch) {
        if (FontUtils.wcwidth(ch) < 1 || glyphProvider.getGlyph(ch) == null) {
            if (ch == '?') return null;
            else {
                if (!charMap.containsKey((int) '?')) charMap.put((int) '?', createCharIcon('?'));
                return charMap.get((int) '?');
            }
        } else {
            CharTexture last = textures.get(textures.size() - 1);
            if (last.isFull(ch)) {
                last = new CharTexture(this);
                textures.add(last);
            }
            return last.add(ch);
        }
    }

    public static class CharTexture {
        private final DynamicFontRenderer owner;
        private final ResourceLocation location;
        private final DynamicTexture texture;
        private final RenderType rt;

        // Some padding to avoid bleeding.
        private final int cellWidth;
        private final int cellHeight;
        private final int cols;
        private final int rows;
        private final float uStep;
        private final float vStep;
        private final float pad = 1f / size;
        private final int capacity;

        private int chars = 0;
        private boolean dirty = false;

        public CharTexture(DynamicFontRenderer owner) {
            this.owner = owner;
            this.location = new ResourceLocation(OpenComputers.ID, "dynamic_font/" + (nextTextureId++));
            this.texture = new DynamicTexture(size, size, true);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            this.rt = RenderTypes.createFontTex("dyn_" + location.getPath().replace('/', '_'), location, Settings.get().textLinearFiltering);

            cellWidth = owner.charWidth() + 2;
            cellHeight = owner.charHeight() + 2;
            cols = size / cellWidth;
            rows = size / cellHeight;
            uStep = cellWidth / (float) size;
            vStep = cellHeight / (float) size;
            capacity = cols * rows;
        }

        public void delete() {
            Minecraft.getInstance().getTextureManager().release(location);
        }

        public RenderType getType() {
            return rt;
        }

        public boolean isFull(int ch) {
            return chars + FontUtils.wcwidth(ch) > capacity;
        }

        public void uploadIfDirty() {
            if (dirty) {
                dirty = false;
                texture.upload();
            }
        }

        public CharIcon add(int ch) {
            final int glyphWidth = FontUtils.wcwidth(ch);
            final int w = owner.charWidth() * glyphWidth;
            final int h = owner.charHeight();
            // Force line break if we have a char that's wider than what space remains in this row.
            if (chars % cols + glyphWidth > cols) {
                chars += 1;
            }
            final int x = chars % cols;
            final int y = chars / cols;

            final ByteBuffer glyph = owner.glyphProvider.getGlyph(ch);
            final NativeImage pixels = texture.getPixels();
            if (glyph != null && pixels != null) {
                final int ox = 1 + x * cellWidth;
                final int oy = 1 + y * cellHeight;
                for (int py = 0; py < h; py++) {
                    for (int px = 0; px < w; px++) {
                        final int r = glyph.get() & 0xFF;
                        final int g = glyph.get() & 0xFF;
                        final int b = glyph.get() & 0xFF;
                        final int a = glyph.get() & 0xFF;
                        // NativeImage stores ABGR.
                        pixels.setPixelRGBA(ox + px, oy + py, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }
                dirty = true;
            }

            chars += glyphWidth;

            return new CharIcon(this, w, h, pad + x * uStep, pad + y * vStep, (x + glyphWidth) * uStep - pad, (y + 1) * vStep - pad);
        }
    }

    public static class CharIcon {
        public final CharTexture texture;
        public final int w;
        public final int h;
        public final float u1;
        public final float v1;
        public final float u2;
        public final float v2;

        public CharIcon(CharTexture texture, int w, int h, float u1, float v1, float u2, float v2) {
            this.texture = texture;
            this.w = w;
            this.h = h;
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }

        public void draw(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty) {
            final float r = ((color >> 16) & 0xFF) / 255f;
            final float g = ((color >> 8) & 0xFF) / 255f;
            final float b = (color & 0xFF) / 255f;
            builder.vertex(matrix, tx, ty + h, 0).color(r, g, b, 1f).uv(u1, v2).endVertex();
            builder.vertex(matrix, tx + w, ty + h, 0).color(r, g, b, 1f).uv(u2, v2).endVertex();
            builder.vertex(matrix, tx + w, ty, 0).color(r, g, b, 1f).uv(u2, v1).endVertex();
            builder.vertex(matrix, tx, ty, 0).color(r, g, b, 1f).uv(u1, v1).endVertex();
        }
    }
}
