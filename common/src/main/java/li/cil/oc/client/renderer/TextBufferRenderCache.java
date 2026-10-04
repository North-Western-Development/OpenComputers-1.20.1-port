package li.cil.oc.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.Settings;
import li.cil.oc.client.renderer.font.DynamicFontRenderer;
import li.cil.oc.client.renderer.font.StaticFontRenderer;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import li.cil.oc.client.renderer.font.TextureFontRenderer;
import li.cil.oc.util.RenderState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class TextBufferRenderCache {
    private TextBufferRenderCache() {
    }

    public static final TextureFontRenderer renderer =
        "texture".equals(Settings.get().fontRenderer) ? new StaticFontRenderer() : new DynamicFontRenderer();

    // Evicted caches hold GPU buffers which must be freed on the render thread.
    private static final List<RenderCache> released = new ArrayList<>();

    private static final Cache<TextBufferRenderData, RenderCache> cache = CacheBuilder.newBuilder()
        .expireAfterAccess(2, TimeUnit.SECONDS)
        .removalListener((RemovalNotification<TextBufferRenderData, RenderCache> notification) -> {
            final RenderCache value = notification.getValue();
            if (value != null) {
                synchronized (released) {
                    released.add(value);
                }
            }
        })
        .build();

    // ----------------------------------------------------------------------- //
    // Rendering
    // ----------------------------------------------------------------------- //

    public static void render(PoseStack stack, TextBufferRenderData buffer) {
        RenderState.checkError(TextBufferRenderCache.class.getName() + ".render: entering (aka: wasntme)");

        releaseEvicted();

        final RenderCache cached;
        try {
            cached = cache.get(buffer, RenderCache::new);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (buffer.dirty() || cached.isEmpty()) {
            for (int[] line : buffer.data().buffer) {
                renderer.generateChars(line);
            }

            buffer.setDirty(false);

            cached.clear();
            renderer.drawBuffer(new PoseStack(), cached, buffer.data(), buffer.viewport().getLeft(), buffer.viewport().getRight());
            cached.finish();

            RenderState.checkError(TextBufferRenderCache.class.getName() + ".render: compiled buffer");
        }

        cached.render(stack);

        RenderState.checkError(TextBufferRenderCache.class.getName() + ".render: leaving");
    }

    private static void releaseEvicted() {
        if (!RenderSystem.isOnRenderThread()) return;
        synchronized (released) {
            for (RenderCache value : released) {
                value.close();
            }
            released.clear();
        }
    }

    // ----------------------------------------------------------------------- //
    // Tick handling
    // ----------------------------------------------------------------------- //

    /**
     * Called every client tick (registered in {@link ClientRenderers#register()}).
     */
    public static void onTick() {
        cache.cleanUp();
        releaseEvicted();
    }
}
