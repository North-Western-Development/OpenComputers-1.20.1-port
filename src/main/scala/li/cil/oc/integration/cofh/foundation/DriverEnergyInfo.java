package li.cil.oc.integration.cofh.foundation;

import cofh.thermal.lib.block.entity.AugmentableBlockEntity;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class DriverEnergyInfo extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return AugmentableBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((AugmentableBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<AugmentableBlockEntity> {
        public Environment(final AugmentableBlockEntity tileEntity) {
            super(tileEntity, "energy_info");
        }

        @Callback(doc = "function():number --  Returns the amount of stored energy.")
        public Object[] getEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getEnergyStorage().getEnergyStored()};
        }

        @Callback(doc = "function():number --  Returns the energy per tick.")
        public Object[] getEnergyPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCurSpeed()};
        }

        @Callback(doc = "function():number --  Returns the maximum energy per tick.")
        public Object[] getMaxEnergyPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxSpeed()};
        }
    }
}
