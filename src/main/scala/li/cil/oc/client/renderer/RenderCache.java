package li.cil.oc.client.renderer
;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.BufferBuilder.DrawState;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;

public class RenderCache implements MultiBufferSource {
    public record DrawEntry(RenderType type, VertexBuffer vertexBuffer) {
    }

    private final List<DrawEntry> cached;
    private RenderType activeType;
    private BufferBuilder activeBuilder;

    public RenderCache() {
        cached = new ArrayList<>();
    }

    public boolean isEmpty() {
        return cached.isEmpty();
    }

    public void clear() {
        cached.clear();
    }

    private void flush(RenderType type) {
        if (type == activeType) {
            activeBuilder.end();

            var vertexBuffer = new VertexBuffer();
            vertexBuffer.bind();
            vertexBuffer.upload(activeBuilder);
            cached.add(new DrawEntry(type, vertexBuffer));

            activeType = null;
        }
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (type == null) throw new NullPointerException(); // Same as vanilla.
        if (activeType != null) {
            if (activeType == type) return activeBuilder;
            flush(activeType);
        }
        activeType = type;
        activeBuilder = Tesselator.getInstance().getBuilder();
        activeBuilder.clear();
        activeBuilder.begin(type.mode(), type.format());
        return activeBuilder;
    }

    public void finish() {
        // Flush the last active type (if any) so it gets rendered too.
        if (activeType != null) flush(activeType);
    }

    public void render(PoseStack stack) {
        // Apply transform globally so we don't have to update stored vertices.
        stack.pushPose();

        cached.forEach(frame -> {
            RenderSystem.bindTexture(MissingTextureAtlasSprite.getTexture().getId());
            frame.type().setupRenderState();
            var identity = new Matrix4f();
            identity.setIdentity();
            frame.vertexBuffer.drawWithShader(identity, identity, RenderSystem.getShader());
            frame.type().clearRenderState();
        });

        stack.popPose();
    }
}