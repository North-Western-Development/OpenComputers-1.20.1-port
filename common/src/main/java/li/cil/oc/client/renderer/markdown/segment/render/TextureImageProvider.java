package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.client.Textures;
import net.minecraft.resources.ResourceLocation;

public final class TextureImageProvider implements ImageProvider {
    public static final TextureImageProvider INSTANCE = new TextureImageProvider();

    // Textures.GUI locations are full texture paths already.
    public static final ResourceLocation ManualMissingItem = Textures.GUI.ManualMissingItem;

    private TextureImageProvider() {
    }

    @Override
    public ImageRenderer getImage(String data) {
        try {
            return new TextureImageRenderer(new ResourceLocation(data.toLowerCase()));
        } catch (Throwable t) {
            return new MissingImageRenderer("oc:gui.Manual.Warning.ImageMissing");
        }
    }
}
