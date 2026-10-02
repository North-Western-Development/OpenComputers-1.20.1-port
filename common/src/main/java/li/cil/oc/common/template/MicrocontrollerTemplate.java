package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.util.ItemUtils;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public final class MicrocontrollerTemplate extends Template {
    public static final MicrocontrollerTemplate INSTANCE = new MicrocontrollerTemplate();

    private MicrocontrollerTemplate() {
    }

    @Override
    protected List<Pair<String, Predicate<Container>>> suggestedComponents() {
        return List.of(Pair.of("BIOS", hasComponent("eeprom")));
    }

    @Override
    protected Class<? extends EnvironmentHost> hostClass() {
        return li.cil.oc.api.internal.Microcontroller.class;
    }

    public static boolean selectTier1(ItemStack stack) {
        return is(stack, Constants.ItemName.MicrocontrollerCaseTier1);
    }

    public static boolean selectTier2(ItemStack stack) {
        return is(stack, Constants.ItemName.MicrocontrollerCaseTier2);
    }

    public static boolean selectTierCreative(ItemStack stack) {
        return is(stack, Constants.ItemName.MicrocontrollerCaseCreative);
    }

    public static Object[] validate(Container inventory) {
        return INSTANCE.validateComputer(inventory);
    }

    public static Object[] assemble(Container inventory) {
        final MicrocontrollerData data = new MicrocontrollerData();
        data.tier = INSTANCE.caseTier(inventory);
        data.components = nonEmpty(items(inventory, 1));
        data.storedEnergy = (int) Settings.get().bufferMicrocontroller;
        final ItemStack stack = data.createItemStack();
        final double energy = Settings.get().microcontrollerBaseCost + INSTANCE.complexity(inventory) * Settings.get().microcontrollerComplexityCost;

        return new Object[]{stack, energy};
    }

    public static boolean selectDisassembler(ItemStack stack) {
        return is(stack, Constants.BlockName.Microcontroller);
    }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        final MicrocontrollerData info = new MicrocontrollerData(stack);
        final String itemName = Constants.ItemName.MicrocontrollerCase(info.tier);

        final List<ItemStack> result = new ArrayList<>();
        result.add(li.cil.oc.api.Items.get(itemName).createItemStack(1));
        result.addAll(Arrays.asList(info.components));
        return result.toArray(new ItemStack[0]);
    }

    public static void register() {
        // Tier 1
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Microcontroller (Tier 1)",
          "li.cil.oc.common.template.MicrocontrollerTemplate.selectTier1",
          "li.cil.oc.common.template.MicrocontrollerTemplate.validate",
          "li.cil.oc.common.template.MicrocontrollerTemplate.assemble",
          INSTANCE.hostClass(),
          null,
          new int[]{Tier.Two},
          Arrays.asList(
            toPair(Slot.Card, Tier.One),
            toPair(Slot.Card, Tier.One),
            null,
            toPair(Slot.CPU, Tier.One),
            toPair(Slot.Memory, Tier.One),
            null,
            toPair(Slot.EEPROM, Tier.Any)
          ));

        // Tier 2
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Microcontroller (Tier 2)",
          "li.cil.oc.common.template.MicrocontrollerTemplate.selectTier2",
          "li.cil.oc.common.template.MicrocontrollerTemplate.validate",
          "li.cil.oc.common.template.MicrocontrollerTemplate.assemble",
          INSTANCE.hostClass(),
          null,
          new int[]{Tier.Three},
          Arrays.asList(
            toPair(Slot.Card, Tier.Two),
            toPair(Slot.Card, Tier.One),
            null,
            toPair(Slot.CPU, Tier.One),
            toPair(Slot.Memory, Tier.One),
            toPair(Slot.Memory, Tier.One),
            toPair(Slot.EEPROM, Tier.Any)
          ));

        // Creative
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Microcontroller (Creative)",
          "li.cil.oc.common.template.MicrocontrollerTemplate.selectTierCreative",
          "li.cil.oc.common.template.MicrocontrollerTemplate.validate",
          "li.cil.oc.common.template.MicrocontrollerTemplate.assemble",
          INSTANCE.hostClass(),
          null,
          new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three},
          Arrays.asList(
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.CPU, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.EEPROM, Tier.Any)
          ));

        // Disassembler
        li.cil.oc.api.IMC.registerDisassemblerTemplate(
          "Microcontroller",
          "li.cil.oc.common.template.MicrocontrollerTemplate.selectDisassembler",
          "li.cil.oc.common.template.MicrocontrollerTemplate.disassemble");

    }

    @Override
    protected int maxComplexity(Container inventory) {
        if (caseTier(inventory) == Tier.Two) return 5;
        else if (caseTier(inventory) == Tier.Four) return 9001; // Creative
        else return 4;
    }

    @Override
    protected int caseTier(Container inventory) {
        return ItemUtils.caseTier(inventory.getItem(0));
    }

    private static boolean is(ItemStack stack, String name) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(name);
    }

    private static List<ItemStack> items(Container inventory, int from) {
        final List<ItemStack> result = new ArrayList<>();
        for (int slot = from; slot < inventory.getContainerSize(); slot++) result.add(inventory.getItem(slot));
        return result;
    }

    private static ItemStack[] nonEmpty(List<ItemStack> stacks) {
        return stacks.stream().filter(s -> !s.isEmpty()).toArray(ItemStack[]::new);
    }
}
