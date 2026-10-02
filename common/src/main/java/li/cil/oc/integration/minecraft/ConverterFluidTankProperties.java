package li.cil.oc.integration.minecraft;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.util.ExtendedArguments.TankProperties;

import java.util.Map;

public final class ConverterFluidTankProperties implements Converter {
    public static final ConverterFluidTankProperties INSTANCE = new ConverterFluidTankProperties();

    private ConverterFluidTankProperties() {
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof TankProperties properties) {
            output.put("capacity", (int) properties.capacity);
            final FluidStack fluid = properties.contents;
            if (fluid != null) {
                ConverterFluidStack.INSTANCE.convert(fluid, output);
            } else output.put("amount", 0);
        }
    }
}
