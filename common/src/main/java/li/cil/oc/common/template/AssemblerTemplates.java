package li.cil.oc.common.template;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.IMC;
import li.cil.oc.common.Tier;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AssemblerTemplates {
    private AssemblerTemplates() {
    }

    public static final Slot NoSlot = new Slot(li.cil.oc.common.Slot.None, Tier.None, Optional.empty(), Optional.empty());

    private static final List<Template> templates = new ArrayList<>();

    private static final List<Method> templateFilters = new ArrayList<>();

    public static void add(CompoundTag template) throws ReflectiveOperationException {
        final Method selector = IMC.getStaticMethod(template.getString("select"), ItemStack.class);
        final Method validator = IMC.getStaticMethod(template.getString("validate"), Container.class);
        final Method assembler = IMC.getStaticMethod(template.getString("assemble"), Container.class);
        final Optional<Class<? extends EnvironmentHost>> hostClass = tryGetHostClass(template.getString("hostClass"));
        final Slot[] containerSlots = parseSlots(template, "containerSlots", Optional.of(li.cil.oc.common.Slot.Container), hostClass, 3);
        final Slot[] upgradeSlots = parseSlots(template, "upgradeSlots", Optional.of(li.cil.oc.common.Slot.Upgrade), hostClass, 9);
        final Slot[] componentSlots = parseSlots(template, "componentSlots", Optional.empty(), hostClass, 9);

        templates.add(new Template(selector, validator, assembler, containerSlots, upgradeSlots, componentSlots));
    }

    private static Slot[] parseSlots(CompoundTag template, String name, Optional<String> kindOverride, Optional<Class<? extends EnvironmentHost>> hostClass, int count) throws ReflectiveOperationException {
        final Slot[] result = new Slot[count];
        final List<CompoundTag> tags = ExtendedNBT.map(template.getList(name, Tag.TAG_COMPOUND), (CompoundTag tag) -> tag);
        for (int i = 0; i < count; i++) {
            result[i] = i < tags.size() ? parseSlot(tags.get(i), kindOverride, hostClass) : NoSlot;
        }
        return result;
    }

    public static void addFilter(String method) throws ReflectiveOperationException {
        templateFilters.add(IMC.getStaticMethod(method, ItemStack.class));
    }

    public static Optional<Template> select(ItemStack stack) {
        if (!stack.isEmpty() && templateFilters.stream().allMatch(filter -> IMC.tryInvokeStatic(filter, true, stack))) {
            return templates.stream().filter(template -> template.select(stack)).findFirst();
        } else {
            return Optional.empty();
        }
    }

    public static final class Template {
        public final Method selector;
        public final Method validator;
        public final Method assembler;
        public final Slot[] containerSlots;
        public final Slot[] upgradeSlots;
        public final Slot[] componentSlots;

        public Template(Method selector, Method validator, Method assembler, Slot[] containerSlots, Slot[] upgradeSlots, Slot[] componentSlots) {
            this.selector = selector;
            this.validator = validator;
            this.assembler = assembler;
            this.containerSlots = containerSlots;
            this.upgradeSlots = upgradeSlots;
            this.componentSlots = componentSlots;
        }

        public boolean select(ItemStack stack) {
            return IMC.tryInvokeStatic(selector, false, stack);
        }

        /** @return (valid, progress, warnings) */
        public Triple<Boolean, Component, Component[]> validate(Container inventory) {
            final Object result = IMC.tryInvokeStatic(validator, null, inventory);
            if (result instanceof Object[] array) {
                if (array.length == 3 && array[0] instanceof Boolean valid && array[1] instanceof Component progress && array[2] instanceof Component[] warnings) {
                    return Triple.of(valid, progress, warnings);
                }
                if (array.length == 2 && array[0] instanceof Boolean valid && array[1] instanceof Component progress) {
                    return Triple.of(valid, progress, new Component[0]);
                }
                if (array.length == 1 && array[0] instanceof Boolean valid) {
                    return Triple.of(valid, null, new Component[0]);
                }
            }
            return Triple.of(false, null, new Component[0]);
        }

        /** @return (result, energy) */
        public Pair<ItemStack, Double> assemble(Container inventory) {
            final Object result = IMC.tryInvokeStatic(assembler, null, inventory);
            if (result instanceof Object[] array) {
                if (array.length == 2 && array[0] instanceof ItemStack stack && array[1] instanceof Number energy) {
                    return Pair.of(stack, energy.doubleValue());
                }
                if (array.length == 1 && array[0] instanceof ItemStack stack) {
                    return Pair.of(stack, 0.0);
                }
            }
            return Pair.of(ItemStack.EMPTY, 0.0);
        }
    }

    public static final class Slot {
        public final String kind;
        public final int tier;
        public final Optional<Method> validator;
        public final Optional<Class<? extends EnvironmentHost>> hostClass;

        public Slot(String kind, int tier, Optional<Method> validator, Optional<Class<? extends EnvironmentHost>> hostClass) {
            this.kind = kind;
            this.tier = tier;
            this.validator = validator;
            this.hostClass = hostClass;
        }

        public boolean validate(Container inventory, int slot, ItemStack stack) {
            if (validator.isPresent()) {
                return IMC.tryInvokeStatic(validator.get(), false, inventory, Integer.valueOf(slot), Integer.valueOf(tier), stack);
            }
            final DriverItem driver = hostClass.isPresent()
                ? li.cil.oc.api.Driver.driverFor(stack, hostClass.get())
                : li.cil.oc.api.Driver.driverFor(stack);
            if (driver != null) {
                try {
                    return driver.slot(stack).equals(kind) && driver.tier(stack) <= tier;
                } catch (AbstractMethodError t) {
                    OpenComputers.log.warn("Error trying to query driver '" + driver.getClass().getName() + "' for slot and/or tier information. Probably their fault. Yell at them before coming to OpenComputers for support. :P");
                    return false;
                }
            }
            return false;
        }
    }

    private static Slot parseSlot(CompoundTag nbt, Optional<String> kindOverride, Optional<Class<? extends EnvironmentHost>> hostClass) throws ReflectiveOperationException {
        final String kind = kindOverride.orElse(nbt.contains("type") ? nbt.getString("type") : li.cil.oc.common.Slot.None);
        final int tier = nbt.contains("tier") ? nbt.getInt("tier") : Tier.Any;
        final Optional<Method> validator = nbt.contains("validate")
            ? Optional.of(IMC.getStaticMethod(nbt.getString("validate"), Container.class, int.class, int.class, ItemStack.class))
            : Optional.empty();
        return new Slot(kind, tier, validator, hostClass);
    }

    private static Optional<Class<? extends EnvironmentHost>> tryGetHostClass(String name) throws ClassNotFoundException {
        if (Strings.isNullOrEmpty(name)) return Optional.empty();
        else return Optional.of(Class.forName(name).asSubclass(EnvironmentHost.class));
    }
}
