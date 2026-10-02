package li.cil.oc.common.item;

import java.util.List;
import java.util.Optional;
import li.cil.oc.common.item.traits.CPULike;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CPU extends SimpleItem implements ItemTier, CPULike {
    public final int tier;

    public CPU(Properties props, int tier) {
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
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        return cpuTooltipData();
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
