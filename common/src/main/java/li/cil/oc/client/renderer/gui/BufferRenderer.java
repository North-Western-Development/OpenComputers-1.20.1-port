package li.cil.oc.client.renderer.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.Textures;
import li.cil.oc.util.RenderState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class BufferRenderer {
    private BufferRenderer() {
    }

    public static final int margin = 7;

    public static final int innerMargin = 1;

    public static void drawBackground(GuiGraphics graphics, int bufferWidth, int bufferHeight) {
        drawBackground(graphics, bufferWidth, bufferHeight, false);
    }

    public static void drawBackground(GuiGraphics graphics, int bufferWidth, int bufferHeight, boolean forRobot) {
        // Make sure everything batched so far is drawn below the background.
        graphics.flush();
        drawBackground(graphics.pose(), bufferWidth, bufferHeight, forRobot);
    }

    public static void drawBackground(PoseStack stack, int bufferWidth, int bufferHeight) {
        drawBackground(stack, bufferWidth, bufferHeight, false);
    }

    public static void drawBackground(PoseStack stack, int bufferWidth, int bufferHeight, boolean forRobot) {
        RenderState.checkError(BufferRenderer.class.getName() + ".drawBackground: entering (aka: wasntme)");

        final int innerWidth = innerMargin * 2 + bufferWidth;
        final int innerHeight = innerMargin * 2 + bufferHeight;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Textures.GUI.Borders);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        final BufferBuilder r = Tesselator.getInstance().getBuilder();
        r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        final int margin = forRobot ? 2 : 7;
        final int c0 = forRobot ? 5 : 0;
        final int c1 = 7;
        final int c2 = 9;
        final int c3 = forRobot ? 11 : 16;

        final Matrix4f pose = stack.last().pose();

        // Top border (left corner, middle bar, right corner).
        drawQuad(pose, r,
            0, 0, margin, margin,
            c0, c0, c1, c1);
        drawQuad(pose, r,
            margin, 0, innerWidth, margin,
            c1 + 0.25f, c0, c2 - 0.25f, c1);
        drawQuad(pose, r,
            margin + innerWidth, 0, margin, margin,
            c2, c0, c3, c1);

        // Middle area (left bar, screen background, right bar).
        drawQuad(pose, r,
            0, margin, margin, innerHeight,
            c0, c1 + 0.25f, c1, c2 - 0.25f);
        drawQuad(pose, r,
            margin, margin, innerWidth, innerHeight,
            c1 + 0.25f, c1 + 0.25f, c2 - 0.25f, c2 - 0.25f);
        drawQuad(pose, r,
            margin + innerWidth, margin, margin, innerHeight,
            c2, c1 + 0.25f, c3, c2 - 0.25f);

        // Bottom border (left corner, middle bar, right corner).
        drawQuad(pose, r,
            0, margin + innerHeight, margin, margin,
            c0, c2, c1, c3);
        drawQuad(pose, r,
            margin, margin + innerHeight, innerWidth, margin,
            c1 + 0.25f, c2, c2 - 0.25f, c3);
        drawQuad(pose, r,
            margin + innerWidth, margin + innerHeight, margin, margin,
            c2, c2, c3, c3);

        BufferUploader.drawWithShader(r.end());

        RenderState.checkError(BufferRenderer.class.getName() + ".drawBackground: leaving");
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer builder, float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
        final float u1f = u1 / 16f;
        final float u2f = u2 / 16f;
        final float v1f = v1 / 16f;
        final float v2f = v2 / 16f;
        builder.vertex(matrix, x, y + h, 0).uv(u1f, v2f).endVertex();
        builder.vertex(matrix, x + w, y + h, 0).uv(u2f, v2f).endVertex();
        builder.vertex(matrix, x + w, y, 0).uv(u2f, v1f).endVertex();
        builder.vertex(matrix, x, y, 0).uv(u1f, v1f).endVertex();
    }

    public static boolean drawText(GuiGraphics graphics, TextBuffer screen) {
        graphics.flush();
        return screen.renderText(graphics.pose());
    }

    public static boolean drawText(PoseStack stack, TextBuffer screen) {
        return screen.renderText(stack);
    }
}
