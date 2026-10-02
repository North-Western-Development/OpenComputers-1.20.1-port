package li.cil.oc.common.template;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.IMC;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DisassemblerTemplates {
    private DisassemblerTemplates() {
    }

    private static final List<Template> templates = new ArrayList<>();

    public static void add(CompoundTag template) {
        try {
            final Method selector = IMC.getStaticMethod(template.getString("select"), ItemStack.class);
            final Method disassembler = IMC.getStaticMethod(template.getString("disassemble"), ItemStack.class, ItemStack[].class);

            templates.add(new Template(selector, disassembler));
        } catch (Throwable t) {
            OpenComputers.log.warn("Failed registering disassembler template.", t);
        }
    }

    public static Optional<Template> select(ItemStack stack) {
        return templates.stream().filter(template -> template.select(stack)).findFirst();
    }

    public static final class Template {
        public final Method selector;
        public final Method disassembler;

        public Template(Method selector, Method disassembler) {
            this.selector = selector;
            this.disassembler = disassembler;
        }

        public boolean select(ItemStack stack) {
            return IMC.tryInvokeStatic(selector, false, stack);
        }

        /** @return (stacks to queue, stacks to drop) */
        public Pair<Optional<ItemStack[]>, Optional<ItemStack[]>> disassemble(ItemStack stack, ItemStack[] ingredients) {
            final Object result = IMC.tryInvokeStatic(disassembler, null, stack, ingredients);
            if (result instanceof ItemStack[] stacks) {
                return Pair.of(Optional.of(stacks), Optional.empty());
            }
            if (result instanceof Object[] array && array.length == 2) {
                if (array[0] instanceof ItemStack[] stacks && array[1] instanceof ItemStack[] drops) {
                    return Pair.of(Optional.of(stacks), Optional.of(drops));
                }
                if (array[0] instanceof ItemStack single && array[1] instanceof ItemStack[] drops) {
                    return Pair.of(Optional.of(new ItemStack[]{single}), Optional.of(drops));
                }
                if (array[0] instanceof ItemStack[] stacks && array[1] instanceof ItemStack drop) {
                    return Pair.of(Optional.of(stacks), Optional.of(new ItemStack[]{drop}));
                }
            }
            return Pair.of(Optional.empty(), Optional.empty());
        }
    }
}
