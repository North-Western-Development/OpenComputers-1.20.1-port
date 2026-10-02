package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class BlockImageProvider implements ImageProvider {
    public static final BlockImageProvider INSTANCE = new BlockImageProvider();

    private BlockImageProvider() {
    }

    @Override
    public ImageRenderer getImage(String data) {
        final ResourceLocation id = ResourceLocation.tryParse(data.toLowerCase());
        final Block block = id != null && BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.get(id) : null;
        if (block != null && block.asItem() != null && block.asItem() != Items.AIR) {
            return new ItemStackImageRenderer(new ItemStack[]{new ItemStack(block)});
        }
        return new MissingImageRenderer("oc:gui.Manual.Warning.BlockMissing");
    }
}
