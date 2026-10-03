package li.cil.oc.integration.appeng;

import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
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
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public final class DriverPartInterface implements DriverBlock {
    public static final DriverPartInterface INSTANCE = new DriverPartInterface();

    private DriverPartInterface() {
    }

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        return AEUtil.hasPart(world, pos, AEUtil::isPartInterface);
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        final IPartHost host = (IPartHost) world.getBlockEntity(pos);
        // Prefer the interface on the face touching the adapter (side is the face of this block
        // the adapter is attached to), otherwise use any interface on the cable bus.
        Direction partSide = null;
        if (side != null && AEUtil.isPartInterface(host.getPart(side))) {
            partSide = side;
        } else if (side != null && AEUtil.isPartInterface(host.getPart(side.getOpposite()))) {
            partSide = side.getOpposite();
        } else {
            for (Direction d : Direction.values()) {
                if (AEUtil.isPartInterface(host.getPart(d))) {
                    partSide = d;
                    break;
                }
            }
        }
        return new Environment(host, partSide);
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPartHost> implements NamedBlock, PartEnvironmentBase, NetworkControl {
        private final Direction partSide;

        public Environment(IPartHost host, Direction partSide) {
            super(host, "me_interface");
            this.partSide = partSide;
        }

        @Override
        public IPartHost host() {
            return tileEntity;
        }

        @Override
        public String preferredName() {
            return "me_interface";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Nullable
        @Override
        public IGridNode gridNode() {
            final IPart part = tileEntity.getPart(partSide);
            return part != null ? part.getGridNode() : null;
        }

        @Override
        public BlockEntity hostEntity() {
            return tileEntity.getBlockEntity();
        }

        @Nullable
        @Override
        public Direction partSide() {
            return partSide;
        }

        @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the interface pointing in the specified direction.")
        public Object[] getInterfaceConfiguration(Context context, Arguments args) {
            return getPartConfig(context, args, AEUtil::isPartInterface);
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface pointing in the specified direction.")
        public Object[] setInterfaceConfiguration(Context context, Arguments args) {
            return setPartConfig(context, args, AEUtil::isPartInterface);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return AEUtil.isPartInterface(stack) ? Environment.class : null;
        }
    }
}
