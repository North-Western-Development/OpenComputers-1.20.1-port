package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class LinkedCard extends SimpleItem implements ItemTier {
    public LinkedCard(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        if (stack.hasTag() && stack.getTag().contains(Settings.namespace + "data")) {
            final CompoundTag data = stack.getTag().getCompound(Settings.namespace + "data");
            if (data.contains(Settings.namespace + "tunnel")) {
                final String channel = data.getString(Settings.namespace + "tunnel");
                final String shown = channel.length() > 13 ? channel.substring(0, 13) + "..." : channel;
                for (String curr : Tooltip.get(unlocalizedName + "_channel", shown)) {
                    tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
    }
}
