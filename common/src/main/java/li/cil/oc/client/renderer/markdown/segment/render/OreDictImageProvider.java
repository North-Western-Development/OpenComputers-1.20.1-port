package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows all items in an item (or block) tag.
 */
public final class OreDictImageProvider implements ImageProvider {
    public static final OreDictImageProvider INSTANCE = new OreDictImageProvider();

    private OreDictImageProvider() {
    }

    @Override
    public ImageRenderer getImage(String data) {
        final ResourceLocation desired = ResourceLocation.tryParse(data.toLowerCase());
        final List<ItemStack> stacks = new ArrayList<>();
        if (desired != null) {
            BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, desired)).ifPresent(tag -> {
                for (Holder<Item> holder : tag) stacks.add(new ItemStack(holder.value()));
            });
            if (stacks.isEmpty()) {
                BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, desired)).ifPresent(tag -> {
                    for (Holder<Block> holder : tag) {
                        final ItemStack stack = new ItemStack(holder.value());
                        if (!stack.isEmpty()) stacks.add(stack);
                    }
                });
            }
        }
        if (!stacks.isEmpty()) return new ItemStackImageRenderer(stacks.toArray(new ItemStack[0]));
        else return new MissingImageRenderer("oc:gui.Manual.Warning.OreDictMissing");
    }
}
