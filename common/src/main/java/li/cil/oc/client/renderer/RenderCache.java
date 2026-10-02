package li.cil.oc.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MultiBufferSource} that records everything rendered into it into
 * GPU vertex buffers, which can then be drawn repeatedly.
 * <p>
 * Used to cache the geometry of text buffers, which only changes when the
 * buffer's contents change. Must only be used from the render thread.
 */
public class RenderCache implements MultiBufferSource, AutoCloseable {
    // Shared builder used while recording; we never record two caches at once.
    private static final BufferBuilder BUILDER = new BufferBuilder(256 * 1024);

    public static class DrawEntry {
        private final RenderType type;
        private final VertexBuffer buffer;

        public DrawEntry(RenderType type, VertexBuffer buffer) {
            this.type = type;
            this.buffer = buffer;
        }

        public RenderType type() {
            return type;
        }

        public VertexBuffer buffer() {
            return buffer;
        }
    }

    private final List<DrawEntry> cached;
    private RenderType activeType;

    public RenderCache() {
        cached = new ArrayList<>();
    }

    public boolean isEmpty() {
        return cached.isEmpty();
    }

    public void clear() {
        for (DrawEntry entry : cached) {
            entry.buffer().close();
        }
        cached.clear();
    }

    @Override
    public void close() {
        if (activeType != null) {
            BUILDER.discard();
            activeType = null;
        }
        clear();
    }

    private void flush(RenderType type) {
        if (type == activeType) {
            final BufferBuilder.RenderedBuffer rendered = BUILDER.endOrDiscardIfEmpty();
            if (rendered != null) {
                final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                buffer.bind();
                buffer.upload(rendered);
                VertexBuffer.unbind();
                cached.add(new DrawEntry(type, buffer));
            }
            activeType = null;
        }
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (type == null) throw new NullPointerException(); // Same as vanilla.
        if (activeType != null) {
            if (activeType == type) return BUILDER;
            flush(activeType);
        }
        activeType = type;
        BUILDER.begin(type.mode(), type.format());
        return BUILDER;
    }

    public void finish() {
        // Flush the last active type (if any) so it gets rendered too.
        if (activeType != null) flush(activeType);
    }

    public void render(PoseStack stack) {
        RenderSystem.assertOnRenderThread();
        // Apply transform globally so we don't have to update stored vertices.
        final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.last().pose());
        final Matrix4f projection = RenderSystem.getProjectionMatrix();

        for (DrawEntry frame : cached) {
            frame.type().setupRenderState();
            final VertexBuffer buffer = frame.buffer();
            buffer.bind();
            buffer.drawWithShader(modelView, projection, RenderSystem.getShader());
            VertexBuffer.unbind();
            frame.type().clearRenderState();
        }
    }
}
