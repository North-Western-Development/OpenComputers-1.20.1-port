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
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverFurnace extends DriverSidedTileEntity {
    public static final DriverFurnace INSTANCE = new DriverFurnace();

    private DriverFurnace() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return FurnaceBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((FurnaceBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<FurnaceBlockEntity> implements NamedBlock {
        public Environment(FurnaceBlockEntity tileEntity) {
            super(tileEntity, "furnace");
        }

        @Override
        public String preferredName() {
            return "furnace";
        }

        @Override
        public int priority() {
            return 0;
        }

        // Fields of AbstractFurnaceBlockEntity are made accessible by opencomputers.accesswidener.

        @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
        public Object[] getBurnTime(Context context, Arguments args) {
            return result(tileEntity.litTime);
        }

        @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
        public Object[] getCurrentItemBurnTime(Context context, Arguments args) {
            return result(tileEntity.litDuration);
        }

        @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
        public Object[] getCookTime(Context context, Arguments args) {
            return result(tileEntity.cookingProgress);
        }

        @Callback(doc = "function():number -- The number of ticks that the current item needs to cook.")
        public Object[] getTotalCookTime(Context context, Arguments args) {
            return result(tileEntity.cookingTotalTime);
        }

        @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
        public Object[] isBurning(Context context, Arguments args) {
            return result(tileEntity.litTime > 0);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (!stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.FURNACE)
                return Environment.class;
            else return null;
        }
    }
}
