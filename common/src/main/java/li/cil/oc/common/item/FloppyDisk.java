package li.cil.oc.common.item;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.FileSystemLike;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FloppyDisk extends SimpleItem implements CustomModel, FileSystemLike {
    public final int kiloBytes = Settings.get().floppySize;

    public FloppyDisk(Properties props) {
        super(props);
        // Necessary for anonymous subclasses used for loot disks.
        unlocalizedName = "floppydisk";
    }

    @Override
    public int kiloBytes() {
        return kiloBytes;
    }

    private static ResourceLocation modelLocationFromDyeName(DyeColor dye) {
        return new ResourceLocation(Settings.resourceDomain, "item/" + Constants.ItemName.Floppy + "_" + dye.getName());
    }

    @Override
    public ResourceLocation getModelLocation(ItemStack stack) {
        final int dyeIndex =
            stack.hasTag() && stack.getTag().contains(Settings.namespace + "color")
                ? stack.getTag().getInt(Settings.namespace + "color")
                : DyeColor.GRAY.getId();
        return modelLocationFromDyeName(DyeColor.byId(Math.min(Math.max(dyeIndex, 0), 15)));
    }

    @Override
    public List<ResourceLocation> modelLocations() {
        final List<ResourceLocation> result = new ArrayList<>();
        for (DyeColor dye : DyeColor.values()) {
            result.add(modelLocationFromDyeName(dye));
        }
        return result;
    }

    @Override
    public boolean sneakBypassesUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    // ----------------------------------------------------------------------- //
    // traits.FileSystemLike

    @Override
    protected Optional<String> tooltipName() {
        return Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        fsAppendHoverText(stack, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        return fsUse(stack, world, player);
    }
}
