package li.cil.oc.integration.minecraft;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.driver.Converter;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;

/**
 * Converts Architectury {@link FluidStack}s. Amounts are expected in millibuckets,
 * as used by {@link li.cil.oc.common.transfer.FluidHandler}.
 */
public final class ConverterFluidStack implements Converter {
    public static final ConverterFluidStack INSTANCE = new ConverterFluidStack();

    private ConverterFluidStack() {
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof FluidStack stack) {
            output.put("amount", (int) stack.getAmount());
            output.put("hasTag", stack.hasTag());
            output.put("name", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
            output.put("label", stack.getName().getString());
        }
    }
}
