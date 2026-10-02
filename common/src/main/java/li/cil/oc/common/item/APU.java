package li.cil.oc.common.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.traits.CPULike;
import li.cil.oc.common.item.traits.GPULike;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class APU extends SimpleItem implements ItemTier, CPULike, GPULike {
    public final int tier;

    public APU(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    protected Optional<String> tooltipName() {
        return Optional.ofNullable(unlocalizedName);
    }

    @Override
    public int cpuTier() {
        return Math.min(Tier.Three, tier + 1);
    }

    @Override
    public int gpuTier() {
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        final List<Object> result = new ArrayList<>(cpuTooltipData());
        result.addAll(gpuTooltipData());
        return result;
    }

    @Override
    protected void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        cpuTooltipExtended(stack, tooltip);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        return cpuUse(stack, world, player);
    }
}
