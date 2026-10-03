package li.cil.oc.integration.mekanism;

import li.cil.oc.Settings;
import li.cil.oc.api.driver.Converter;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Map;

/**
 * Converts Mekanism chemical stacks (gas, infuse type, pigment and slurry stacks).
 * Formerly {@code ConverterGasStack}, which only handled gases.
 */
public final class ConverterChemicalStack implements Converter {
    public static final ConverterChemicalStack INSTANCE = new ConverterChemicalStack();

    private ConverterChemicalStack() {
    }

    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ChemicalStack<?> stack) {
            convertStack(stack, output);
        }
    }

    static void convertStack(final ChemicalStack<?> stack, final Map<Object, Object> output) {
        output.put("amount", stack.getAmount());
        if (stack.isEmpty()) {
            return;
        }
        final Chemical<?> chemical = stack.getType();
        final ChemicalType type = ChemicalType.getTypeFor(chemical);
        if (type != null) {
            output.put("type", type.getSerializedName());
        }
        if (Settings.get().insertIdsInConverters && type != null) {
            final int id = registryId(type, chemical);
            if (id >= 0) {
                output.put("id", id);
            }
        }
        output.put("name", stack.getTypeRegistryName().toString());
        output.put("label", stack.getTextComponent().getString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int registryId(final ChemicalType type, final Chemical<?> chemical) {
        final IForgeRegistry registry = switch (type) {
            case GAS -> MekanismAPI.gasRegistry();
            case INFUSION -> MekanismAPI.infuseTypeRegistry();
            case PIGMENT -> MekanismAPI.pigmentRegistry();
            case SLURRY -> MekanismAPI.slurryRegistry();
        };
        if (registry instanceof ForgeRegistry forgeRegistry) {
            return forgeRegistry.getID(chemical);
        }
        return -1;
    }
}
