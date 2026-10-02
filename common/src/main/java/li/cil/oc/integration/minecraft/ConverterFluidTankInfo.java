package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.common.transfer.FluidHandler;

import java.util.Map;

/**
 * Converts single tanks. On 1.16.5 this handled Forge's {@code IFluidTank}; the
 * equivalent now is a {@link FluidHandler} (the first tank is described).
 */
public final class ConverterFluidTankInfo implements Converter {
    public static final ConverterFluidTankInfo INSTANCE = new ConverterFluidTankInfo();

    private ConverterFluidTankInfo() {
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof FluidHandler tankInfo && tankInfo.getTanks() > 0) {
            output.put("capacity", (int) tankInfo.getTankCapacity(0));
            if (!tankInfo.getFluidInTank(0).isEmpty()) {
                ConverterFluidStack.INSTANCE.convert(tankInfo.getFluidInTank(0), output);
            } else output.put("amount", 0);
        }
    }
}
