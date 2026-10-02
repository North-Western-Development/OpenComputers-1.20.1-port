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

public final class RobotTemplate extends Template {
    public static final RobotTemplate INSTANCE = new RobotTemplate();

    private RobotTemplate() {
    }

    @Override
    protected Class<? extends EnvironmentHost> hostClass() {
        return li.cil.oc.api.internal.Robot.class;
    }

    public static boolean selectTier1(ItemStack stack) {
        return is(stack, Constants.BlockName.CaseTier1);
    }

    public static boolean selectTier2(ItemStack stack) {
        return is(stack, Constants.BlockName.CaseTier2);
    }

    public static boolean selectTier3(ItemStack stack) {
        return is(stack, Constants.BlockName.CaseTier3);
    }

    public static boolean selectCreative(ItemStack stack) {
        return is(stack, Constants.BlockName.CaseCreative);
    }

    public static Object[] validate(Container inventory) {
        return INSTANCE.validateComputer(inventory);
    }

    public static Object[] assemble(Container inventory) {
        final List<ItemStack> items = items(inventory, 1);
        final RobotData data = new RobotData();
        data.tier = INSTANCE.caseTier(inventory);
        data.name = RobotData.randomName();
        data.robotEnergy = (int) Settings.get().bufferRobot;
        data.totalEnergy = data.robotEnergy;
        data.containers = nonEmpty(items.subList(0, Math.min(3, items.size())));
        data.components = nonEmpty(items.subList(Math.min(3, items.size()), items.size()));
        final ItemStack stack = data.createItemStack();
        final double energy = Settings.get().robotBaseCost + INSTANCE.complexity(inventory) * Settings.get().robotComplexityCost;

        return new Object[]{stack, energy};
    }

    public static boolean selectDisassembler(ItemStack stack) {
        return is(stack, Constants.BlockName.Robot);
    }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        final RobotData info = new RobotData(stack);
        final String itemName = Constants.BlockName.Case(info.tier);

        final List<ItemStack> result = new ArrayList<>();
        result.add(li.cil.oc.api.Items.get(itemName).createItemStack(1));
        result.addAll(Arrays.asList(info.containers));
        result.addAll(Arrays.asList(info.components));
        return result.toArray(new ItemStack[0]);
    }

    public static void register() {
        // Tier 1
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Robot (Tier 1)",
          "li.cil.oc.common.template.RobotTemplate.selectTier1",
          "li.cil.oc.common.template.RobotTemplate.validate",
          "li.cil.oc.common.template.RobotTemplate.assemble",
          INSTANCE.hostClass(),
          new int[]{Tier.Two, Tier.One, Tier.One},
          new int[]{Tier.One, Tier.One, Tier.One},
          Arrays.asList(
            toPair(Slot.Card, Tier.One),
            null,
            null,
            toPair(Slot.CPU, Tier.One),
            toPair(Slot.Memory, Tier.One),
            toPair(Slot.Memory, Tier.One),
            toPair(Slot.EEPROM, Tier.Any),
            toPair(Slot.HDD, Tier.One)
          ));

        // Tier 2
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Robot (Tier 2)",
          "li.cil.oc.common.template.RobotTemplate.selectTier2",
          "li.cil.oc.common.template.RobotTemplate.validate",
          "li.cil.oc.common.template.RobotTemplate.assemble",
          INSTANCE.hostClass(),
          new int[]{Tier.Three, Tier.Two, Tier.One},
          new int[]{Tier.Two, Tier.Two, Tier.Two, Tier.One, Tier.One, Tier.One},
          Arrays.asList(
            toPair(Slot.Card, Tier.Two),
            toPair(Slot.Card, Tier.One),
            null,
            toPair(Slot.CPU, Tier.Two),
            toPair(Slot.Memory, Tier.Two),
            toPair(Slot.Memory, Tier.Two),
            toPair(Slot.EEPROM, Tier.Any),
            toPair(Slot.HDD, Tier.Two)
          ));

        // Tier 3
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Robot (Tier 3)",
          "li.cil.oc.common.template.RobotTemplate.selectTier3",
          "li.cil.oc.common.template.RobotTemplate.validate",
          "li.cil.oc.common.template.RobotTemplate.assemble",
          INSTANCE.hostClass(),
          new int[]{Tier.Three, Tier.Two, Tier.Two},
          new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Two, Tier.Two, Tier.Two, Tier.One, Tier.One, Tier.One},
          Arrays.asList(
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Two),
            toPair(Slot.Card, Tier.Two),
            toPair(Slot.CPU, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.EEPROM, Tier.Any),
            toPair(Slot.HDD, Tier.Three),
            toPair(Slot.HDD, Tier.Two)
          ));

        // Creative
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Robot (Creative)",
          "li.cil.oc.common.template.RobotTemplate.selectCreative",
          "li.cil.oc.common.template.RobotTemplate.validate",
          "li.cil.oc.common.template.RobotTemplate.assemble",
          INSTANCE.hostClass(),
          new int[]{Tier.Three, Tier.Three, Tier.Three},
          new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three},
          Arrays.asList(
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.CPU, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.EEPROM, Tier.Any),
            toPair(Slot.HDD, Tier.Three),
            toPair(Slot.HDD, Tier.Three)
          ));

        // Disassembler
        li.cil.oc.api.IMC.registerDisassemblerTemplate(
          "Robot",
          "li.cil.oc.common.template.RobotTemplate.selectDisassembler",
          "li.cil.oc.common.template.RobotTemplate.disassemble");

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
