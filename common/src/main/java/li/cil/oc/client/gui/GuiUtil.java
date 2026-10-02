package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Small drawing helpers replacing the immediate mode Tessellator quads of the
 * 1.16.5 GUIs (textured quads with arbitrary, normalized UVs).
 */
public final class GuiUtil {
    private GuiUtil() {
    }

    /**
     * Draws a textured quad from (x0, y0) to (x1, y1) using normalized texture
     * coordinates, like {@code GuiGraphics#innerBlit} but with float UVs.
     */
    public static void texturedQuad(GuiGraphics graphics, ResourceLocation texture,
                                    float x0, float y0, float x1, float y1, float z,
                                    float u0, float u1, float v0, float v1) {
        if (texture == null) return;
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        final Matrix4f matrix = graphics.pose().last().pose();
        final BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, x0, y1, z).uv(u0, v1).endVertex();
        builder.vertex(matrix, x1, y1, z).uv(u1, v1).endVertex();
        builder.vertex(matrix, x1, y0, z).uv(u1, v0).endVertex();
        builder.vertex(matrix, x0, y0, z).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    /**
     * Draws the full texture stretched over the given rectangle.
     */
    public static void texturedQuad(GuiGraphics graphics, ResourceLocation texture, float x, float y, float w, float h) {
        texturedQuad(graphics, texture, x, y, x + w, y + h, 0, 0, 1, 0, 1);
    }
}
