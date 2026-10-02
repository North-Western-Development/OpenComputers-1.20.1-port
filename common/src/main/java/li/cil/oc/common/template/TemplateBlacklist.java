package li.cil.oc.common.template;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateBlacklist {
    private TemplateBlacklist() {
    }

    private static List<ItemStack> theBlacklist; // scnr

    private static synchronized List<ItemStack> theBlacklist() {
        if (theBlacklist == null) {
            final Pattern pattern = Pattern.compile("^([^@]+)(?:@(\\d+))?$");
            final List<ItemStack> result = new ArrayList<>();
            for (String entry : Settings.get().assemblerBlacklist) {
                final Matcher matcher = pattern.matcher(entry);
                Optional<ItemStack> stack = Optional.empty();
                if (matcher.matches()) {
                    final String id = matcher.group(1);
                    final String meta = matcher.group(2);
                    if (meta == null) {
                        stack = parseDescriptor(id, 0);
                    } else {
                        try {
                            stack = parseDescriptor(id, Integer.parseInt(meta));
                        } catch (NumberFormatException e) {
                            OpenComputers.log.warn("Bad assembler blacklist entry '" + id + "@" + meta + "', invalid damage value.");
                        }
                    }
                } else {
                    OpenComputers.log.warn("Bad assembler blacklist entry '" + entry + "', invalid format (should be 'id' or 'id@damage').");
                }
                stack.ifPresent(result::add);
            }
            theBlacklist = result;
        }
        return theBlacklist;
    }

    private static Optional<ItemStack> parseDescriptor(String id, int meta) {
        final ResourceLocation location = ResourceLocation.tryParse(id);
        final Item item = location != null && BuiltInRegistries.ITEM.containsKey(location) ? BuiltInRegistries.ITEM.get(location) : null;
        if (item == null) {
            OpenComputers.log.warn("Bad assembler blacklist entry '" + id + "', unknown item id.");
            return Optional.empty();
        } else {
            final ItemStack stack = new ItemStack(item, 1);
            stack.setDamageValue(meta);
            return Optional.of(stack);
        }
    }

    public static void register() {
        li.cil.oc.api.IMC.registerAssemblerFilter("li.cil.oc.common.template.TemplateBlacklist.filter");
    }

    public static boolean filter(ItemStack stack) {
        return theBlacklist().stream().noneMatch(entry -> ItemStack.isSameItem(entry, stack));
    }
}
