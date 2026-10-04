package li.cil.oc.client.renderer.font;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.TextBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Base class for texture based font rendering.
 * <p>
 * Provides common logic for the static one (using an existing texture) and the
 * dynamic one (generating textures on the fly from a font).
 */
public abstract class TextureFontRenderer {
    protected static final String basicChars = "☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■";

    public int charRenderWidth() {
        return charWidth() / 2;
    }

    public int charRenderHeight() {
        return charHeight() / 2;
    }

    /**
     * Should be called before rendering, so that no characters have to be
     * generated while recording geometry.
     */
    public void generateChars(int[] chars) {
        for (int ch : chars) {
            generateChar(ch);
        }
        onCharsGenerated();
    }

    public void drawBuffer(PoseStack stack, MultiBufferSource renderBuff, TextBuffer buffer, int viewportWidth, int viewportHeight) {
        final PackedColor.ColorFormat format = buffer.format();

        stack.pushPose();

        stack.scale(0.5f, 0.5f, 1);

        final Matrix4f matrix = stack.last().pose();
        final int lines = Math.min(viewportHeight, buffer.height);

        // Background first. We try to merge adjacent backgrounds of the same
        // color to reduce the number of quads we have to draw.
        VertexConsumer quadBuilder = null;
        for (int y = 0; y < lines; y++) {
            final short[] color = buffer.color[y];
            int cbg = 0x000000;
            int x = 0;
            int width = 0;
            for (short packed : color) {
                if (x + width >= viewportWidth) break;
                final int col = PackedColor.unpackBackground(packed, format);
                if (col != cbg) {
                    if (quadBuilder == null) quadBuilder = renderBuff.getBuffer(RenderTypes.FONT_QUAD);
                    drawQuad(quadBuilder, matrix, cbg, x, y, width);
                    cbg = col;
                    x += width;
                    width = 0;
                }
                width = width + 1;
            }
            if (quadBuilder != null) drawQuad(quadBuilder, matrix, cbg, x, y, width);
        }

        // Foreground second. We only have to flush when the texture changes.
        for (int i = 0; i < textureCount(); i++) {
            final VertexConsumer fontBuilder = renderBuff.getBuffer(selectType(i));
            for (int y = 0; y < lines; y++) {
                final int[] line = buffer.buffer[y];
                final short[] color = buffer.color[y];
                final float ty = y * charHeight();
                float tx = 0f;
                for (int n = 0; n < viewportWidth && n < line.length; n++) {
                    final int ch = line[n];
                    // Don't render whitespace.
                    if (ch != ' ') {
                        final int col = PackedColor.unpackForeground(color[n], format);
                        drawChar(fontBuilder, matrix, col, tx, ty, ch);
                    }
                    tx += charWidth();
                }
            }
        }

        stack.popPose();
    }

    public void drawString(PoseStack stack, String s, int x, int y) {
        drawString(stack, s, x, y, 0xFFFFFF);
    }

    public void drawString(PoseStack stack, String s, int x, int y, int color) {
        final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        drawString(stack, buffers, s, x, y, color);
        buffers.endBatch();
    }

    public void drawString(PoseStack stack, MultiBufferSource buffers, String s, int x, int y, int color) {
        generateChars(s.codePoints().toArray());

        stack.pushPose();

        stack.translate(x, y, 0);
        stack.scale(0.5f, 0.5f, 1);

        final Matrix4f matrix = stack.last().pose();
        for (int i = 0; i < textureCount(); i++) {
            final VertexConsumer builder = buffers.getBuffer(selectType(i));
            float tx = 0f;
            for (int n = 0; n < s.length(); n = s.offsetByCodePoints(n, 1)) {
                final int ch = s.codePointAt(n);
                // Don't render whitespace.
                if (ch != ' ') {
                    drawChar(builder, matrix, color, tx, 0, ch);
                }
                tx += charWidth();
            }
        }

        stack.popPose();
    }

    protected abstract int charWidth();

    protected abstract int charHeight();

    protected abstract int textureCount();

    protected abstract RenderType selectType(int index);

    protected abstract void generateChar(int ch);

    /**
     * Called after a batch of characters was generated, e.g. to upload
     * modified textures.
     */
    protected void onCharsGenerated() {
    }

    protected abstract void drawChar(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty, int ch);

    private void drawQuad(VertexConsumer builder, Matrix4f matrix, int color, int x, int y, int width) {
        if (color != 0 && width > 0) {
            final int x0 = x * charWidth();
            final int x1 = (x + width) * charWidth();
            final int y0 = y * charHeight();
            final int y1 = (y + 1) * charHeight();
            final float r = ((color >> 16) & 0xFF) / 255f;
            final float g = ((color >> 8) & 0xFF) / 255f;
            final float b = (color & 0xFF) / 255f;
            builder.vertex(matrix, x0, y1, 0).color(r, g, b, 1f).endVertex();
            builder.vertex(matrix, x1, y1, 0).color(r, g, b, 1f).endVertex();
            builder.vertex(matrix, x1, y0, 0).color(r, g, b, 1f).endVertex();
            builder.vertex(matrix, x0, y0, 0).color(r, g, b, 1f).endVertex();
        }
    }
}
