package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

public class EEPROM extends SimpleItem {
    public EEPROM(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasTag()) {
            final CompoundTag tag = stack.getTag();
            if (tag.contains(Settings.namespace + "data")) {
                final CompoundTag data = tag.getCompound(Settings.namespace + "data");
                if (data.contains(Settings.namespace + "label")) {
                    return Component.literal(data.getString(Settings.namespace + "label"));
                }
            }
        }
        return super.getName(stack);
    }

    @Override
    public boolean sneakBypassesUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }
}
