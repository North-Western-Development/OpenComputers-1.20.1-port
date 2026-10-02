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

public final class TabletTemplate extends Template {
    public static final TabletTemplate INSTANCE = new TabletTemplate();

    private TabletTemplate() {
    }

    @Override
    protected List<Pair<String, Predicate<Container>>> suggestedComponents() {
        return Arrays.asList(
            Pair.of("BIOS", hasComponent(Constants.ItemName.EEPROM)),
            Pair.of("Keyboard", hasComponent(Constants.BlockName.Keyboard)),
            Pair.of("GraphicsCard", hasGraphicsCard()),
            Pair.of("OS", this::hasFileSystem));
    }

    @Override
    protected Class<? extends EnvironmentHost> hostClass() {
        return li.cil.oc.api.internal.Tablet.class;
    }

    public static boolean selectTier1(ItemStack stack) {
        return is(stack, Constants.ItemName.TabletCaseTier1);
    }

    public static boolean selectTier2(ItemStack stack) {
        return is(stack, Constants.ItemName.TabletCaseTier2);
    }

    public static boolean selectCreative(ItemStack stack) {
        return is(stack, Constants.ItemName.TabletCaseCreative);
    }

    public static Object[] validate(Container inventory) {
        return INSTANCE.validateComputer(inventory);
    }

    public static Object[] assemble(Container inventory) {
        final List<ItemStack> items = items(inventory, 1);
        final TabletData data = new TabletData();
        data.tier = ItemUtils.caseTier(inventory.getItem(0));
        data.container = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        final List<ItemStack> tabletItems = new ArrayList<>();
        tabletItems.add(li.cil.oc.api.Items.get(Constants.BlockName.ScreenTier1).createItemStack(1));
        final int drop = data.tier == Tier.One ? 0 : 1;
        tabletItems.addAll(Arrays.asList(nonEmpty(items.subList(Math.min(drop, items.size()), items.size()))));
        data.items = tabletItems.toArray(new ItemStack[0]);
        data.energy = Settings.get().bufferTablet;
        data.maxEnergy = data.energy;
        final ItemStack stack = li.cil.oc.api.Items.get(Constants.ItemName.Tablet).createItemStack(1);
        data.saveData(stack);
        final double energy = Settings.get().tabletBaseCost + INSTANCE.complexity(inventory) * Settings.get().tabletComplexityCost;

        return new Object[]{stack, energy};
    }

    public static boolean selectDisassembler(ItemStack stack) {
        return is(stack, Constants.ItemName.Tablet);
    }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        final TabletData info = new TabletData(stack);
        final String itemName = Constants.ItemName.TabletCase(info.tier);
        final List<ItemStack> result = new ArrayList<>();
        result.add(li.cil.oc.api.Items.get(itemName).createItemStack(1));
        result.add(info.container);
        final ItemStack[] nonEmptyItems = nonEmpty(Arrays.asList(info.items));
        result.addAll(Arrays.asList(nonEmptyItems).subList(Math.min(1, nonEmptyItems.length), nonEmptyItems.length)); // Screen
        return result.stream().filter(s -> !s.isEmpty()).toArray(ItemStack[]::new);
    }

    public static void register() {
        // Tier 1
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Tablet (Tier 1)",
          "li.cil.oc.common.template.TabletTemplate.selectTier1",
          "li.cil.oc.common.template.TabletTemplate.validate",
          "li.cil.oc.common.template.TabletTemplate.assemble",
          INSTANCE.hostClass(),
          null,
          new int[]{Tier.Three, Tier.Two, Tier.One},
          Arrays.asList(
            toPair(Slot.Card, Tier.Two),
            toPair(Slot.Card, Tier.Two),
            null,
            toPair(Slot.CPU, Tier.Two),
            toPair(Slot.Memory, Tier.Two),
            toPair(Slot.Memory, Tier.Two),
            toPair(Slot.EEPROM, Tier.Any),
            toPair(Slot.HDD, Tier.Two)
          ));

        // Tier 2
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Tablet (Tier 2)",
          "li.cil.oc.common.template.TabletTemplate.selectTier2",
          "li.cil.oc.common.template.TabletTemplate.validate",
          "li.cil.oc.common.template.TabletTemplate.assemble",
          INSTANCE.hostClass(),
          new int[]{Tier.Two},
          new int[]{Tier.Three, Tier.Two, Tier.Two},
          Arrays.asList(
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Two),
            null,
            toPair(Slot.CPU, Tier.Three),
            toPair(Slot.Memory, Tier.Two),
            toPair(Slot.Memory, Tier.Two),
            toPair(Slot.EEPROM, Tier.Any),
            toPair(Slot.HDD, Tier.Two)
          ));

        // Creative
        li.cil.oc.api.IMC.registerAssemblerTemplate(
          "Tablet (Creative)",
          "li.cil.oc.common.template.TabletTemplate.selectCreative",
          "li.cil.oc.common.template.TabletTemplate.validate",
          "li.cil.oc.common.template.TabletTemplate.assemble",
          INSTANCE.hostClass(),
          new int[]{Tier.Three},
          new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three},
          Arrays.asList(
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.Card, Tier.Three),
            toPair(Slot.CPU, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.Memory, Tier.Three),
            toPair(Slot.EEPROM, Tier.Any),
            toPair(Slot.HDD, Tier.Three)
          ));

        // Disassembler
        li.cil.oc.api.IMC.registerDisassemblerTemplate(
          "Tablet",
          "li.cil.oc.common.template.TabletTemplate.selectDisassembler",
          "li.cil.oc.common.template.TabletTemplate.disassemble");

    }

    @Override
    protected int maxComplexity(Container inventory) {
        return super.maxComplexity(inventory) / 2 + 5;
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
