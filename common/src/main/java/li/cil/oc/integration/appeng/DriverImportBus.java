package li.cil.oc.integration.appeng;

import appeng.api.parts.IPartHost;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DriverImportBus implements DriverBlock {
    public static final DriverImportBus INSTANCE = new DriverImportBus();

    private DriverImportBus() {
    }

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        return AEUtil.hasPart(world, pos, AEUtil::isImportBus);
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((IPartHost) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPartHost> implements NamedBlock, PartEnvironmentBase {
        public Environment(IPartHost host) {
            super(host, "me_importbus");
        }

        @Override
        public IPartHost host() {
            return tileEntity;
        }

        @Override
        public String preferredName() {
            return "me_importbus";
        }

        @Override
        public int priority() {
            return 1;
        }

        @Callback(doc = "function(side:number[, slot:number]):boolean -- Get the configuration of the import bus pointing in the specified direction.")
        public Object[] getImportConfiguration(Context context, Arguments args) {
            return getPartConfig(context, args, AEUtil::isImportBus);
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the import bus pointing in the specified direction to import item stacks matching the specified descriptor.")
        public Object[] setImportConfiguration(Context context, Arguments args) {
            return setPartConfig(context, args, AEUtil::isImportBus);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return AEUtil.isImportBus(stack) ? Environment.class : null;
        }
    }
}
