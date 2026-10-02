package li.cil.oc.common.item;

import li.cil.oc.Localization;
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

public class UpgradeExperience extends SimpleItem implements ItemTier {
    public UpgradeExperience(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        if (stack.hasTag()) {
            final CompoundTag nbt = li.cil.oc.integration.opencomputers.Item.dataTagStatic(stack);
            final double experience = li.cil.oc.util.UpgradeExperience.getExperience(nbt);
            final int level = li.cil.oc.util.UpgradeExperience.calculateLevelFromExperience(experience);
            final double reportedLevel = li.cil.oc.util.UpgradeExperience.calculateExperienceLevel(level, experience);
            tooltip.add(Component.literal(Localization.Tooltip.ExperienceLevel(reportedLevel)).setStyle(Tooltip.DefaultStyle));
        }
    }
}
