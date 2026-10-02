package li.cil.oc.common.item.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.integration.opencomputers.DriverCPU;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementers ({@link SimpleItem} subclasses) delegate {@code tooltipData()},
 * {@code tooltipExtended(...)} and {@code use(stack, world, player)} to the
 * {@code cpu*} default methods below.
 */
public interface CPULike {
    int cpuTier();

    default List<Object> cpuTooltipData() {
        return Collections.singletonList(Settings.get().cpuComponentSupport[cpuTier()]);
    }

    default void cpuTooltipExtended(ItemStack stack, List<Component> tooltip) {
        for (String curr : Tooltip.get("cpu.Architecture", Machine.getArchitectureName(DriverCPU.INSTANCE.architecture(stack)))) {
            tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
        }
    }

    default InteractionResultHolder<ItemStack> cpuUse(ItemStack stack, Level world, Player player) {
        if (player.isCrouching()) {
            if (!world.isClientSide) {
                final DriverItem driver = Driver.driverFor(stack);
                if (driver instanceof MutableProcessor processor) {
                    final List<Class<? extends li.cil.oc.api.machine.Architecture>> architectures = new ArrayList<>(processor.allArchitectures());
                    if (!architectures.isEmpty()) {
                        final int currentIndex = architectures.indexOf(processor.architecture(stack));
                        final int newIndex = (currentIndex + 1) % architectures.size();
                        final Class<? extends li.cil.oc.api.machine.Architecture> archClass = architectures.get(newIndex);
                        final String archName = Machine.getArchitectureName(archClass);
                        processor.setArchitecture(stack, archClass);
                        player.sendSystemMessage(Component.translatable(Settings.namespace + "tooltip.cpu.Architecture", archName));
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }
                // else: No known driver for this processor.
            }
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }
}
