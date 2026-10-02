package li.cil.oc.client.renderer.tileentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.Settings;
import li.cil.oc.client.Textures;
import li.cil.oc.common.tileentity.Hologram;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Hologram projector renderer.
 * <p>
 * Port note: the 1.16 version used fixed function client arrays with a
 * shared static vertex buffer and per-hologram index buffers. With the core
 * profile this is now one vertex buffer per hologram containing only the
 * visible faces, rebuilt when the hologram's data changes. Rendering is
 * deferred to after the level was rendered so transparency works.
 */
public class HologramRenderer implements BlockEntityRenderer<Hologram> {
    private static final Random random = new Random();

    /**
     * Cached geometry for the projectors we render.
     */
    private static final class HologramBuffer {
        VertexBuffer buffer;
        int quads;
    }

    private static final List<HologramBuffer> released = new ArrayList<>();

    private static final Cache<Hologram, HologramBuffer> cache = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.SECONDS)
        .removalListener((RemovalNotification<Hologram, HologramBuffer> notification) -> {
            final HologramBuffer value = notification.getValue();
            if (value != null) {
                synchronized (released) {
                    released.add(value);
                }
            }
        })
        .build();

    private static final BufferBuilder builder = new BufferBuilder(256 * 1024);

    /**
     * Whether initialization failed (e.g. due to an out of memory error) and we
     * should render using the fallback renderer instead.
     */
    private static boolean failed = false;

    private static final ArrayDeque<Hologram> renderQueue = new ArrayDeque<>();

    public HologramRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Hologram hologram, float f, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        if (failed) {
            HologramRendererFallback.render(hologram, f, stack, buffer, light, overlay);
            return;
        }

        if (hologram.hasPower) renderQueue.addLast(hologram);
    }

    @Override
    public boolean shouldRenderOffScreen(Hologram hologram) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(Hologram hologram, Vec3 cameraPos) {
        final double distance = hologram.getViewDistance();
        return Vec3.atCenterOf(hologram.getBlockPos()).distanceToSqr(cameraPos) < distance * distance;
    }

    // ----------------------------------------------------------------------- //

    /**
     * Defer actual rendering until now so transparent things render correctly.
     * Registered via {@link li.cil.oc.client.platform.RenderPlatform#registerLevelRenderer}.
     */
    public static void onRenderLevelLast(PoseStack stack, float partialTicks, Camera camera) {
        RenderState.checkError(HologramRenderer.class.getName() + ".onRenderWorldLastEvent: entering (aka: wasntme)");

        releaseEvicted();

        final Vec3 camPos = camera.getPosition();

        while (!renderQueue.isEmpty()) {
            final Hologram holo = renderQueue.removeFirst();
            if (holo.isRemoved()) continue;
            final BlockPos pos = holo.getBlockPos();
            stack.pushPose();
            stack.translate(pos.getX() + 0.5 - camPos.x, pos.getY() + 0.5 - camPos.y, pos.getZ() + 0.5 - camPos.z);
            try {
                doRender(holo, partialTicks, stack);
            } finally {
                stack.popPose();
            }
        }

        RenderState.checkError(HologramRenderer.class.getName() + ".onRenderWorldLastEvent: leaving");
    }

    private static void doRender(Hologram hologram, float f, PoseStack stack) {
        final BlockPos pos = hologram.getBlockPos();
        final Vec3 relPos = Minecraft.getInstance().player.getEyePosition(f).
            subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        final double playerDistSq = relPos.dot(relPos);
        final double maxDistSq = hologram.getViewDistance() * hologram.getViewDistance();
        final double fadeDistSq = hologram.getFadeStartDistanceSquared();
        final float alpha = 0.75f * (playerDistSq > fadeDistSq ? Math.max(0, 1 - (float) ((playerDistSq - fadeDistSq) / (maxDistSq - fadeDistSq))) : 1);

        RenderUtil.rotateYaw(stack, hologram.yaw());
        final Direction pitch = hologram.pitch();
        if (pitch == Direction.DOWN) stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        else if (pitch == Direction.UP) stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));

        RenderUtil.rotate(stack, hologram.rotationAngle, hologram.rotationX, hologram.rotationY, hologram.rotationZ);
        RenderUtil.rotate(stack, hologram.rotationSpeed * (hologram.getLevel().getGameTime() % (360 * 20 - 1) + f) / 20f,
            hologram.rotationSpeedX, hologram.rotationSpeedY, hologram.rotationSpeedZ);

        stack.scale(1.001f, 1.001f, 1.001f); // Avoid z-fighting with other blocks.
        stack.translate(
            (hologram.translation.x * hologram.width / 16 - 1.5) * hologram.scale,
            hologram.translation.y * hologram.height / 16 * hologram.scale,
            (hologram.translation.z * hologram.width / 16 - 1.5) * hologram.scale);

        // Do a bit of flickering, because that's what holograms do!
        if (Settings.get().hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get().hologramFlickerFrequency) {
            stack.scale(1 + (float) random.nextGaussian() * 0.01f, 1 + (float) random.nextGaussian() * 0.001f, 1 + (float) random.nextGaussian() * 0.01f);
            stack.translate(random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, random.nextGaussian() * 0.01);
        }

        // After the below scaling, hologram is drawn inside a [0..48]x[0..32]x[0..48] box
        stack.scale((float) hologram.scale / 16f, (float) hologram.scale / 16f, (float) hologram.scale / 16f);

        final HologramBuffer glBuffer;
        try {
            glBuffer = cache.get(hologram, () -> {
                // Force re-indexing.
                hologram.needsRendering = true;
                return new HologramBuffer();
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (!validate(hologram, glBuffer) || glBuffer.buffer == null || glBuffer.quads == 0) {
            return;
        }

        RenderState.makeItBlend();
        RenderState.setBlendAlpha(alpha);

        final double sx = relPos.x * hologram.scale;
        final double sy = relPos.y * hologram.scale;
        final double sz = relPos.z * hologram.scale;
        if (sx >= -1.5 && sx <= 1.5 && sz >= -1.5 && sz <= 1.5 && sy >= 0 && sy <= 2) {
            // Camera is inside the hologram.
            RenderSystem.disableCull();
        } else {
            // Camera is outside the hologram.
            RenderSystem.enableCull();
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Textures.Model.HologramEffect);

        final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.last().pose());
        final Matrix4f projection = RenderSystem.getProjectionMatrix();

        // We do two passes here to avoid weird transparency effects: in the first
        // pass we find the front-most fragment, in the second we actually draw it.
        // When we don't do this the hologram will look different from different
        // angles (because some faces will shine through sometimes and sometimes
        // they won't), so a more... consistent look is desirable.
        RenderSystem.enableDepthTest();
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        draw(glBuffer, modelView, projection);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL11.GL_EQUAL);
        draw(glBuffer, modelView, projection);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableCull();
        RenderState.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void draw(HologramBuffer glBuffer, Matrix4f modelView, Matrix4f projection) {
        glBuffer.buffer.bind();
        glBuffer.buffer.drawWithShader(modelView, projection, RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    /**
     * Rebuilds the geometry when the hologram's data changed.
     */
    private static boolean validate(Hologram hologram, HologramBuffer glBuffer) {
        if (!hologram.needsRendering && glBuffer.buffer != null) return true;
        try {
            final int[] colors = hologram.colors();

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int quads = 0;
            for (int hx = 0; hx < hologram.width; hx++) {
                for (int hz = 0; hz < hologram.width; hz++) {
                    for (int hy = 0; hy < hologram.height; hy++) {
                        // Do we need to draw at least one face?
                        final int value = value(hologram, hx, hy, hz);
                        if (value != 0) {
                            // Yes, get the color of the voxel (0xBBGGRR).
                            final int color = colors[value - 1];
                            final int r = color & 0xFF;
                            final int g = (color >> 8) & 0xFF;
                            final int b = (color >> 16) & 0xFF;
                            final int x = hx, y = hy, z = hz;

                            /*
                                  0---1
                                  | N |
                              0---3---2---1---0
                              | W | U | E | D |
                              5---6---7---4---5
                                  | S |
                                  5---4
                             */

                            // South
                            if (!isSolid(hologram, hx, hy, hz + 1)) {
                                vertex(x + 1, y + 1, z + 1, 0, 0, r, g, b);
                                vertex(x, y + 1, z + 1, 1, 0, r, g, b);
                                vertex(x, y, z + 1, 1, 1, r, g, b);
                                vertex(x + 1, y, z + 1, 0, 1, r, g, b);
                                quads++;
                            }
                            // North
                            if (!isSolid(hologram, hx, hy, hz - 1)) {
                                vertex(x + 1, y, z, 0, 0, r, g, b);
                                vertex(x, y, z, 1, 0, r, g, b);
                                vertex(x, y + 1, z, 1, 1, r, g, b);
                                vertex(x + 1, y + 1, z, 0, 1, r, g, b);
                                quads++;
                            }

                            // East
                            if (!isSolid(hologram, hx + 1, hy, hz)) {
                                vertex(x + 1, y + 1, z + 1, 1, 0, r, g, b);
                                vertex(x + 1, y, z + 1, 1, 1, r, g, b);
                                vertex(x + 1, y, z, 0, 1, r, g, b);
                                vertex(x + 1, y + 1, z, 0, 0, r, g, b);
                                quads++;
                            }
                            // West
                            if (!isSolid(hologram, hx - 1, hy, hz)) {
                                vertex(x, y, z + 1, 1, 0, r, g, b);
                                vertex(x, y + 1, z + 1, 1, 1, r, g, b);
                                vertex(x, y + 1, z, 0, 1, r, g, b);
                                vertex(x, y, z, 0, 0, r, g, b);
                                quads++;
                            }

                            // Up
                            if (!isSolid(hologram, hx, hy + 1, hz)) {
                                vertex(x + 1, y + 1, z, 0, 0, r, g, b);
                                vertex(x, y + 1, z, 1, 0, r, g, b);
                                vertex(x, y + 1, z + 1, 1, 1, r, g, b);
                                vertex(x + 1, y + 1, z + 1, 0, 1, r, g, b);
                                quads++;
                            }
                            // Down
                            if (!isSolid(hologram, hx, hy - 1, hz)) {
                                vertex(x + 1, y, z + 1, 0, 0, r, g, b);
                                vertex(x, y, z + 1, 1, 0, r, g, b);
                                vertex(x, y, z, 1, 1, r, g, b);
                                vertex(x + 1, y, z, 0, 1, r, g, b);
                                quads++;
                            }
                        }
                    }
                }
            }

            final BufferBuilder.RenderedBuffer rendered = builder.endOrDiscardIfEmpty();
            hologram.visibleQuads = quads;
            glBuffer.quads = quads;
            if (rendered != null) {
                if (glBuffer.buffer == null) {
                    glBuffer.buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                }
                glBuffer.buffer.bind();
                glBuffer.buffer.upload(rendered);
                VertexBuffer.unbind();
            }

            hologram.needsRendering = false;
            return true;
        } catch (OutOfMemoryError oom) {
            if (builder.building()) builder.discard();
            HologramRendererFallback.text = "Not enough memory";
            failed = true;
            return false;
        }
    }

    private static void vertex(int x, int y, int z, int u, int v, int r, int g, int b) {
        builder.vertex(x, y, z).uv(u, v).color(r, g, b, 255).endVertex();
    }

    private static int value(Hologram hologram, int hx, int hy, int hz) {
        if (hx >= 0 && hy >= 0 && hz >= 0 && hx < hologram.width && hy < hologram.height && hz < hologram.width) {
            return hologram.getColor(hx, hy, hz);
        }
        return 0;
    }

    private static boolean isSolid(Hologram hologram, int hx, int hy, int hz) {
        return value(hologram, hx, hy, hz) != 0;
    }

    // ----------------------------------------------------------------------- //
    // Cache
    // ----------------------------------------------------------------------- //

    private static void releaseEvicted() {
        synchronized (released) {
            for (HologramBuffer value : released) {
                if (value.buffer != null) value.buffer.close();
                value.buffer = null;
            }
            released.clear();
        }
    }

    /**
     * Called every client tick (registered in {@link li.cil.oc.client.renderer.ClientRenderers}).
     */
    public static void onTick() {
        cache.cleanUp();
    }
}
