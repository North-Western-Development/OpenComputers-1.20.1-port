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
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverBrewingStand extends DriverSidedTileEntity {
    public static final DriverBrewingStand INSTANCE = new DriverBrewingStand();

    private DriverBrewingStand() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return BrewingStandBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((BrewingStandBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<BrewingStandBlockEntity> implements NamedBlock {
        public Environment(BrewingStandBlockEntity tileEntity) {
            super(tileEntity, "brewing_stand");
        }

        @Override
        public String preferredName() {
            return "brewing_stand";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
        public Object[] getBrewTime(Context context, Arguments args) {
            return result(tileEntity.brewTime);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (!stack.isEmpty() && stack.getItem() == Items.BREWING_STAND)
                return Environment.class;
            else return null;
        }
    }
}
