package li.cil.oc.api.prefab;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

/**
 * Simple implementation of a tab icon renderer using a full texture as its graphic.
 */
@SuppressWarnings("UnusedDeclaration")
public class TextureTabIconRenderer implements TabIconRenderer {
    private final ResourceLocation location;

    public TextureTabIconRenderer(ResourceLocation location) {
        this.location = location;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack stack) {
        RenderSystem.setShaderTexture(0, location);
        final Tesselator t = Tesselator.getInstance();
        final BufferBuilder r = t.getBuilder();
        r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        r.vertex(stack.last().pose(), 0, 16, 0).uv(0, 1).endVertex();
        r.vertex(stack.last().pose(), 16, 16, 0).uv(1, 1).endVertex();
        r.vertex(stack.last().pose(), 16, 0, 0).uv(1, 0).endVertex();
        r.vertex(stack.last().pose(), 0, 0, 0).uv(0, 0).endVertex();
        t.end();
    }
}
