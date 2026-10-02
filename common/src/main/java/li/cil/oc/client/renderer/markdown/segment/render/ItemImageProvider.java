package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemImageProvider implements ImageProvider {
    public static final ItemImageProvider INSTANCE = new ItemImageProvider();

    private ItemImageProvider() {
    }

    @Override
    public ImageRenderer getImage(String data) {
        final ResourceLocation id = ResourceLocation.tryParse(data.toLowerCase());
        final Item item = id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
        if (item != null) {
            return new ItemStackImageRenderer(new ItemStack[]{new ItemStack(item)});
        }
        return new MissingImageRenderer("oc:gui.Manual.Warning.ItemMissing");
    }
}
