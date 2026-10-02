package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.mixin.PistonBaseBlockInvoker;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.Map;

public abstract class UpgradePiston extends AbstractManagedEnvironment implements DeviceInfo, PistonTraits.ExtendAware {
    public final EnvironmentHost host;

    /** Was an overridable {@code val} in Scala; sticky subclasses pass {@code true}. */
    public final boolean isSticky;

    public UpgradePiston(EnvironmentHost host) {
        this(host, false);
    }

    protected UpgradePiston(EnvironmentHost host, boolean isSticky) {
        this.host = host;
        this.isSticky = isSticky;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("piston").
            withConnector().
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    @Override
    public EnvironmentHost host() {
        return host;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Piston upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Displacer II+"
            );
        }
        return deviceInfo;
    }

    @Callback(doc = "function():boolean -- Returns true if the piston is sticky, i.e. it can also pull.")
    public Object[] isSticky(Context context, Arguments args) {
        return ResultWrapper.result(isSticky);
    }

    protected Object[] doPistonAction(Context context, Direction side, boolean extending) {
        final ResourceLocation sound = extending ? SoundEvents.PISTON_EXTEND.getLocation() : SoundEvents.PISTON_CONTRACT.getLocation();
        final BlockPos hostPos = pushOrigin(side).toBlockPos();
        final PistonBaseBlockInvoker piston = (PistonBaseBlockInvoker) (isSticky ? Blocks.STICKY_PISTON : Blocks.PISTON);

        if (!extending) {
            if (!isSticky) {
                // this is a bug in oc code
                throw new NoSuchMethodError("piston is not sticky. does not have pull");
            }
            // make sure that any obstruction block has breaking mobility
            final BlockPos innerBlockPos = hostPos.relative(side);
            final BlockState innerBlockState = host.world().getBlockState(innerBlockPos);
            if (innerBlockState != null) {
                if (!innerBlockState.isAir()) {
                    if (innerBlockState.getPistonPushReaction() != PushReaction.DESTROY) {
                        return ResultWrapper.result(false, "path is obstructed");
                    }
                }
            }
        }

        if (piston.oc$moveBlocks(host.world(), hostPos, side, extending)) {
            // send piston extend sound to clients
            synchronized (host) {
                PacketSender.sendSound(
                    host.world(), hostPos.getX(), hostPos.getY(), hostPos.getZ(),
                    sound, SoundSource.BLOCKS, 15.0);
            }
            context.pause(1.0 / 20.0);
            return ResultWrapper.result(true);
        }
        else {
            return ResultWrapper.result(false, "move failed");
        }
    }

    @Callback(doc = "function([side:number]):boolean -- Tries to push the block on the specified side of the container of the upgrade. Defaults to front.")
    public Object[] push(Context context, Arguments args) {
        final Direction side = pushDirection(args, 0);
        return doPistonAction(context, side, true);
    }

    // ----------------------------------------------------------------------- //

    public static class Drone extends UpgradePiston implements PistonTraits.DroneLike {
        public Drone(li.cil.oc.api.internal.Drone drone) {
            super(drone);
        }
    }

    public static class Rotatable extends UpgradePiston implements PistonTraits.RotatableLike {
        public final li.cil.oc.api.internal.Rotatable rotatable;

        public <T extends li.cil.oc.api.internal.Rotatable & EnvironmentHost> Rotatable(T rotatable) {
            super(rotatable);
            this.rotatable = rotatable;
        }

        @Override
        public li.cil.oc.api.internal.Rotatable rotatable() {
            return rotatable;
        }
    }

    public static class Tablet extends Rotatable implements PistonTraits.TabletLike {
        public final li.cil.oc.api.internal.Tablet tablet;

        public Tablet(li.cil.oc.api.internal.Tablet tablet) {
            super(tablet);
            this.tablet = tablet;
        }

        @Override
        public li.cil.oc.api.internal.Tablet tablet() {
            return tablet;
        }
    }
}
