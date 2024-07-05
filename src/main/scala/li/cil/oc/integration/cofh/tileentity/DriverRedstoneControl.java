package li.cil.oc.integration.cofh.tileentity;

import cofh.lib.util.control.IRedstoneControllable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class DriverRedstoneControl extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return IRedstoneControllable.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        IRedstoneControllable tileEntity = (IRedstoneControllable) world.getBlockEntity(pos);
        if (!tileEntity.isControllable()) return null;
        return new Environment(tileEntity);
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IRedstoneControllable> {
        public Environment(final IRedstoneControllable tileEntity) {
            super(tileEntity, "redstone_control");
        }

        @Callback(doc = "function():boolean --  Returns whether the control is disabled.")
        public Object[] getControlDisable(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMode() == IRedstoneControllable.ControlMode.DISABLED};
        }

        @Callback(doc = "function():int --  Returns the control status.")
        public Object[] getControlSetting(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMode().ordinal()};

        }

        @Callback(doc = "function():string --  Returns the control status.")
        public Object[] getControlSettingName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMode().name()};
        }

        @Callback(doc = "function(int):string --  Returns the name of the given control")
        public Object[] getControlName(final Context context, final Arguments args) {
            IRedstoneControllable.ControlMode m = IRedstoneControllable.ControlMode.values()[args.checkInteger(0)];
            return new Object[]{m.name()};
        }

        @Callback(doc = "function():boolean --  Returns whether the component is powered.")
        public Object[] isPowered(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getState()};
        }

        @Callback(doc = "function():boolean --  Sets the control to disabled.")
        public Object[] setControlDisable(final Context context, final Arguments args) {
            tileEntity.setControl(0, IRedstoneControllable.ControlMode.DISABLED);
            return new Object[]{true};
        }

        @Callback(doc = "function(state:int[, threshold:number=8]):boolean --  Sets the control status and threshold to the given value.")
        public Object[] setControlSetting(final Context context, final Arguments args) {
            if (args.isInteger(0)) {
                int threshold = args.optInteger(1, 8);
                tileEntity.setControl(threshold, IRedstoneControllable.ControlMode.values()[args.checkInteger(0)]);
                return new Object[]{true};
            } else {
                int threshold = args.optInteger(1, 8);
                tileEntity.setControl(threshold, IRedstoneControllable.ControlMode.valueOf(args.checkString(0)));
                return new Object[]{true};
            }

        }
    }
}
