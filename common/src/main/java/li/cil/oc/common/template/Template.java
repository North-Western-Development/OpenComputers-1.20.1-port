package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Base of OC's own assembler templates. The IMC callbacks ({@code selectX}, {@code validate},
 * {@code assemble}, {@code disassemble}) are {@code static} methods on the subclasses (they are
 * invoked reflectively by name) delegating to the subclass' {@code INSTANCE}.
 */
public abstract class Template {
    protected List<Pair<String, Predicate<Container>>> suggestedComponents() {
        return Arrays.asList(
            Pair.of("BIOS", hasComponent(Constants.ItemName.EEPROM)),
            Pair.of("Screen", hasComponent(Constants.BlockName.ScreenTier1)),
            Pair.of("Keyboard", hasComponent(Constants.BlockName.Keyboard)),
            Pair.of("GraphicsCard", hasGraphicsCard()),
            Pair.of("Inventory", this::hasInventory),
            Pair.of("OS", this::hasFileSystem));
    }

    protected Predicate<Container> hasGraphicsCard() {
        return inventory -> Arrays.stream(new String[]{
            Constants.ItemName.APUCreative,
            Constants.ItemName.APUTier1,
            Constants.ItemName.APUTier2,
            Constants.ItemName.GraphicsCardTier1,
            Constants.ItemName.GraphicsCardTier2,
            Constants.ItemName.GraphicsCardTier3}).anyMatch(name -> hasComponent(name).test(inventory));
    }

    protected abstract Class<? extends EnvironmentHost> hostClass();

    protected Object[] validateComputer(Container inventory) {
        final boolean hasCase = caseTier(inventory) != Tier.None;
        final boolean hasCPU = this.hasCPU(inventory);
        final boolean hasRAM = this.hasRAM(inventory);
        final boolean requiresRAM = this.requiresRAM(inventory);
        final int complexity = this.complexity(inventory);
        final int maxComplexity = this.maxComplexity(inventory);

        final boolean valid = hasCase && hasCPU && (hasRAM || !requiresRAM) && complexity <= maxComplexity;

        final Component progress;
        if (!hasCPU) progress = Localization.Assembler.InsertCPU();
        else if (!hasRAM && requiresRAM) progress = Localization.Assembler.InsertRAM();
        else progress = Localization.Assembler.Complexity(complexity, maxComplexity);

        final List<Component> warnings = new ArrayList<>();
        for (Pair<String, Predicate<Container>> suggested : suggestedComponents()) {
            if (!suggested.getRight().test(inventory)) {
                warnings.add(Localization.Assembler.Warning(suggested.getLeft()));
            }
        }
        if (!warnings.isEmpty()) {
            warnings.add(0, Localization.Assembler.Warnings());
        }

        return new Object[]{valid, progress, warnings.toArray(new Component[0])};
    }

    protected boolean exists(Container inventory, Predicate<ItemStack> p) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && p.test(stack)) return true;
        }
        return false;
    }

    protected boolean hasCPU(Container inventory) {
        return exists(inventory, stack -> li.cil.oc.api.Driver.driverFor(stack, hostClass()) instanceof li.cil.oc.api.driver.item.Processor);
    }

    protected boolean hasRAM(Container inventory) {
        return exists(inventory, stack -> li.cil.oc.api.Driver.driverFor(stack, hostClass()) instanceof li.cil.oc.api.driver.item.Memory);
    }

    protected boolean requiresRAM(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (li.cil.oc.api.Driver.driverFor(stack, hostClass()) instanceof li.cil.oc.api.driver.item.Processor driver) {
                final Class<? extends li.cil.oc.api.machine.Architecture> architecture = driver.architecture(stack);
                if (architecture != null && architecture.getAnnotation(li.cil.oc.api.machine.Architecture.NoMemoryRequirements.class) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    protected Predicate<Container> hasComponent(String name) {
        return inventory -> exists(inventory, stack -> {
            final ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
            return descriptor != null && descriptor.name().equals(name);
        });
    }

    protected boolean hasInventory(Container inventory) {
        return exists(inventory, stack -> li.cil.oc.api.Driver.driverFor(stack, hostClass()) instanceof li.cil.oc.api.driver.item.Inventory);
    }

    protected boolean hasFileSystem(Container inventory) {
        return exists(inventory, stack -> {
            final DriverItem driver = li.cil.oc.api.Driver.driverFor(stack, hostClass());
            return driver != null && (Slot.Floppy.equals(driver.slot(stack)) || Slot.HDD.equals(driver.slot(stack)));
        });
    }

    protected int complexity(Container inventory) {
        int acc = 0;
        for (int slot = 1; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            final DriverItem driver = li.cil.oc.api.Driver.driverFor(stack, hostClass());
            if (driver instanceof li.cil.oc.api.driver.item.Processor) {
                acc += 0; // CPUs are exempt, since they control the limit.
            } else if (driver instanceof li.cil.oc.api.driver.item.Container) {
                acc += (1 + driver.tier(stack)) * 2;
            } else if (driver != null && !Slot.EEPROM.equals(driver.slot(stack))) {
                acc += 1 + driver.tier(stack);
            }
        }
        return acc;
    }

    protected int maxComplexity(Container inventory) {
        final int caseTier = this.caseTier(inventory);
        int cpuTier = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (li.cil.oc.api.Driver.driverFor(stack, hostClass()) instanceof li.cil.oc.api.driver.item.Processor processor) {
                cpuTier += processor.tier(stack);
            }
        }
        if (caseTier >= Tier.One && cpuTier >= Tier.One) {
            return Settings.deviceComplexityByTier[caseTier] - (Math.min(2, caseTier) - cpuTier) * 6;
        } else return 0;
    }

    protected abstract int caseTier(Container inventory);

    /** Helper for slot lists passed to {@code api.IMC.registerAssemblerTemplate} (null = no slot). */
    protected static Pair<String, Integer> toPair(String slot, int tier) {
        return Pair.of(slot, tier);
    }
}
