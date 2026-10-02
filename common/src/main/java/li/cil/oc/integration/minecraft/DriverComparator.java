package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverComparator extends DriverSidedTileEntity {
    public static final DriverComparator INSTANCE = new DriverComparator();

    private DriverComparator() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return ComparatorBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((ComparatorBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ComparatorBlockEntity> implements NamedBlock {
        public Environment(ComparatorBlockEntity tileEntity) {
            super(tileEntity, "comparator");
        }

        @Override
        public String preferredName() {
            return "comparator";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
        public Object[] getOutputSignal(Context context, Arguments args) {
            return result(tileEntity.getOutputSignal());
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (!stack.isEmpty() && stack.getItem() == Items.COMPARATOR)
                return Environment.class;
            else return null;
        }
    }
}
