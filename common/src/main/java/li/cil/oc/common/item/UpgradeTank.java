package li.cil.oc.common.item;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class UpgradeTank extends SimpleItem implements ItemTier {
    public UpgradeTank(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        if (stack.hasTag()) {
            // TODO(port): the fluid NBT format is now Architectury's (platform dependent); it must match
            //  whatever the tank upgrade component writes.
            final FluidStack fluid = FluidStack.read(stack.getTag().getCompound(Settings.namespace + "data"));
            if (fluid != null && !fluid.isEmpty()) {
                final long amountMb = fluid.getAmount() * 1000 / FluidStack.bucketAmount();
                tooltip.add(Component.literal(fluid.getName().getString() + ": " + amountMb + "/16000").setStyle(Tooltip.DefaultStyle));
            }
        }
    }
}
