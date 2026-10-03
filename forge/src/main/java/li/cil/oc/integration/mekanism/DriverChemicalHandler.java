package li.cil.oc.integration.mekanism;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.slurry.ISlurryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Driver for blocks exposing Mekanism chemical handlers (gas, infusion, pigment, slurry), e.g.
 * chemical tanks and machines. Read-only, like the generic fluid handler driver.
 */
public final class DriverChemicalHandler implements DriverBlock {
    // Same capabilities as mekanism.common.capabilities.Capabilities (not part of Mekanism's API).
    private static final Capability<IGasHandler> GAS_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IInfusionHandler> INFUSION_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IPigmentHandler> PIGMENT_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<ISlurryHandler> SLURRY_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});

    private static final List<Capability<? extends IChemicalHandler<?, ?>>> CAPABILITIES = List.of(GAS_HANDLER, INFUSION_HANDLER, PIGMENT_HANDLER, SLURRY_HANDLER);

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity != null && !handlers(tileEntity, side).isEmpty();
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity != null ? new Environment(tileEntity, side) : null;
    }

    private static List<IChemicalHandler<?, ?>> handlers(final BlockEntity tileEntity, final Direction side) {
        final List<IChemicalHandler<?, ?>> result = new ArrayList<>();
        for (Capability<? extends IChemicalHandler<?, ?>> capability : CAPABILITIES) {
            tileEntity.getCapability(capability, side).ifPresent(result::add);
        }
        return result;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<BlockEntity> {
        private final Direction side;

        public Environment(final BlockEntity tileEntity, final Direction side) {
            super(tileEntity, "chemical_handler");
            this.side = side;
        }

        @Callback(doc = "function():table -- Get information about the chemical tanks (gas, infusion, pigment, slurry) of the block: " +
                "a list of {type, amount, capacity, name, label}.")
        public Object[] getChemicalTanks(final Context context, final Arguments args) {
            final List<Map<Object, Object>> tanks = new ArrayList<>();
            if (!tileEntity.isRemoved()) {
                for (IChemicalHandler<?, ?> handler : handlers(tileEntity, side)) {
                    for (int tank = 0; tank < handler.getTanks(); tank++) {
                        final Map<Object, Object> info = new HashMap<>();
                        ConverterChemicalStack.convertStack(handler.getChemicalInTank(tank), info);
                        info.putIfAbsent("type", typeOf(handler));
                        info.put("capacity", handler.getTankCapacity(tank));
                        tanks.add(info);
                    }
                }
            }
            return new Object[]{tanks.toArray()};
        }

        private static String typeOf(final IChemicalHandler<?, ?> handler) {
            if (handler instanceof IGasHandler) return "gas";
            if (handler instanceof IInfusionHandler) return "infuse_type";
            if (handler instanceof IPigmentHandler) return "pigment";
            if (handler instanceof ISlurryHandler) return "slurry";
            return "unknown";
        }
    }
}
