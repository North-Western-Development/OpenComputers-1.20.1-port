package li.cil.oc.client.renderer.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Font renderer using a user specified texture file, meaning the list of
 * supported characters is fixed. But at least this one works.
 */
public class StaticFontRenderer extends TextureFontRenderer {
    protected final String chars;
    protected final int charWidth;
    protected final int charHeight;

    private final int cols;
    private final float uStep;
    private final float uSize;
    private final float vStep;
    private final float vSize;
    private final float s = (float) Settings.get().fontCharScale;
    private final float dw;
    private final float dh;

    private final RenderType renderType;

    public StaticFontRenderer() {
        String chars;
        int w, h;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager()
            .getResourceOrThrow(new ResourceLocation(Settings.resourceDomain, "textures/font/chars.txt")).open(), StandardCharsets.UTF_8))) {
            chars = reader.readLine();
            if (chars == null) throw new IllegalStateException("Empty font metadata.");
            final String size = reader.readLine();
            if (size != null) {
                final String[] parts = size.split(" ", 2);
                w = Integer.parseInt(parts[0].trim());
                h = Integer.parseInt(parts[1].trim());
            } else {
                w = 10;
                h = 18;
            }
        } catch (Throwable t) {
            OpenComputers.log.warn("Failed reading font metadata, using defaults.", t);
            chars = basicChars;
            w = 10;
            h = 18;
        }
        this.chars = chars;
        this.charWidth = w;
        this.charHeight = h;

        cols = 256 / charWidth;
        uStep = charWidth / 256f;
        uSize = uStep;
        vStep = (charHeight + 1) / 256f;
        vSize = charHeight / 256f;
        dw = charWidth * s - charWidth;
        dh = charHeight * s - charHeight;

        if (Settings.get().textAntiAlias) {
            renderType = RenderTypes.createFontTex("smoothed", Textures.Font.AntiAliased, Settings.get().textLinearFiltering);
        } else {
            renderType = RenderTypes.createFontTex("aliased", Textures.Font.Aliased, Settings.get().textLinearFiltering);
        }
    }

    @Override
    protected int charWidth() {
        return charWidth;
    }

    @Override
    protected int charHeight() {
        return charHeight;
    }

    @Override
    protected int textureCount() {
        return 1;
    }

    @Override
    protected RenderType selectType(int index) {
        return renderType;
    }

    @Override
    protected void drawChar(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty, int ch) {
        int found = chars.indexOf(ch);
        if (found == -1) found = chars.indexOf('?');
        final int index = 1 + found;
        final int x = (index - 1) % cols;
        final int y = (index - 1) / cols;
        final float u = x * uStep;
        final float v = y * vStep;
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;
        builder.vertex(matrix, tx - dw, ty + charHeight * s, 0).color(r, g, b, 1f).uv(u, v + vSize).endVertex();
        builder.vertex(matrix, tx + charWidth * s, ty + charHeight * s, 0).color(r, g, b, 1f).uv(u + uSize, v + vSize).endVertex();
        builder.vertex(matrix, tx + charWidth * s, ty - dh, 0).color(r, g, b, 1f).uv(u + uSize, v).endVertex();
        builder.vertex(matrix, tx - dw, ty - dh, 0).color(r, g, b, 1f).uv(u, v).endVertex();
    }

    @Override
    protected void generateChar(int ch) {
    }
}
