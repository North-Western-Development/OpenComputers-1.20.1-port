package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.FileSystemLike;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class HardDiskDrive extends SimpleItem implements ItemTier, FileSystemLike {
    public final int tier;

    public final int kiloBytes;

    public final int platterCount;

    public HardDiskDrive(Properties props, int tier) {
        super(props);
        this.tier = tier;
        this.kiloBytes = Settings.get().hddSizes[tier];
        this.platterCount = Settings.get().hddPlatterCounts[tier];
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    public int kiloBytes() {
        return kiloBytes;
    }

    @Override
    public Component getName(ItemStack stack) {
        final MutableComponent localizedName = super.getName(stack).copy();
        if (kiloBytes >= 1024) {
            localizedName.append(" (" + (kiloBytes / 1024) + "MB)");
        } else {
            localizedName.append(" (" + kiloBytes + "KB)");
        }
        return localizedName;
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
