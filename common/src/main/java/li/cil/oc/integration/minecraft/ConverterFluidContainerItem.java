package li.cil.oc.integration.minecraft;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.common.transfer.ItemFluidHandler;
import li.cil.oc.util.FluidUtils;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds the fluid contents of fluid container items (buckets, tanks, cells, ...) to item stack tables:
 * {@code capacity} (total, mB) and {@code fluid} (the fluid stack, or a list of per-tank tables with
 * {@code capacity} for items with several tanks).
 */
public final class ConverterFluidContainerItem implements Converter {
    public static final ConverterFluidContainerItem INSTANCE = new ConverterFluidContainerItem();

    private ConverterFluidContainerItem() {
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (!(value instanceof ItemStack stack) || stack.isEmpty()) return;
        final ItemFluidHandler fc = FluidUtils.fluidHandlerOf(stack);
        if (fc == null || fc.getTanks() < 1) return;

        long capacity = 0;
        for (int i = 0; i < fc.getTanks(); i++) {
            capacity += fc.getTankCapacity(i);
        }
        output.put("capacity", (int) Math.min(capacity, Integer.MAX_VALUE));
        if (fc.getTanks() > 1) {
            final List<Map<Object, Object>> tanks = new ArrayList<>();
            for (int i = 0; i < fc.getTanks(); i++) {
                final Map<Object, Object> tank = new HashMap<>();
                tank.put("capacity", (int) Math.min(fc.getTankCapacity(i), Integer.MAX_VALUE));
                final FluidStack fluid = fc.getFluidInTank(i);
                if (fluid != null) {
                    ConverterFluidStack.INSTANCE.convert(fluid, tank);
                } else {
                    tank.put("amount", 0);
                }
                tanks.add(tank);
            }
            output.put("fluid", tanks);
        } else {
            output.put("fluid", fc.getFluidInTank(0));
        }
    }
}
