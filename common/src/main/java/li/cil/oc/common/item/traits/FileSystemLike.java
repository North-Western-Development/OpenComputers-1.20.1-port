package li.cil.oc.common.item.traits;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.client.ItemClientHooks;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.util.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Implementers ({@link SimpleItem} subclasses) return {@code Optional.empty()} from
 * {@code tooltipName()}, call {@link #fsAppendHoverText} after {@code super.appendHoverText}
 * and delegate {@code use(stack, world, player)} to {@link #fsUse}.
 */
public interface FileSystemLike {
    int kiloBytes();

    default void fsAppendHoverText(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag()) {
            final CompoundTag nbt = stack.getTag();
            if (nbt.contains(Settings.namespace + "data")) {
                final CompoundTag data = nbt.getCompound(Settings.namespace + "data");
                if (data.contains(Settings.namespace + "fs.label")) {
                    tooltip.add(Component.literal(data.getString(Settings.namespace + "fs.label")).setStyle(Tooltip.DefaultStyle));
                }
                if (flag.isAdvanced() && data.contains("fs")) {
                    final CompoundTag fsNbt = data.getCompound("fs");
                    if (fsNbt.contains("capacity.used")) {
                        final long used = fsNbt.getLong("capacity.used");
                        tooltip.add(Component.literal(Localization.Tooltip.DiskUsage(used, kiloBytes() * 1024L)).setStyle(Tooltip.DefaultStyle));
                    }
                }
            }
            final DriveData data = new DriveData(stack);
            tooltip.add(Component.literal(Localization.Tooltip.DiskMode(data.isUnmanaged)).setStyle(Tooltip.DefaultStyle));
            tooltip.add(Component.literal(Localization.Tooltip.DiskLock(data.lockInfo)).setStyle(Tooltip.DefaultStyle));
        }
    }

    default InteractionResultHolder<ItemStack> fsUse(ItemStack stack, Level world, Player player) {
        if (!player.isCrouching() && (!stack.hasTag() || !stack.getTag().contains(Settings.namespace + "lootFactory"))) {
            if (world.isClientSide) ItemClientHooks.openDriveGui(player, stack);
            player.swing(InteractionHand.MAIN_HAND);
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }
}
