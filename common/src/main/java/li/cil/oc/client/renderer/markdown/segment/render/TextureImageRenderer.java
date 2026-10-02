package li.cil.oc.client.renderer.markdown.segment.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

public class TextureImageRenderer implements ImageRenderer {
    public final ResourceLocation location;

    private final ImageTexture texture;

    public TextureImageRenderer(ResourceLocation location) {
        this.location = location;
        final TextureManager manager = Minecraft.getInstance().getTextureManager();
        final AbstractTexture existing = manager.getTexture(location, null);
        if (existing instanceof ImageTexture image) {
            texture = image;
        } else {
            final ImageTexture image = new ImageTexture(location);
            manager.register(location, image);
            // Loading failures are swallowed by the texture manager (it falls
            // back to the missing texture), so check whether it worked.
            if (manager.getTexture(location, null) != image || image.width <= 0 || image.height <= 0) {
                throw new IllegalArgumentException("Failed loading texture " + location);
            }
            texture = image;
        }
    }

    @Override
    public int getWidth() {
        return texture.width;
    }

    @Override
    public int getHeight() {
        return texture.height;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(location, 0, 0, 0f, 0f, texture.width, texture.height, texture.width, texture.height);
    }

    private static class ImageTexture extends AbstractTexture {
        private final ResourceLocation location;
        int width = 0;
        int height = 0;

        ImageTexture(ResourceLocation location) {
            this.location = location;
        }

        @Override
        public void load(ResourceManager manager) throws IOException {
            final NativeImage image;
            try (InputStream is = manager.open(location)) {
                image = NativeImage.read(is);
            }
            width = image.getWidth();
            height = image.getHeight();
            if (!RenderSystem.isOnRenderThreadOrInit()) {
                RenderSystem.recordRenderCall(() -> upload(image));
            } else {
                upload(image);
            }
        }

        private void upload(NativeImage image) {
            TextureUtil.prepareImage(getId(), 0, image.getWidth(), image.getHeight());
            image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), false, false, false, true);
        }
    }
}
