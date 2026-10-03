package li.cil.oc.integration.appeng;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public final class DriverController extends DriverSidedTileEntity {
    public static final DriverController INSTANCE = new DriverController();

    private DriverController() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return AEUtil.controllerClass();
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment(world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<BlockEntity> implements NamedBlock, NetworkControl {
        public Environment(BlockEntity tileEntity) {
            super(tileEntity, "me_controller");
        }

        @Override
        public String preferredName() {
            return "me_controller";
        }

        @Override
        public int priority() {
            return 5;
        }

        @Nullable
        @Override
        public IGridNode gridNode() {
            // Only ever referenced through API interfaces: AE2's block entity classes have
            // Fabric-only supertypes that aren't on common's compile classpath.
            return tileEntity instanceof IActionHost host ? host.getActionableNode() : null;
        }

        @Override
        public BlockEntity hostEntity() {
            return tileEntity;
        }

        @Nullable
        @Override
        public Direction partSide() {
            return null;
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return AEUtil.isController(stack) ? Environment.class : null;
        }
    }
}
